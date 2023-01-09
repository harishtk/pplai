package com.example.app.di;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.app.commons.util.AppForegroundObserver;
import com.example.app.commons.util.PersistentStore;
import com.example.app.service.websocket.AppWebSocket;
import com.example.app.service.websocket.WebSocketConnection;
import com.example.app.service.websocket.WebSocketFactory;

public class ApplicationDependencyProvider implements ApplicationDependencies.Provider {

    @SuppressWarnings({"UNUSED", "FieldCanBeLocal"})
    private final Context context;

    public ApplicationDependencyProvider(Application context) {
        this.context = context;
    }

    @NonNull
    @Override
    public AppWebSocket provideAppWebSocket() {
        return new AppWebSocket(provideWebSocketFactory());
    }

    @NonNull
    @Override
    public PersistentStore providePersistentStore() {
        return PersistentStore.Companion.getInstance(context);
    }

    @NonNull
    @Override
    public AppForegroundObserver provideAppForegroundObserver() {
        return new AppForegroundObserver();
    }

    private @NonNull WebSocketFactory provideWebSocketFactory() {
        return new WebSocketFactory() {
            @NonNull
            @Override
            public WebSocketConnection createWebSocket() {
                return new WebSocketConnection("normal");
            }
        };
    }
}
