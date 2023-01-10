package com.pepulai.app.commons.util.concurrent;

import android.os.Handler;
import android.os.Looper;

public final class ThreadUtil {

    private static volatile Handler handler;

    public static volatile boolean enforceAssertions = false;

    private ThreadUtil() {}

    private static Handler getHandler() {
        if (handler == null) {
            synchronized (ThreadUtil.class) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return handler;
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void assertMainThread() {
        if (!isMainThread() && enforceAssertions) {
            throw new AssertionError("Must run on main thread.");
        }
    }

    public static void assertNotMainThread() {
        if (isMainThread() && enforceAssertions) {
            throw new AssertionError("Cannot run on main thread.");
        }
    }
}