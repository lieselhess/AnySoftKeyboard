package com.radicalninja.logger;

abstract class LogFileController {

    protected final String TAG = this.getClass().getSimpleName();

    private final FileWriter fileWriter;

    LogFileController() {
        fileWriter = openLogFile();
    }

    protected FileWriter openLogFile() {
        try {
            return (isLogEnabled()) ?
                    LogManager.getInstance().createFileWriter(getFilename()) : null;
        } catch (final Exception e) {
            onConstructorError(e);
            return null;
        }
    }

    final String getDebugTag() {
        return TAG;
    }

    final FileWriter getFileWriter() {
        return fileWriter;
    }

    protected abstract boolean isLogEnabled();

    abstract String getFilename();

    abstract void onConstructorError(final Throwable error);

}
