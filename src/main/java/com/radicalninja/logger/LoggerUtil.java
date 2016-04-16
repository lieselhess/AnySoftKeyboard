package com.radicalninja.logger;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoggerUtil {

    private static final boolean SDCARD_DEMO_MODE_ENABLED = true;

    private static final String FALLBACK_LOG_DIRECTORY = "AnySoftKeyboardLogs";
    private static final String RAW_LOG_FILENAME = "raw.log";
    private static final String BUFFER_LOG_FILENAME = "buffered.log";

    public static final String TAG = "LoggerUtil";

    private final Context context;
    //private final PreferencesManager preferencesManager;

    private boolean isNewLine = false;
    private boolean isLineFinished = true;
    private FileOutputStream rawOutputStream, bufferedOutputStream;

    @SuppressWarnings("NewApi")
    public LoggerUtil(Context context) {
        this.context = context;
        //preferencesManager = PreferencesManager.getInstance(context);

        try {
            if (!SDCARD_DEMO_MODE_ENABLED) {
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
        } catch (final FileNotFoundException e1) {
            CrashReportUtility.throwCrashReportNotification(context, e1);
        }
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

    public void writeBufferedLine(String strToWrite) throws IOException {
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
