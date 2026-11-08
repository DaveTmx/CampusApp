package com.example.campusapp.data.model;

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
        /** Another account already uses this email address. */
        DUPLICATE_EMAIL,
        /** The group already has 16 active students. */
        GROUP_FULL,
        /** Someone else changed the record first; see serverCopy. */
        CONFLICT,
        /** The server rejected a field. */
        INVALID,
        /** Wrong username or password. */
        BAD_CREDENTIALS,
        /** The student was deleted, on this phone or by someone else. */
        NOT_FOUND,
        /** Signed in, but not allowed to do this. */
        NOT_ALLOWED,
        SESSION_EXPIRED,
        /** No connection, and this action cannot be queued. */
        OFFLINE,
        /** Too many wrong passwords. The account is locked for a while. */
        LOCKED_OUT,
        /** Password was right for a staff account; the emailed code is still needed. */
        NEEDS_CODE,
        /** The email address has not been verified yet. */
        NOT_VERIFIED,
        /** A lecturer signed in on the student screen. Use the staff sign-in. */
        USE_STAFF_SIGN_IN,
        /** The emailed code was wrong. The user may try again. */
        WRONG_CODE,
        /** The emailed code has expired or was used up. Start the sign-in again. */
        CODE_EXPIRED,
        /** A code was sent a moment ago. Wait before asking for another. */
        CODE_TOO_SOON,
        /** The email service could not send the code. */
        CODE_NOT_SENT,
        /** Too many requests from this phone. Wait and try again. */
        RATE_LIMITED,
        /** The account was created on the server. The student must open the link in their email first. */
        CHECK_EMAIL,
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
