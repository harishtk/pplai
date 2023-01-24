package com.aiavatar.app.commons.util.concurrent;

public class ThreadSafeCounter {

    private final Object mLock = new Object();

    private int counter;

    public ThreadSafeCounter() {
        this(0);
    }

    public ThreadSafeCounter(int initialCount) {
        this.counter = initialCount;
    }

    synchronized public int increment() {
        synchronized (mLock) {
            counter++;
        }
        return counter;
    }

    synchronized public int getCount() {
        return counter;
    }
}
