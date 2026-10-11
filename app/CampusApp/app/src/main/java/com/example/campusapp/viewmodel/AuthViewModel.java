package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.Data.repository.AuthRepository;
import com.example.campusapp.R;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.Validation;

/**
 * State for the sign-in screen, and for logging out from any screen.
 * The password is passed in, used once and never stored here.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> error = new MutableLiveData<>();
    private final MutableLiveData<Session> session = new MutableLiveData<>();

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
        // Someone who is already signed in skips the form.
        session.setValue(repository.currentSession());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** String resource ID of the error to show, or null for none. */
    public LiveData<Integer> getError() {
        return error;
    }

    /** Null while signed out. The screen opens the student or lecturer home from the role. */
    public LiveData<Session> getSession() {
        return session;
    }

    /** True once the server rejects the saved token; the screen should return to sign-in. */
    public LiveData<Boolean> getSessionExpired() {
        return repository.sessionExpired();
    }

    public void signIn(String username, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return; // a second tap while the first request is still running
        }
        String user = Validation.clean(username);
        if (user.isEmpty()) {
            error.setValue(R.string.error_username_required);
            return;
        }
        if (password == null || password.isEmpty()) {
            error.setValue(R.string.error_password_required);
            return;
        }

        error.setValue(null);
        loading.setValue(true);
        repository.signIn(user, password, result -> {
            loading.postValue(false);
            if (result.isSaved()) {
                session.postValue(result.data);
            } else {
                error.postValue(StatusMessages.forStatus(result.status));
            }
        });
    }

    public void signOut() {
        repository.signOut(() -> session.postValue(null));
    }
}
