package com.aiavatar.app.commons.util.io;

import androidx.room.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileUtils {

    private static final int BUFFER_SIZE = 8192;

    private FileUtils() {
    }

    public static void copyFile(File inFile, File outFile) {
        try (InputStream ins = new BufferedInputStream(Files.newInputStream(inFile.toPath()));
             OutputStream ous = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()))) {
            byte[] buf = new byte[BUFFER_SIZE];
            long nread = 0;
            int n;
            while ((n = ins.read(buf)) > 0) {
                ous.write(buf, 0, n);
                nread += n;
            }
            ous.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(InputStream ins, OutputStream ous) throws IOException {
        try (BufferedInputStream bins = new BufferedInputStream(ins);
            BufferedOutputStream bous = new BufferedOutputStream(ous)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = bins.read(buf)) > 0) {
                bous.write(buf, 0, n);
            }
            bous.flush();
        }
    }

}
