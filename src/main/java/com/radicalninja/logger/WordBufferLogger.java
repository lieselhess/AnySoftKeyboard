package com.radicalninja.logger;

import android.view.inputmethod.EditorInfo;

import com.anysoftkeyboard.utils.Log;

/**
 * WordBufferLogger keeps a buffer of the user's current typing line.
 * The buffer is updated in real-time with auto-completed word, cancelled
 * auto-completions, manual character input (punctuation, emoji, etc).
 */
public class WordBufferLogger {

    // TODO: Determine if the InputConnection.setComposingText(String, int) calls should be logged. (Maybe? Uses .replaceLastInput()
    // TODO: Character deletion is not working properly.

    private static final String TAG = WordBufferLogger.class.getSimpleName();

    private final LoggerUtil log;
    private final StringBuilder lineBuffer = new StringBuilder();

    private boolean loggingIsEnabled;
    private int cursorStart, cursorEnd;

    private String prevInput = "";
    private String prevInputUntouched = "";

    public WordBufferLogger() {
        Log.d(TAG, "Default constructor");
        this.log = null;
    }

    public WordBufferLogger(LoggerUtil log) {
        Log.d(TAG, "Constructor with LoggerUtil");
        this.log = log;
    }

    /**
     * Check the current EditorInfo object for the potential for sensitive data entry.
     * If detected, the log will be disabled for this text-entry session.
     *
     * @param attribute the attributes object of the focused text-input view.
     */
    public void determineEnabledState(final EditorInfo attribute) {

        final int editorClass = attribute.inputType & EditorInfo.TYPE_MASK_CLASS;
        switch (editorClass) {
            case EditorInfo.TYPE_CLASS_DATETIME:
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_PHONE:
                loggingIsEnabled = false;
                return;
        }

        final int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
        switch (variation) {
            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
            case EditorInfo.TYPE_TEXT_VARIATION_FILTER:
            case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
            case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME:
            case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_URI:
            case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
            case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
                loggingIsEnabled = false;
                return;
        }
        loggingIsEnabled = true;
    }

    public void clearBuffer(final boolean logBuffer) {
        if (logBuffer && log != null && lineBuffer.length() > 0) {
            // TODO: Log lineBuffer to the LoggerUtil with timestamp
        }
        lineBuffer.delete(0, lineBuffer.length());
    }

    public void setCursorPositions(final int cursorStart, final int cursorEnd) {
        if (!loggingIsEnabled) {
            return;
        }
        this.cursorStart = cursorStart;
        this.cursorEnd = cursorEnd;
    }

    // TODO: submitInput methods need to take cursor position in to account.
    public void submitInput(final String input) {
        Log.d(TAG, "submitInput("+input+")");
        if (!loggingIsEnabled) {
            return;
        }
        prevInput = input;
        prevInputUntouched = "";
        lineBuffer.append(input);
    }

    public void submitInput(final CharSequence input) {
        submitInput(input.toString());
    }

    public void submitInput(final String corrected, final String untouched) {
        Log.d(TAG, "submitInput("+corrected+", "+ untouched+")");
        if (!loggingIsEnabled) {
            return;
        }
        prevInput = corrected;
        prevInputUntouched = untouched;
        lineBuffer.append(corrected);
    }

    public void submitInput(final CharSequence corrected, final CharSequence untouched) {
        submitInput(corrected.toString(), untouched.toString());
    }

    public void replaceLastInput(final String replaceWith) {
        if (!loggingIsEnabled) {
            return;
        }
        submitInput(prevInput, replaceWith);
    }

    public void replaceLastInput(final CharSequence replaceWith) {
        replaceLastInput(replaceWith.toString());
    }

    public void deleteSurroundingText(final int lengthBefore, final int lengthAfter) {
        if (!loggingIsEnabled) {
            return;
        }
        int deleteStart = cursorStart - lengthBefore + 1;
        if (deleteStart < 0) {
            deleteStart = 0;
        }
        int deleteEnd = cursorEnd - lengthAfter + 1;
        if (deleteEnd > lineBuffer.length()) {
            deleteEnd = lineBuffer.length();
        }
        lineBuffer.delete(deleteStart, deleteEnd);
    }

    public void revertLastInput() {
        Log.d(TAG, "revertLastInput()");
        if (!loggingIsEnabled) {
            return;
        }
        lineBuffer.setLength(lineBuffer.length() - prevInput.length());
        lineBuffer.append(prevInputUntouched);
    }
}
