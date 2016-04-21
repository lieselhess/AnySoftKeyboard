package com.radicalninja.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

class LogFileOutputStream extends FileOutputStream {

    final String filePath;

    LogFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
        super(file, append);
        filePath = file.getAbsolutePath();
    }

    public File getFile() {
        return new File(filePath);
    }

}
