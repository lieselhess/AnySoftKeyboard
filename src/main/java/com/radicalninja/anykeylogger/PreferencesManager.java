package com.radicalninja.anykeylogger;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private final static String PREFS_NAME = "LoggerPrefs";

    private final static String KEY_LASTUPLOADED = "LastUploaded";
    private final static long DEF_LASTUPLOADED = 0;

    private final static String KEY_DBOAUTH = "DbOauthKey";
    private final static String DEF_DBOAUTH = "";

    private static PreferencesManager sInstance;

    private SharedPreferences mPrefs;

    public static PreferencesManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PreferencesManager(context);
        }
        return sInstance;
    }

    public PreferencesManager(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public long getLastUploaded() {
        return mPrefs.getLong(KEY_LASTUPLOADED, DEF_LASTUPLOADED);
    }

    public void setLastUploaded(long lastUploadedInMillis) {
        mPrefs.edit().putLong(KEY_LASTUPLOADED, lastUploadedInMillis).commit();
    }

    public String getDbOauth() {
        return mPrefs.getString(KEY_DBOAUTH, DEF_DBOAUTH);
    }

    public void setDbOauth(String dropboxOauthKey) {
        mPrefs.edit().putString(KEY_DBOAUTH, dropboxOauthKey).commit();
    }

}
