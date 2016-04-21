package com.radicalninja.logger;

import android.provider.Settings;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.anysoftkeyboard.utils.Log;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;

import java.io.File;

class AwsUtil {

    public interface FileTransferCallback {
        void onStart();
        void onComplete();
        void onCancel();
        void onError();
    }

    private static final String TAG = AwsUtil.class.getCanonicalName();

    private static final Regions REGION = Regions.fromName(BuildConfig.AWS_REGION.toLowerCase());

    private static CognitoCachingCredentialsProvider credentialsProvider;
    private static String userId;

    private static void init() {
        initCredentialsProvider();
        initUserId();
    }

    private static void initCredentialsProvider() {
        if (credentialsProvider == null) {
            // Initialize the Amazon Cognito credentials provider
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    AnyApplication.getInstance(),
                    BuildConfig.AWS_POOL_ID,
                    REGION
            );
        }
    }

    private static void initUserId() {
        if (TextUtils.isEmpty(userId)) {
            userId = Settings.Secure.getString(
                    AnyApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }

    public static void uploadFileToBucket(
            final File file, final String filename, final FileTransferCallback callback) {
        init();
        // S3 client
        final AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        s3.setRegion(Region.getRegion(REGION));
        // Transfer Utility
        final TransferUtility transferUtility =
                new TransferUtility(s3, AnyApplication.getInstance());
        // Upload the file
        final TransferObserver observer =
                transferUtility.upload(BuildConfig.AWS_BUCKET_NAME, filename, file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                final String logLine =
                        String.format("onStateChanged: Transfer ID: %d | New State: %s", id, state);
                Log.d(TAG, logLine);
                switch (state) {
                    case IN_PROGRESS:
                        Log.d(TAG, String.format("Transfer ID %d has begun", id));
                        callback.onStart();
                        break;
                    case COMPLETED:
                        Log.d(TAG, String.format("Transfer ID %d has completed", id));
                        callback.onComplete();
                        // log: finished
                        // delete: the local copy of the file just uploaded.
                        break;
                    case CANCELED:
                        Log.d(TAG, String.format("Transfer ID %d has been cancelled", id));
                        callback.onCancel();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) { }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, String.format("onError: Transfer ID: %d", id), ex);
                callback.onError();
            }
        });
    }

}
