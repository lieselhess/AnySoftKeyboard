package com.radicalninja.anykeylogger;

import android.content.Context;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

public class DropboxUtil {

    final static private String APP_KEY = "yhmtrpmv2aulexa";
    final static private String APP_SECRET = "mwqbqhk47pzv1ea";

    private Context mContext;

    private boolean mDBDidAuth = false;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    public DropboxUtil(Context context) {
        mContext = context;
        setupDBSession();
    }

    private void setupDBSession() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        String dbOauthToken = PreferencesManager.getInstance(mContext).getDbOauth();
        AndroidAuthSession session;
        if (!dbOauthToken.isEmpty()) {
            session = new AndroidAuthSession(appKeys, dbOauthToken);
        } else {
            session = new AndroidAuthSession(appKeys);
        }
        mDBApi = new DropboxAPI<>(session);
        if (!mDBApi.getSession().isLinked()) {
            mDBApi.getSession().startOAuth2Authentication(mContext);
            mDBDidAuth  = true;
        }
    }

    /**
     * Runs authentication finalization code. Call this in your activities' onResume() method.
     */
    public void finishAuthentication() {
        if (mDBDidAuth && mDBApi.getSession().authenticationSuccessful()) {
            try {
                mDBApi.getSession().finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                PreferencesManager.getInstance(mContext).setDbOauth(accessToken);
                mDBDidAuth = false;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
}
