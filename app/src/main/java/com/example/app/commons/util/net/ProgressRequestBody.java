package com.example.app.commons.util.net;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by Haris on 14/7/18.
 */

public class ProgressRequestBody extends RequestBody {
    private final String mContentType;
    private File mFile;
    private String mPath;
    private ProgressCallback mListener;

    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final long PROGRESS_DEBOUNCE_MILLIS = 10;

    public interface ProgressCallback {
        void onProgressUpdate(int percentage);

        void onError();
    }

    public ProgressRequestBody(final File file, final String contentType, final ProgressCallback listener) {
        mFile = file;
        mContentType = contentType;
        mListener = listener;
    }

    @Override
    public MediaType contentType() {
        // i want to upload only images
        return MediaType.parse(mContentType);
    }

    @Override
    public long contentLength() throws IOException {
        return mFile.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        try (FileInputStream in = new FileInputStream(mFile)) {
            long uploaded = 0;
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            long lastUpdate = 0;
            while ((read = in.read(buffer)) != -1) {

                if (System.currentTimeMillis() > lastUpdate + PROGRESS_DEBOUNCE_MILLIS) {
                    // update progress on UI thread
                    handler.post(new ProgressUpdater(uploaded, fileLength));
                    lastUpdate = System.currentTimeMillis();
                }

                uploaded += read;
                sink.write(buffer, 0, read);
            }
            handler.post(new ProgressUpdater(fileLength, fileLength));
        } catch (IOException e) {
            mListener.onError();
        }
        //  mListener.onError();
    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;

        public ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            if (mUploaded > 0) {
                mListener.onProgressUpdate((int) (100 * mUploaded / mTotal));
            }
        }
    }
}