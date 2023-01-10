package com.pepulai.app.di;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.pepulai.app.commons.util.AppForegroundObserver;
import com.pepulai.app.commons.util.PersistentStore;
import com.pepulai.app.service.websocket.AppWebSocket;
import com.pepulai.app.commons.util.AppForegroundObserver;
import com.pepulai.app.commons.util.PersistentStore;
import com.pepulai.app.service.websocket.AppWebSocket;

public class ApplicationDependencies {

    private static final Object LOCK = new Object();

    private static Application application;
    private static Provider provider;
    private static AppForegroundObserver appForegroundObserver;


    private static volatile PersistentStore persistentStore;
    private static volatile AppWebSocket appWebSocket;

    @MainThread
    public static void init(@NonNull Application application, @NonNull Provider provider) {
        synchronized (LOCK) {
            if (ApplicationDependencies.application != null || ApplicationDependencies.provider != null) {
                throw new IllegalStateException("Already initialized!");
            }

            ApplicationDependencies.application = application;
            ApplicationDependencies.provider    = provider;
            ApplicationDependencies.appForegroundObserver = provider.provideAppForegroundObserver();

            ApplicationDependencies.appForegroundObserver.begin();
            persistentStore = provider.providePersistentStore();
        }
    }

    public static AppForegroundObserver getAppForegroundObserver() { return appForegroundObserver; }

    public static PersistentStore getPersistentStore() { return persistentStore; }

    public static @NonNull AppWebSocket getAppWebSocket() {
        if (appWebSocket == null) {
            synchronized (LOCK) {
                if (appWebSocket == null) {
                    appWebSocket = provider.provideAppWebSocket();
                }
            }
        }
        return appWebSocket;
    }

    public interface Provider {
        @NonNull AppForegroundObserver provideAppForegroundObserver();
        @NonNull PersistentStore providePersistentStore();
        @NonNull AppWebSocket provideAppWebSocket();
    }
}
