package com.example.campusapp.util;

/**
 * A value that should be shown once, such as a Snackbar message. LiveData
 * re-delivers its last value after rotation; wrapping the value in an Event
 * stops the same message appearing a second time.
 */
public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the value the first time, and null on every later call. */
    public synchronized T getIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /** Reads the value without marking it as shown. */
    public T peek() {
        return content;
    }
}
