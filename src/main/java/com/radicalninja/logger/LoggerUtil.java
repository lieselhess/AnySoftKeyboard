package com.radicalninja.logger;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoggerUtil {

    // TODO: make method that will generate a dated log filename every Nth hour.

    private static final String LOG_FILENAME = "Logger.log";

    public static final String TAG = "LoggerUtil";

    private boolean mNewLine = false;
    private boolean mLineFinished = true;
    private Context mContext;
    private FileOutputStream mOutputStream;
    //private PreferencesManager mPrefs;

    public LoggerUtil(Context context) throws FileNotFoundException {
        mContext = context;
        //mPrefs = PreferencesManager.getInstance(context);
        openLogfile();
    }

    private void openLogfile() throws FileNotFoundException {
        mOutputStream = mContext.openFileOutput(LOG_FILENAME, Context.MODE_APPEND);
    }

    public void close() throws IOException {
        mOutputStream.close();
    }

    public void write(String strToWrite) throws IOException {
        if (mNewLine) {
            writeNewLine();
        }

        mOutputStream.write(strToWrite.getBytes());
    }

    public void write(char charToWrite) throws IOException {
        write(Character.toString(charToWrite));
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
            write(timestamp);
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
            write("\n");
        } catch (IOException e) {
            Log.e(TAG, "Unable to write new-line [\\n]");
        }
    }

}
