package com.example.campusapp.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Shared threads for database work and for returning results to the UI.
 * Room and network work never run on the main thread.
 */
public final class AppExecutors {

    private static final AppExecutors INSTANCE = new AppExecutors();

    private final Executor diskIO = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    public static AppExecutors get() {
        return INSTANCE;
    }

    /** Background thread for Room reads and writes. */
    public Executor diskIO() {
        return diskIO;
    }

    /** Runs the task on the main thread, so LiveData can be set safely. */
    public void runOnMain(Runnable task) {
        mainThread.post(task);
    }
}
