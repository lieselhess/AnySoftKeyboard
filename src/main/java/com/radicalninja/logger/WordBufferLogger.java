package com.radicalninja.logger;

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

    public void setCursorPositions(final int cursorStart, final int cursorEnd) {
        this.cursorStart = cursorStart;
        this.cursorEnd = cursorEnd;
    }

    // TODO: submitInput methods need to take cursor position in to account.
    public void submitInput(final String input) {
        Log.d(TAG, "submitInput("+input+")");
        prevInput = input;
        prevInputUntouched = "";
        lineBuffer.append(input);
    }

    public void submitInput(final CharSequence input) {
        submitInput(input.toString());
    }

    public void submitInput(final String corrected, final String untouched) {
        Log.d(TAG, "submitInput("+corrected+", "+ untouched+")");
        prevInput = corrected;
        prevInputUntouched = untouched;
        lineBuffer.append(corrected);
    }

    public void submitInput(final CharSequence corrected, final CharSequence untouched) {
        submitInput(corrected.toString(), untouched.toString());
    }

    public void replaceLastInput(final String replaceWith) {
        submitInput(prevInput, replaceWith);
    }

    public void replaceLastInput(final CharSequence replaceWith) {
        replaceLastInput(replaceWith.toString());
    }

    public void deleteSurroundingText(final int lengthBefore, final int lengthAfter) {
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

    public void clearBuffer(final boolean logBuffer) {
        if (logBuffer && log != null && lineBuffer.length() > 0) {
            // TODO: Log lineBuffer to the LoggerUtil with timestamp
        }
        lineBuffer.delete(0, lineBuffer.length());
    }

    public void revertLastInput() {
        Log.d(TAG, "revertLastInput()");
        lineBuffer.setLength(lineBuffer.length() - prevInput.length();
        lineBuffer.append(prevInputUntouched);
    }
}
