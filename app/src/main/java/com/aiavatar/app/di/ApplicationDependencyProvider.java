package com.aiavatar.app.di;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.aiavatar.app.commons.util.AppForegroundObserver;
import com.aiavatar.app.commons.util.PersistentStore;
import com.aiavatar.app.service.websocket.AppWebSocket;
import com.aiavatar.app.service.websocket.WebSocketConnection;
import com.aiavatar.app.service.websocket.WebSocketFactory;

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
