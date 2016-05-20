package com.radicalninja.logger;

import android.text.TextUtils;

import com.menny.android.anysoftkeyboard.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileWriter {

    final boolean encryptionEnabled;
    final String filePath;
    final BufferedWriter writer;

    public FileWriter(final String filePath, final boolean append) throws IOException {
        this(filePath, append, BuildConfig.USE_ENCRYPTION);
    }

    private FileWriter(final String filePath, final boolean append, final boolean encryptionEnabled)
            throws IOException {
        this.filePath = filePath;
        this.encryptionEnabled = encryptionEnabled;
        writer = createFileWriter(append, encryptionEnabled);
    }

    private BufferedWriter createFileWriter(final boolean append, final boolean useEncryption) throws IOException {
        final File file = getFile();
        return useEncryption ?
                CipherUtils.flushableEncryptedBufferedWriter(file, append) :
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    }

    public File getFile() {
        return (!TextUtils.isEmpty(filePath)) ? new File(filePath) : null;
    }

    public void write(final String str) throws IOException {
        writer.write(str);
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    public File exportFile(final String filename) {
        // close file stream
        // create file reader
        // copy file data to new file
        // truncate file
        // open file stream again
        // return the File object with the copied data
        return null;
    }

}
