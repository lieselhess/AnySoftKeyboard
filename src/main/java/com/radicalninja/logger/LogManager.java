package com.radicalninja.logger;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.menny.android.anysoftkeyboard.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogManager {

    private static final String FALLBACK_LOG_DIRECTORY = "AnySoftKeyboardLogs";
    private static final String RAW_LOG_FILENAME = "raw.log";
    private static final String BUFFER_LOG_FILENAME = "buffered.log";

    public static final String TAG = "LogManager";

    private final Context context;
    private boolean privacyModeEnabled;
    //private final PreferencesManager preferencesManager;

    private boolean isNewLine = false;
    private boolean isLineFinished = true;
    private FileOutputStream rawOutputStream, bufferedOutputStream;

    @SuppressWarnings({"NewApi", "PointlessBooleanExpression", "ConstantConditions"})
    public LogManager(Context context) {
        this.context = context;
        //preferencesManager = PreferencesManager.getInstance(context);

        try {
            if (!BuildConfig.USE_SDCARD) {
                openLogfile();
            } else {
                // Try opening log files on the SD card, fall back on alternate locations on failures.
                try {
                    openLogFileExternalStorage();
                    CrashReportUtility.displayLoggingAlertNotification(context, "Log file location",
                            this.context.getExternalFilesDir(null).getAbsolutePath());
                } catch (final FileNotFoundException e2) {
                    try {
                        openLogExternalStorageFallback();
                        CrashReportUtility.throwCrashReportNotification(context, e2);
                        CrashReportUtility.displayLoggingAlertNotification(context, "Log file location",
                                Environment.getExternalStorageDirectory() + FALLBACK_LOG_DIRECTORY);
                    } catch (final FileNotFoundException e3) {
                        // Final exception defaults to private storage in /data/data.
                        openLogfile();
                        CrashReportUtility.throwCrashReportNotification(context, e3);
                        CrashReportUtility.displayLoggingAlertNotification(context, "Log file location",
                                "PRIVATE APP STORAGE");
                    }
                }
            }
        } catch (final FileNotFoundException | NullPointerException e1) {
            CrashReportUtility.throwCrashReportNotification(context, e1);
        }
    }

    /**
     * Check the current EditorInfo object for the potential for sensitive data entry.
     * If detected, the log will be disabled for this line session.
     *
     * @param attribute the attributes object of the focused text-input view.
     */
    void setupPrivacyMode(final EditorInfo attribute) {

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

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    boolean isWordBufferEnabled() {
        return BuildConfig.LOG_WORDS && privacyModeEnabled;
    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    boolean isRawBufferEnabled() {
        return BuildConfig.LOG_RAW && privacyModeEnabled;
    }

    private void openLogfile() throws FileNotFoundException {
        rawOutputStream = context.openFileOutput(RAW_LOG_FILENAME, Context.MODE_APPEND);
        bufferedOutputStream = context.openFileOutput(BUFFER_LOG_FILENAME, Context.MODE_APPEND);
    }

    @SuppressWarnings("NewApi")
    private void openLogFileExternalStorage() throws FileNotFoundException {
        final File fileRaw = new File(context.getExternalFilesDir(null), RAW_LOG_FILENAME);
        rawOutputStream = new FileOutputStream(fileRaw, true);
        final File fileBuffered = new File(context.getExternalFilesDir(null), BUFFER_LOG_FILENAME);
        bufferedOutputStream = new FileOutputStream(fileBuffered, true);
    }

    @SuppressWarnings("NewApi")
    private void openLogExternalStorageFallback() throws FileNotFoundException {
        final String logDirPath = String.format("%s/%s/",
                Environment.getExternalStorageDirectory(), FALLBACK_LOG_DIRECTORY);
        final File logDir = new File(logDirPath);
        logDir.mkdirs();
        final File fileRaw = new File(logDir, RAW_LOG_FILENAME);
        rawOutputStream = new FileOutputStream(fileRaw, true);
        final File fileBuffered = new File(logDir, BUFFER_LOG_FILENAME);
        bufferedOutputStream = new FileOutputStream(fileBuffered, true);
    }

    public void close() throws IOException {
        rawOutputStream.close();
    }

    private void writeBufferedLine(String strToWrite) throws IOException {
        bufferedOutputStream.write(strToWrite.getBytes());
    }

    public void write(char charToWrite) throws IOException {
        writeRawString(Character.toString(charToWrite));
    }

    public void writeRawString(final String strToWrite) throws IOException {
        if (isNewLine) {
            writeNewLine();
        }

        rawOutputStream.write(strToWrite.getBytes());
    }

    public void startLine() {
        isNewLine = true;
    }

    private void writeNewLine() {
        isNewLine = false;   // reset the flag before we recursively call write(String);
        if (!isLineFinished) {
            writeEndLine();
        }
        isLineFinished = false;
        final String timestamp =
                new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ", Locale.US).format(new Date());
        try {
            writeRawString(timestamp);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write timestamp!");
        }
    }

    public void finishLine() {
        if (!isNewLine || !isLineFinished) {
            writeEndLine();
            isNewLine = true;
        }
    }

    private void writeEndLine() {
        isLineFinished = true;
        isNewLine = false;
        try {
            writeRawString("\n");
        } catch (IOException e) {
            Log.e(TAG, "Unable to write new-line [\\n]");
        }
    }

    public void writeLineBuffer(final String bufferContents, final Date startTime) {
        if (TextUtils.isEmpty(bufferContents)) {
            return;
        }
        final SimpleDateFormat format = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]", Locale.US);
        final String startTimeString = format.format(startTime);
        final String endTimeString = format.format(new Date());
        final String logLine = String.format("[%s - %s] %s\n", startTimeString, endTimeString, bufferContents);
        Log.i(TAG, String.format("LINE LOGGED: %s", logLine));
        try {
            writeBufferedLine(logLine);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write buffered line!!");
            e.printStackTrace();
        }
    }

}
