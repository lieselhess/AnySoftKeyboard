package com.radicalninja.anykeylogger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.menny.android.anysoftkeyboard.R;

public class DropboxUploaderActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mUploadButton;
    private DropboxUtil mDropbox;
    private TextView mLastUploaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_uploader);

        mUploadButton = (Button) findViewById(R.id.upload_button);
        mLastUploaded = (TextView) findViewById(R.id.last_uploaded_date);

        long lastUploaded = PreferencesManager.getInstance(this).getLastUploaded();
        if (lastUploaded == 0) {
            mLastUploaded.setText("Never");
        } else {
            // TODO: Time formatting here, setText on mLastUploaded.
        }

        mDropbox = new DropboxUtil(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDropbox.finishAuthentication();
    }

    @Override
    public void onClick(View v) {
        // TODO: Upload file to dropbox, renamed to "Logger_startDate-endDate"
        // TODO: Delete local file / clear it out.
        // TODO: Update the lastUploaded preferences manager
    }
}
