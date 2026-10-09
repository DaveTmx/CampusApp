package com.example.campusapp.Data.repository;

import androidx.lifecycle.LiveData;

import com.example.campusapp.model.Session;

/**
 * What the ViewModels need for signing in and out. The real class (Retrofit
 * plus secure token storage) implements this; tests use a fake.
 */
public interface AuthRepository {

    /** The saved session, or null when nobody is signed in. */
    Session currentSession();

    void signIn(String username, String password, RepoCallback<Session> callback);

    /** Clears the token and this account's local data, then calls done. */
    void signOut(Runnable done);

    /** Becomes true when the server rejects the token; sync must pause until sign-in. */
    LiveData<Boolean> sessionExpired();
}
