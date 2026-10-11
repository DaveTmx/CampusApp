package com.example.campusapp.util;

import android.os.Handler;
import android.os.Looper;

/**
 * Runs an action only after input has been quiet for {@code delayMs}.
 * Each submit() cancels the previous pending action, so typing "abc"
 * quickly produces one search, not three.
 */
public final class Debouncer {

    /** Abstracts the timer so unit tests can control time. */
    public interface Scheduler {
        void postDelayed(Runnable task, long delayMs);
        void cancel(Runnable task);
    }

    private final Scheduler scheduler;
    private final long delayMs;
    private Runnable pending;

    public Debouncer(Scheduler scheduler, long delayMs) {
        this.scheduler = scheduler;
        this.delayMs = delayMs;
    }

    public void submit(Runnable action) {
        cancelPending();
        pending = () -> {
            pending = null;
            action.run();
        };
        scheduler.postDelayed(pending, delayMs);
    }

    public void cancelPending() {
        if (pending != null) {
            scheduler.cancel(pending);
            pending = null;
        }
    }

    /** Scheduler backed by the main thread's Handler. Used in the app. */
    public static Scheduler mainThread() {
        Handler handler = new Handler(Looper.getMainLooper());
        return new Scheduler() {
            @Override
            public void postDelayed(Runnable task, long delayMs) {
                handler.postDelayed(task, delayMs);
            }

            @Override
            public void cancel(Runnable task) {
                handler.removeCallbacks(task);
            }
        };
    }
}
