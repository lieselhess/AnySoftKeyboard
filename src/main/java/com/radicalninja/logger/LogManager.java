package com.radicalninja.logger;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.menny.android.anysoftkeyboard.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LogManager {

    private static final String TAG = "LogManager";
    private static final String FORMAT_LINE_PREFIX = "[yyyy-MM-dd HH:mm:ss]";

    private static LogManager instance;

    private final Context context;
    private final PreferencesManager preferencesManager;
    private final List<Buffer> buffers = new ArrayList<>();

    private Date startTime;
    private boolean privacyModeEnabled;

    static class LogFileOutputStream extends FileOutputStream {
        final String fileDirectory;

        public LogFileOutputStream(File file, boolean append) throws FileNotFoundException {
            super(file, append);
            fileDirectory = file.getAbsolutePath();
        }
    }

    public static void init(final Context context) {
        if (instance == null) {
            instance = new LogManager(context);
        }
    }

    public static void destroy() {
        if (instance != null) {
            instance.destroyBuffers();
            instance = null;
        }
    }

    public static void startLine(@NonNull final EditorInfo attribute) {
        if (instance != null) {
            instance.startNewLine(attribute, true);
        }
    }

    public static void finishLine() {
        if (instance != null) {
            instance.clearBuffers(true);
        }
    }

    static LogManager getInstance() throws LoggerNotCreatedException {
        if (instance == null) {
            throw new LoggerNotCreatedException();
        }
        return instance;
    }

    boolean isPrivacyModeEnabled() {
        return privacyModeEnabled;
    }

    @SuppressWarnings("NewApi")
    private LogManager(final Context context) {
        this.context = context;
        preferencesManager = PreferencesManager.getInstance(context);
    }

    boolean registerBuffer(final Buffer buffer) {
        return buffer.isBufferAllowed() && buffers.add(buffer);
    }

    boolean unregisterBuffer(final Buffer buffer) {
        return buffers.remove(buffer);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    FileOutputStream createLogOutputStream(final String logFilename) throws IOException {
        LogFileOutputStream outputStream = null;
        Exception exception = null;

        try {
            if (!BuildConfig.USE_SDCARD) {
                outputStream = openPrivateStorage(logFilename);
            } else {
                // Try opening log files on the SD card, fall back to alternate locations on failures.
                try {
                    outputStream = openExternalPublicStorage(logFilename);
                } catch (final FileNotFoundException e2) {
                    exception = e2;
                    try {
                        outputStream = openFallbackPublicStorage(logFilename);
                    } catch (final FileNotFoundException e3) {
                        exception = e3;
                        // Final exception defaults to private storage in /data/data.
                        outputStream = openPrivateStorage(logFilename);
                    }
                }
            }
        } catch (final FileNotFoundException e1) {
            exception = e1;
        }

        if (outputStream == null) {
            throw new IOException("Could not open private or public storage!");
        }

        if (exception != null) {
            CrashReportUtility.throwCrashReportNotification(context, exception);
        }
        CrashReportUtility.displayLoggingAlertNotification(context,
                CrashReportUtility.TAG_LOG_LOCATION, outputStream.fileDirectory);
        return outputStream;
    }

    private LogFileOutputStream openPrivateStorage(final String filename) throws FileNotFoundException {
        final File file = new File(context.getFilesDir(), filename);
        return new LogFileOutputStream(file, true);
    }

    private LogFileOutputStream openExternalPublicStorage(final String filename) throws FileNotFoundException {
        final File file = new File(context.getExternalFilesDir(null), filename);
        return new LogFileOutputStream(file, true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private LogFileOutputStream openFallbackPublicStorage(final String filename) throws FileNotFoundException {
        // TODO: Should probably sanitize the FALLBACK_LOG_DIRECTORY before sending it through File constructor
        final String logDirPath = String.format("%s/%s/",
                Environment.getExternalStorageDirectory(), BuildConfig.FALLBACK_LOG_DIRECTORY);
        final File logDir = new File(logDirPath);
        logDir.mkdirs();
        final File file = new File(logDir, filename);
        return new LogFileOutputStream(file, true);
    }

    /**
     * Reset the buffers for a new line session.
     *
     * @param attribute The EditorInfo object of the current text input view.
     * @param logBuffer Whether or not the current buffer contents should be logged
     *                  before being cleared.
     */
    private void startNewLine(@NonNull final EditorInfo attribute, final boolean logBuffer) {
        clearBuffers(logBuffer);
        setupPrivacyMode(attribute);
        startTime = new Date();
        for (final Buffer buffer : buffers) {
            buffer.startNewLine();
        }
    }

    /**
     * Check the current EditorInfo object for the potential of sensitive data entry.
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

    private void saveBuffers() {
        for (final Buffer buffer : buffers) {
            try {
                writeToFile(buffer);
            } catch (final IOException | NullPointerException e) {
                final String msg = String.format(
                        "Unable to save %s contents to disk.", buffer.getDebugTag());
                Log.e(TAG, msg, e);
            }
        }
    }

    /**
     * Clear the buffers out and start fresh. Has the option to write the current buffer contents
     * to the log if it's not empty.
     *
     * @param logBuffer If the buffer is not empty, write the contents to the log before clearing.
     */
    private void clearBuffers(final boolean logBuffer) {
        if (logBuffer) {
            saveBuffers();
        }
        for (final Buffer buffer : buffers) {
            buffer.clearBuffer();
        }
    }

    private void destroyBuffers() {
        final Iterator<Buffer> iterator = buffers.iterator();
        while (iterator.hasNext()) {
            final Buffer buffer = iterator.next();
            try {
                buffer.getFileOutputStream().close();
            } catch (final IOException e) {
                Log.e(TAG, String.format("Error closing FileOutputStream for %s",
                        buffer.getDebugTag()), e);
            } catch (final NullPointerException e) {
                Log.e(TAG, "Error closing FileOutputStream for null buffer reference", e);
            }
            iterator.remove();
        }
    }

    private void writeToFile(final Buffer buffer)
            throws IOException, NullPointerException {
        final String bufferContents = buffer.getBufferContents();
        if (TextUtils.isEmpty(bufferContents)) {
            return;
        }
        final FileOutputStream outputStream = buffer.getFileOutputStream();
        if (outputStream == null) {
            // Buffers that are don't successfully create a FileOutputStream should not get
            throw new NullPointerException(
                    "Buffer output stream must not be null! THIS SHOULDN'T HAPPEN so something must have gone wrong.");
        }
        final SimpleDateFormat format = new SimpleDateFormat(FORMAT_LINE_PREFIX, Locale.US);
        final String startTimeString = format.format(startTime);
        final String endTimeString = format.format(new Date());
        final String logLine = String.format("[%s - %s] %s\n", startTimeString, endTimeString, bufferContents);
        outputStream.write(logLine.getBytes());
        Log.i(TAG, String.format("%s logged: %s", buffer.getDebugTag(), logLine));
    }

}
