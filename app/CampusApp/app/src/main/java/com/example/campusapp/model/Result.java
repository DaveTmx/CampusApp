package com.example.campusapp.model;

/**
 * What a repository call ended with. Each outcome the lab manual names has its
 * own status, so a ViewModel never has to guess from an error string.
 */
public final class Result<T> {

    public enum Status {
        /** Accepted by the server. */
        SUCCESS,
        /** Saved on the phone and queued; the server has not confirmed it yet. */
        PENDING,
        /** Another student already has this student number. */
        DUPLICATE_NUMBER,
        /** The group already has 15 active students. */
        GROUP_FULL,
        /** Someone else changed the record first; see serverCopy. */
        CONFLICT,
        /** The server rejected a field. */
        INVALID,
        /** Wrong username, password or claim code. */
        BAD_CREDENTIALS,
        /** The student was deleted, on this phone or by someone else. */
        NOT_FOUND,
        /** Signed in, but not allowed to do this. */
        NOT_ALLOWED,
        SESSION_EXPIRED,
        /** No connection, and this action cannot be queued. */
        OFFLINE,
        ERROR
    }

    public final Status status;
    /** The saved or loaded value, when there is one. */
    public final T data;
    /** Only for CONFLICT: the record as the server has it now. */
    public final T serverCopy;

    private Result(Status status, T data, T serverCopy) {
        this.status = status;
        this.data = data;
        this.serverCopy = serverCopy;
    }

    public static <T> Result<T> of(Status status, T data) {
        return new Result<>(status, data, null);
    }

    public static <T> Result<T> conflict(T localProposal, T serverCopy) {
        return new Result<>(Status.CONFLICT, localProposal, serverCopy);
    }

    /** True when the user's work is safe, either on the server or in the local queue. */
    public boolean isSaved() {
        return status == Status.SUCCESS || status == Status.PENDING;
    }
}
