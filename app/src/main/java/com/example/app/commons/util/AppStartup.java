package com.example.app.commons.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.example.app.commons.util.concurrent.AppExecutors;

import java.util.LinkedList;
import java.util.List;

public class AppStartup {

    private static final String TAG = "AppStartup";

    private final long UI_WAIT_TIME = 500;
    private final long FAILSAFE_RENDER_TIME = 2500;

    private final List<Task> blocking;
    private final List<Task> nonBlocking;
    private final List<Task> postRender;
    private final Handler postRenderHandler;

    private int outstandingCriticalRenderEvents;

    private long applicationStartTime;
    private long renderStartTime;
    private long renderEndTime;

    private AppStartup() {
        this.blocking = new LinkedList<>();
        this.nonBlocking = new LinkedList<>();
        this.postRender = new LinkedList<>();
        this.postRenderHandler = new Handler(Looper.getMainLooper());
    }

    public void onApplicationCreate() { this.applicationStartTime = System.currentTimeMillis(); }

    @MainThread
    @NonNull
    public AppStartup addBlocking(@NonNull String name, @NonNull Runnable task) {
        blocking.add(new Task(name, task));
        return this;
    }

    @MainThread
    @NonNull
    public AppStartup addNonBlocking(@NonNull String name, @NonNull Runnable task) {
        nonBlocking.add(new Task(name, task));
        return this;
    }

    @MainThread
    @NonNull
    public AppStartup addPostRender(@NonNull Runnable task) {
        postRender.add(new Task("", task));
        return this;
    }

    @MainThread
    public void onCriticalRenderEventStart() {
        if (outstandingCriticalRenderEvents == 0 && postRender.size() > 0) {
            Log.i(TAG, "Received first critical render event.");
            renderStartTime = System.currentTimeMillis();

            postRenderHandler.removeCallbacksAndMessages(null);
            postRenderHandler.postDelayed(() -> {
                Log.w(TAG, "Reached the failsafe event for post-render! Either someone forgot to call #onRenderend(), the activity was started while the phone was locked, or app start is taking a very long time.");
                executePostRender();
            }, FAILSAFE_RENDER_TIME);
        }

        outstandingCriticalRenderEvents++;
    }

    @MainThread
    public void onCriticalRenderEventEnd() {
        if (outstandingCriticalRenderEvents <= 0) {
            Log.w(TAG, "Too many end events! onCriticalRenderEventsStart/End was mismanaged.");
        }

        outstandingCriticalRenderEvents = Math.max(outstandingCriticalRenderEvents - 1, 0);

        if (outstandingCriticalRenderEvents == 0 && postRender.size() > 0) {
            renderEndTime = System.currentTimeMillis();

            Log.i(TAG, "First render has finished. " +
                    "Cold Start: " + (renderEndTime - applicationStartTime) + " ms, " +
                    "Render Time: " + (renderEndTime - renderStartTime) + " ms");

            postRenderHandler.removeCallbacksAndMessages(null);
            executePostRender();
        }
    }

    @MainThread
    public void execute() {

        for (Task task : blocking) {
            task.runnable.run();
        }
        blocking.clear();

        for (Task task : nonBlocking) {
            AppExecutors.BOUNDED.execute(task.getRunnable());
        }
        nonBlocking.clear();

        postRenderHandler.postDelayed(this::executePostRender, UI_WAIT_TIME);
    }

    private void executePostRender() {
        for (Task t : postRender) { AppExecutors.BOUNDED.execute(t.getRunnable()); }
        postRender.clear();
    }

    @NonNull
    public static AppStartup getInstance() { return AppStartupSingleton.INSTANCE; }

    private static class AppStartupSingleton {
        private static final AppStartup INSTANCE = new AppStartup();
    }

    private static class Task {
        private final String name;
        private final Runnable runnable;

        private Task(@NonNull String name, @NonNull Runnable runnable) {
            this.name = name;
            this.runnable = runnable;
        }

        @NonNull
        public String getName() { return name; }

        @NonNull
        public Runnable getRunnable() { return runnable; }
    }
}
