package com.radicalninja.logger;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LoggerUtil {

    // TODO: make method that will generate a dated log filename every Nth hour.

    private static final String LOG_FILENAME = "Logger.log";

    public static final String TAG = "LoggerUtil";

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
        mOutputStream.write(strToWrite.getBytes());
    }

    public void write(char charToWrite) throws IOException {
        String str = Character.toString(charToWrite);
        mOutputStream.write(str.getBytes());
        //mPrefs.setLastUsed(System.currentTimeMillis());
    }
}
