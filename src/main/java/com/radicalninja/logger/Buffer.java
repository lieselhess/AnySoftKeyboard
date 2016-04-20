package com.radicalninja.logger;

import android.util.Log;

import java.io.FileOutputStream;

public abstract class Buffer {

    protected final String TAG = this.getClass().getSimpleName();

    private final FileOutputStream fileOutputStream;

    public Buffer() {
        fileOutputStream = openBufferFile();
        LogManager.getInstance().registerBuffer(this);
    }

    private FileOutputStream openBufferFile() {
        try {
            return (isBufferAllowed()) ?
                    LogManager.getInstance().createLogOutputStream(getFilename()) : null;
        } catch (Exception e) {
            Log.e(TAG, "Buffer construction error.", e);
            onConstructorError(e);
            return null;
        }
    }

    final FileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    protected final boolean isBufferLoggingEnabled() {
        return isBufferAllowed() && !LogManager.getInstance().isPrivacyModeEnabled();
    }

    abstract void clearBuffer();

    //abstract void finishBuffer();

    abstract String getBufferContents();

    abstract String getFilename();

    abstract boolean isBufferAllowed();

    abstract void onConstructorError(final Throwable error);

    abstract void startNewLine();

}
