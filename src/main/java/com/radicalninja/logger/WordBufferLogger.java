package com.radicalninja.logger;

import android.content.Context;
import android.view.inputmethod.EditorInfo;

import com.anysoftkeyboard.base.dictionaries.WordComposer;
import com.anysoftkeyboard.utils.Log;

import java.util.Date;

/**
 * WordBufferLogger keeps a buffer of the user's current typing line.
 * The buffer is updated in real-time with auto-completed word, cancelled
 * auto-completions, manual character input (punctuation, emoji, etc).
 */
public class WordBufferLogger {

    private static final String TAG = WordBufferLogger.class.getSimpleName();

    private final LoggerUtil log;
    private final StringBuilder lineBuffer = new StringBuilder();

    private boolean privacyModeEnabled;
    private int keyboardCursorStart, keyboardCursorEnd;
    private int oldKeyboardCursorStart, oldKeyboardCursorEnd;
    private int cursorPosition;

    private Date startTime;

    private String composingText = "";
    private String prevInput = "";
    private String prevWordCorrected, prevWordUntouched;

    public WordBufferLogger(final Context context) {
        Log.d(TAG, "Default constructor");
        // TODO: LoggerUtil:log should be initialized here.

        this.log = null;
    }

    public WordBufferLogger(LoggerUtil log) {
        this.log = log;
    }

    private boolean diffOutOfRange(final int a, final int b, final int range) {
        final int diff = a - b;
        return diff > range || diff < -range;
    }

    /**
     * Reset the line buffer for a new line session.
     *
     * @param attribute The EditorInfo object of the current text input view.
     * @param logBuffer Whether or not the current buffer contents should be logged
     *                  before being cleared.
     */
    public void startNewLine(final EditorInfo attribute, final boolean logBuffer) {
        clearBuffer(logBuffer);
        setupPrivacyMode(attribute);
        cursorPosition = 0;
        oldKeyboardCursorStart = oldKeyboardCursorEnd = 0;
        keyboardCursorStart = keyboardCursorEnd = 0;
        composingText = prevInput = "";
        startTime = new Date();
    }

    /**
     * Check the current EditorInfo object for the potential for sensitive data entry.
     * If detected, the log will be disabled for this line session.
     *
     * @param attribute the attributes object of the focused text-input view.
     */
    private void setupPrivacyMode(final EditorInfo attribute) {

        final int editorClass = attribute.inputType & EditorInfo.TYPE_MASK_CLASS;
        switch (editorClass) {
            case EditorInfo.TYPE_CLASS_DATETIME:
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_PHONE:
                privacyModeEnabled = true;
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
                privacyModeEnabled = true;
                return;
        }
        privacyModeEnabled = false;
    }

    /**
     * Clear the buffer out and start fresh. Has the option to write the current buffer
     * to the log if it's not empty.
     *
     * @param logBuffer If the buffer is not empty, write the contents to the log before clearing.
     */
    private void clearBuffer(final boolean logBuffer) {
        if (lineBuffer.length() > 0) {
            if (logBuffer && log != null) {
                // TODO: Log lineBuffer to the LoggerUtil with both start & end timestamps
                // TODO: Store current time in milliseconds as the end of this line session.
                log.writeLineBuffer(lineBuffer.toString(), startTime);
            }
            lineBuffer.delete(0, lineBuffer.length());
        }
    }

    /**
     * Clear the buffer contents without resetting the line session.
     */
    public void clearBuffer() {
        clearBuffer(false);
    }

    public void setCursorPositions(final int cursorStart, final int cursorEnd) {
        if (privacyModeEnabled ||
                (cursorStart == cursorPosition && diffOutOfRange(keyboardCursorStart, cursorStart, 1))) {
            return;
        }
        oldKeyboardCursorStart = keyboardCursorStart;
        //oldKeyboardCursorEnd = keyboardCursorEnd;
        keyboardCursorStart = cursorStart;
        //keyboardCursorEnd = cursorEnd;
        updateCursorPosition(keyboardCursorStart);
    }

    public void setCursorPosition(final int cursorPosition) {
        if (privacyModeEnabled) {
            return;
        }
        this.cursorPosition = cursorPosition;
    }

    private void updateCursorPosition(final int newCursorPosition) {
        if (diffOutOfRange(oldKeyboardCursorStart, newCursorPosition, 1)) {
            cursorPosition = newCursorPosition;
        }
    }

    private void moveCursorToLeft(final int toLeft) {
        cursorPosition -= toLeft;
        updateCursorPosition(cursorPosition);
    }

    public void moveCursorToLeft() {
        moveCursorToLeft(1);
    }

    private void moveCursorToRight(final int toRight) {
        cursorPosition += toRight;
        updateCursorPosition(cursorPosition);
    }

    public void moveCursorToRight() {
        moveCursorToRight(1);
    }

    public void moveCursorToStart() {
        cursorPosition = 0;
        oldKeyboardCursorStart = 0;
    }

    public void moveCursorToEnd() {
        cursorPosition = lineBuffer.length();
        oldKeyboardCursorStart = lineBuffer.length();
    }

    public void setComposingText(final String composingText) {
        this.composingText = composingText;
    }

    public void setComposingText(final CharSequence composingText) {
        this.composingText = composingText.toString();
    }

    public void insertText(final WordComposer word) {
        if (privacyModeEnabled) {
            return;
        }
        final String input = word.getPreferredWord().toString();
        final int start = (cursorPosition > lineBuffer.length()) ? lineBuffer.length() : Math.max(cursorPosition, 0);
        prevInput = input;
        prevWordCorrected = input;
        prevWordUntouched = word.getTypedWord().toString();
        composingText = "";
        lineBuffer.insert(start, input);

        moveCursorToRight(input.length());

        oldKeyboardCursorStart = cursorPosition;
        keyboardCursorStart = cursorPosition;
    }

    public void insertText(String input) {
        if (privacyModeEnabled) {
            return;
        }
        prevInput = input;
        final int start = (cursorPosition > lineBuffer.length()) ? lineBuffer.length() : Math.max(cursorPosition, 0);
        if (composingText.length() > 0) {
            if (!composingText.equals(input)) {
                input = String.format("%s%s", composingText, input);
            }
            composingText = "";
        }
        lineBuffer.insert(start, input);

        moveCursorToRight(input.length());

        oldKeyboardCursorStart = cursorPosition;
        keyboardCursorStart = cursorPosition;
    }

    public void insertText(final CharSequence input) {
        insertText(input.toString());
    }

    public void deleteSurroundingText(final int lengthBefore, final int lengthAfter) {
        if (privacyModeEnabled || lengthBefore == lengthAfter) {
            return;
        }
        int deleteStart = Math.max(cursorPosition - lengthBefore, 0);
        int deleteEnd = Math.min(cursorPosition + lengthAfter, lineBuffer.length());
        lineBuffer.delete(deleteStart, deleteEnd);
        moveCursorToLeft(lengthBefore);
    }

    public void revertLastCorrection() {
        if (privacyModeEnabled) {
            return;
        }
        deleteSurroundingText(prevWordCorrected.length() + prevInput.length(), 0);
//        setCursorPosition(cursorPosition - prevWordCorrected.length() + prevWordUntouched.length());

        oldKeyboardCursorStart = cursorPosition;
        keyboardCursorStart = cursorPosition;
    }
}
