package com.pepul.app.pepulliv.commons.util.concurrent;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {

    public static final ExecutorService BOUNDED     = Executors.newFixedThreadPool(4, new NumberedThreadFactory("pepulnow-bounded"));
    public static final ExecutorService SERIAL      = Executors.newSingleThreadExecutor(new NumberedThreadFactory("pepulnow-serial"));

    private AppExecutors() {}

    private static class NumberedThreadFactory implements ThreadFactory {

        private final String        baseName;
        private final AtomicInteger counter;

        NumberedThreadFactory(@NonNull String baseName) {
            this.baseName   = baseName;
            this.counter    = new AtomicInteger();
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, baseName + "-" + counter.getAndIncrement());
        }
    }
}