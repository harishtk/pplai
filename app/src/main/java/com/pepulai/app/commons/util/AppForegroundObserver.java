package com.pepulai.app.commons.util;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.pepulai.app.commons.util.concurrent.ThreadUtil;
import com.pepulai.app.commons.util.concurrent.ThreadUtil;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AppForegroundObserver {

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private volatile Boolean isForegrounded = null;

    @MainThread
    public void begin() {
        ThreadUtil.assertMainThread();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) { onForeground(); }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) { onBackground(); }
        });
    }

    public boolean isForegrounded() { return isForegrounded != null && isForegrounded; }

    @AnyThread
    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);

        if (isForegrounded != null) {
            if (isForegrounded) {
                listener.onForeground();
            } else {
                listener.onBackground();
            }
        }
    }

    @AnyThread
    public void removeListener(@NonNull Listener listener) { listeners.remove(listener); }

    @MainThread
    private void onForeground() {
        isForegrounded = true;

        for (Listener listener : listeners) {
            listener.onForeground();
        }
    }

    @MainThread
    private void onBackground() {
        isForegrounded = false;

        for (Listener listener : listeners) {
            listener.onBackground();
        }
    }

    public interface Listener {
        default void onForeground() {}
        default void onBackground() {}
    }
}
