package com.example.campusapp.data.repository;

import androidx.lifecycle.LiveData;

import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;

/**
 * What the ViewModels need for signing in and out. The backend implementation is
 * RemoteAuthRepository. LocalAuthRepository is still used for registration and password reset.
 */
public interface AuthRepository {

    /** The saved session, or null when nobody is signed in. */
    Session currentSession();

    /** Students. The email address or 9-digit student number, and the password. */
    void signIn(String identifier, String password, RepoCallback<Session> callback);

    /**
     * Lecturers, step 1. Checks the password. On success the server emails a code and the result
     * is NEEDS_CODE, with the challenge id as its data.
     */
    default void startStaffSignIn(String email, String password, RepoCallback<String> callback) {
        callback.onResult(Result.of(Result.Status.ERROR, null));
    }

    /** Lecturers, step 2. Exchanges the emailed code for a session. */
    default void verifyStaffCode(String challengeId, String code, RepoCallback<Session> callback) {
        callback.onResult(Result.of(Result.Status.ERROR, null));
    }

    /** Local-only staff step. The backend flow uses verifyStaffCode instead. */
    void completeStaffSignIn(String username, RepoCallback<Session> callback);

    /**
     * Students. Creates the account on the server. Result is CHECK_EMAIL on success, or
     * DUPLICATE_EMAIL / DUPLICATE_NUMBER / INVALID when the server refuses the details.
     */
    default void register(String fullName, String email, String password, String studentNumber,
                          String programmeCode, RepoCallback<Void> callback) {
        callback.onResult(Result.of(Result.Status.ERROR, null));
    }

    /** Asks the server to email a reset link. Always SUCCESS when the request was received. */
    default void requestPasswordReset(String email, RepoCallback<Void> callback) {
        callback.onResult(Result.of(Result.Status.ERROR, null));
    }

    /** Clears the token and this account's local data, then calls done. */
    void signOut(Runnable done);

    /** Becomes true when the server rejects the token; sync must pause until sign-in. */
    LiveData<Boolean> sessionExpired();
}
