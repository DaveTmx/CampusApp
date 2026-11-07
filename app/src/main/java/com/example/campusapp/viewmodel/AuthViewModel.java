package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.R;
import com.example.campusapp.data.repository.AuthRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.Event;
import com.example.campusapp.util.Validation;

/**
 * State for the sign-in screens, and for logging out from any screen.
 * Passwords are passed in, used once and never stored here.
 *
 * Students: signIn(). Lecturers: startStaffSignIn() checks the password and the server emails a
 * code. getCodeRequest() then carries the challenge id. The screen asks for the code and calls
 * verifyStaffCode(). A wrong code is reported through getCodeError(), so the screen can keep its
 * dialog open.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> error = new MutableLiveData<>();
    private final MutableLiveData<Session> session = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> codeRequest = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> codeError = new MutableLiveData<>();

    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
        // Someone who is already signed in skips the form.
        session.setValue(repository.currentSession());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** String resource ID of the error to show on the form, or null for none. */
    public LiveData<Integer> getError() {
        return error;
    }

    /** Null while signed out. The screen opens the student or lecturer home from the role. */
    public LiveData<Session> getSession() {
        return session;
    }

    /** Fires once when a lecturer's password was right. The value is the challenge id. */
    public LiveData<Event<String>> getCodeRequest() {
        return codeRequest;
    }

    /** Fires once for each failed code. The value is a string resource ID. */
    public LiveData<Event<Integer>> getCodeError() {
        return codeError;
    }

    /** True once the server rejects the saved token; the screen should return to sign-in. */
    public LiveData<Boolean> getSessionExpired() {
        return repository.sessionExpired();
    }

    /** Students. */
    public void signIn(String username, String password) {
        if (!canStart(username, password)) {
            return;
        }
        repository.signIn(Validation.clean(username), password, result -> {
            loading.postValue(false);
            if (result.isSaved()) {
                session.postValue(result.data);
            } else {
                error.postValue(StatusMessages.forStatus(result.status));
            }
        });
    }

    /** Lecturers, step 1: the password. */
    public void startStaffSignIn(String email, String password) {
        if (!canStart(email, password)) {
            return;
        }
        repository.startStaffSignIn(Validation.clean(email), password, result -> {
            loading.postValue(false);
            if (result.status == Result.Status.NEEDS_CODE) {
                codeRequest.postValue(new Event<>(result.data));
            } else {
                error.postValue(StatusMessages.forStatus(result.status));
            }
        });
    }

    /** Lecturers, step 2: the code from the email. */
    public void verifyStaffCode(String challengeId, String code) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return; // a second tap while the first check is still running
        }
        loading.setValue(true);
        repository.verifyStaffCode(challengeId, code, result -> {
            loading.postValue(false);
            if (result.isSaved()) {
                session.postValue(result.data);
            } else {
                codeError.postValue(new Event<>(StatusMessages.forStatus(result.status)));
            }
        });
    }

    public void signOut() {
        repository.signOut(() -> session.postValue(null));
    }

    /** Shared checks for the sign-in forms. Sets the error and returns false when a field is missing. */
    private boolean canStart(String username, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return false; // a second tap while the first request is still running
        }
        if (Validation.clean(username).isEmpty()) {
            error.setValue(R.string.error_username_required);
            return false;
        }
        if (password == null || password.isEmpty()) {
            error.setValue(R.string.error_password_required);
            return false;
        }
        error.setValue(null);
        loading.setValue(true);
        return true;
    }
}
