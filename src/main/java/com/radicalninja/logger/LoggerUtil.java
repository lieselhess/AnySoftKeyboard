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

    // TODO: make method that will generate a dated log filename every Nth hour.

    private static final boolean SDCARD_DEMO_MODE_ENABLED = true;

    private static final String FALLBACK_LOG_DIRECTORY = "AnySoftKeyboardLogs";
    private static final String RAW_LOG_FILENAME = "raw.log";
    private static final String BUFFER_LOG_FILENAME = "buffered.log";

    public static final String TAG = "LoggerUtil";

    private boolean mNewLine = false;
    private boolean mLineFinished = true;
    private Context mContext;
    private FileOutputStream mRawOutputStream, mBufferedOutputStream;
    //private PreferencesManager mPrefs;

    @SuppressWarnings("NewApi")
    public LoggerUtil(Context context) {
        mContext = context;
        //mPrefs = PreferencesManager.getInstance(context);

        try {
            if (!SDCARD_DEMO_MODE_ENABLED) {
                openLogfile();
            } else {
                // Try opening log files on the SD card, fall back on alternate locations on failures.
                try {
                    openLogFileExternalStorage();
                    CrashReportUtility.displayLoggingAlertNotification(context, "Log file location",
                            mContext.getExternalFilesDir(null).getAbsolutePath());
                } catch (final FileNotFoundException e2) {
                    try {
                        openLogExternalStorageFallback();
//                        CrashReportUtility.throwCrashReportNotification(context, e2);
                        CrashReportUtility.displayLoggingAlertNotification(context, "Log file location",
                                "/sdcard/" + FALLBACK_LOG_DIRECTORY);
                    } catch (final FileNotFoundException e3) {
                        // Final exception defaults to private storage in /data/data.
                        openLogfile();
//                        CrashReportUtility.throwCrashReportNotification(context, e3);
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
        mRawOutputStream = mContext.openFileOutput(RAW_LOG_FILENAME, Context.MODE_APPEND);
        mBufferedOutputStream = mContext.openFileOutput(BUFFER_LOG_FILENAME, Context.MODE_APPEND);
    }

    @SuppressWarnings("NewApi")
    private void openLogFileExternalStorage() throws FileNotFoundException {
        final File fileRaw = new File(mContext.getExternalFilesDir(null), RAW_LOG_FILENAME);
        mRawOutputStream = new FileOutputStream(fileRaw, true);
        final File fileBuffered = new File(mContext.getExternalFilesDir(null), BUFFER_LOG_FILENAME);
        mBufferedOutputStream = new FileOutputStream(fileBuffered, true);
    }

    @SuppressWarnings("NewApi")
    private void openLogExternalStorageFallback() throws FileNotFoundException {
        final String logDirPath = String.format("%s/%s/",
                Environment.getExternalStorageDirectory(), FALLBACK_LOG_DIRECTORY);
        final File logDir = new File(logDirPath);
        logDir.mkdirs();
        final File fileRaw = new File(logDir, RAW_LOG_FILENAME);
        mRawOutputStream = new FileOutputStream(fileRaw, true);
        final File fileBuffered = new File(logDir, BUFFER_LOG_FILENAME);
        mBufferedOutputStream = new FileOutputStream(fileBuffered, true);
    }

    public void close() throws IOException {
        mRawOutputStream.close();
    }

    public void writeBufferedLine(String strToWrite) throws IOException {
        mBufferedOutputStream.write(strToWrite.getBytes());
    }

    public void write(char charToWrite) throws IOException {
        writeRawString(Character.toString(charToWrite));
    }

    public void writeRawString(final String strToWrite) throws IOException {
        if (mNewLine) {
            writeNewLine();
        }

        mRawOutputStream.write(strToWrite.getBytes());
    }

    public void startLine() {
        mNewLine = true;
    }

    private void writeNewLine() {
        mNewLine = false;   // reset the flag before we recursively call write(String);
        if (!mLineFinished) {
            writeEndLine();
        }
        mLineFinished = false;
        String timestamp =
                new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ", Locale.US).format(new Date());
        try {
            writeRawString(timestamp);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write timestamp!");
        }
    }

    public void finishLine() {
        if (!mNewLine || !mLineFinished) {
            writeEndLine();
            mNewLine = true;
        }
    }

    private void writeEndLine() {
        mLineFinished = true;
        mNewLine = false;
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
