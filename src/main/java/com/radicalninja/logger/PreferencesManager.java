package com.radicalninja.logger;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private final static String PREFS_NAME = "LoggerPrefs";

    private final static String KEY_LASTUSED = "LastUsed";
    private final static long DEF_LASTUSED = 0;

    private final static String KEY_DBOAUTH = "DbOauthKey";
    private final static String DEF_DBOAUTH = "";

    private static PreferencesManager sInstance;

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    public static PreferencesManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PreferencesManager(context);
        }
        return sInstance;
    }

    public PreferencesManager(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    public long getLastUsed() {
        return mPrefs.getLong(KEY_LASTUSED, DEF_LASTUSED);
    }

    public void setLastUsed(long lastUsedInMillis) {
        mEditor.putLong(KEY_LASTUSED, lastUsedInMillis).apply();
    }

    public String getDbOauth() {
        return mPrefs.getString(KEY_DBOAUTH, DEF_DBOAUTH);
    }

    public void setDbOauth(String dropboxOauthKey) {
        mEditor.putString(KEY_DBOAUTH, dropboxOauthKey).apply();
    }

}
