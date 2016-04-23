package com.radicalninja.logger;

abstract class LogFileController {

    protected final String TAG = this.getClass().getSimpleName();

    private final LogFileOutputStream fileOutputStream;

    LogFileController() {
        fileOutputStream = openLogFile();
    }

    protected LogFileOutputStream openLogFile() {
        try {
            return (isLogEnabled()) ?
                    LogManager.getInstance().createLogOutputStream(getFilename()) : null;
        } catch (final Exception e) {
            onConstructorError(e);
            return null;
        }
    }

    final String getDebugTag() {
        return TAG;
    }

    final LogFileOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    protected abstract boolean isLogEnabled();

    abstract String getFilename();

    abstract void onConstructorError(final Throwable error);

}
