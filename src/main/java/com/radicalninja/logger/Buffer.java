package com.radicalninja.logger;

import android.util.Log;

abstract class Buffer {

    protected final String TAG = this.getClass().getSimpleName();

    private final LogFileOutputStream fileOutputStream;

    public Buffer() {
        fileOutputStream = openBufferFile();
        LogManager.getInstance().registerBuffer(this);
    }

    private LogFileOutputStream openBufferFile() {
        try {
            return (isBufferAllowed()) ?
                    LogManager.getInstance().createLogOutputStream(getFilename()) : null;
        } catch (final Exception e) {
            Log.e(TAG, "Buffer construction error. Buffer was not registered with LogManager.", e);
            onConstructorError(e);
            return null;
        }
    }

    final LogFileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    protected final boolean isBufferLoggingEnabled() {
        return isBufferAllowed() && !LogManager.getInstance().isPrivacyModeEnabled();
    }

    final String getDebugTag() {
        return TAG;
    }

    abstract void clearBuffer();

    abstract String getBufferContents();

    abstract String getFilename();

    abstract boolean isBufferAllowed();

    abstract void onConstructorError(final Throwable error);

    abstract void startNewLine();

}
