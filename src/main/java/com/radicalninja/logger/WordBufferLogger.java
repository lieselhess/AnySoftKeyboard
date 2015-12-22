package com.radicalninja.logger;

import com.anysoftkeyboard.utils.Log;

/**
 * WordBufferLogger keeps a buffer of the user's current typing line.
 * The buffer is updated in real-time with auto-completed word, cancelled
 * auto-completions, manual character input (punctuation, emoji, etc).
 */
public class WordBufferLogger {

    private static final String TAG = WordBufferLogger.class.getSimpleName();

    private final LoggerUtil log;
    private final StringBuilder lineBuffer = new StringBuilder();

    private String prevInput = "";
    private String prevInputUntouched = "";
    private String prevWhiteSpace = "";

    public WordBufferLogger() {
        Log.d(TAG, "Default constructor");
        this.log = null;
    }

    public WordBufferLogger(LoggerUtil log) {
        Log.d(TAG, "Constructor with LoggerUtil");
        this.log = log;
    }

    public void submitInput(final String input) {
        Log.d(TAG, "submitInput("+input+")");
        prevInput = input;
        prevWhiteSpace = "";
        lineBuffer.append(input);
    }

//    public void submitInput(final String input, final int endPadding) {
//        Log.d(TAG, "submitInput("+input+")");
//        prevInput = input;
//        lineBuffer.append(input);
//        for (int i = 0; i < endPadding; i++) {
//            lineBuffer.append(" ");
//        }
//    }
//
    public void submitInput(final String corrected, final String untouched) {
        Log.d(TAG, "submitInput("+corrected+", "+ untouched+")");
        prevInput = corrected;
        prevInputUntouched = untouched;
        prevWhiteSpace = "";
        lineBuffer.append(corrected);
    }

    public void submitWhiteSpace(final char character) {
        prevWhiteSpace = String.valueOf(character);
        lineBuffer.append(character);
    }

    public void revertLastInput() {
        Log.d(TAG, "revertLastInput()");
        lineBuffer.setLength(lineBuffer.length() - prevInput.length() - prevWhiteSpace.length());
        lineBuffer.append(prevInputUntouched);
    }
}
