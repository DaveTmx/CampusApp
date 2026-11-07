package com.example.campusapp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusapp.data.local.AppDatabase;
import com.example.campusapp.data.remote.ApiService;
import com.example.campusapp.data.remote.AuthDto;
import com.example.campusapp.data.remote.TokenStore;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.AppExecutors;
import com.example.campusapp.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sign-in against the backend (/api/auth). The server decides who may sign in. The phone keeps
 * only the token and the account it belongs to.
 *
 * Students: signIn(), with the email address they registered with.
 * Lecturers: startStaffSignIn() checks the password and the server emails a code. Then
 * verifyStaffCode() exchanges that code for a session.
 *
 * Callbacks arrive on a background thread, so ViewModels answer with postValue().
 */
public class RemoteAuthRepository implements AuthRepository {

    private final Context appContext;
    private final ApiService api;
    private final TokenStore tokens;
    private final Gson gson = new Gson();
    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>(false);

    public RemoteAuthRepository(Context context, ApiService api, TokenStore tokens) {
        this.appContext = context.getApplicationContext();
        this.api = api;
        this.tokens = tokens;
    }

    /** The signed-in person's name, as the server returned it at sign-in. Null when signed out. */
    public String signedInName() {
        return tokens.fullName();
    }

    @Override
    public Session currentSession() {
        Session session = tokens.session();
        if (session != null) {
            SessionManager.signIn(session);
        }
        return session;
    }

    @Override
    public void signIn(String identifier, String password, RepoCallback<Session> callback) {
        api.login(new AuthDto.StudentLoginRequest(clean(identifier), password))
                .enqueue(sessionCallback(callback));
    }

    @Override
    public void register(String fullName, String email, String password, String studentNumber,
                         String programmeCode, RepoCallback<Void> callback) {
        AuthDto.RegisterRequest body = new AuthDto.RegisterRequest(
                fullName == null ? "" : fullName.trim(),
                clean(email), password, studentNumber == null ? "" : studentNumber.trim(), programmeCode);
        api.register(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onResult(Result.of(Result.Status.CHECK_EMAIL, null));
                } else {
                    callback.onResult(failure(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    @Override
    public void requestPasswordReset(String email, RepoCallback<Void> callback) {
        api.forgotPassword(new AuthDto.EmailRequest(clean(email))).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onResult(Result.<Void>of(Result.Status.SUCCESS, null));
                } else {
                    callback.onResult(failure(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    @Override
    public void startStaffSignIn(String email, String password, RepoCallback<String> callback) {
        api.staffLogin(new AuthDto.LoginRequest(clean(email), password))
                .enqueue(new Callback<AuthDto.ChallengeResponse>() {
                    @Override
                    public void onResponse(Call<AuthDto.ChallengeResponse> call,
                                           Response<AuthDto.ChallengeResponse> response) {
                        AuthDto.ChallengeResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.challengeId != null) {
                            callback.onResult(Result.of(Result.Status.NEEDS_CODE, body.challengeId));
                        } else {
                            callback.onResult(failure(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthDto.ChallengeResponse> call, Throwable t) {
                        callback.onResult(Result.of(offlineOrError(t), null));
                    }
                });
    }

    @Override
    public void verifyStaffCode(String challengeId, String code, RepoCallback<Session> callback) {
        String typed = code == null ? "" : code.trim();
        api.verifyStaffCode(new AuthDto.VerifyCodeRequest(challengeId, typed))
                .enqueue(sessionCallback(callback));
    }

    /** Not used on the backend path. Lecturers sign in with verifyStaffCode(). */
    @Override
    public void completeStaffSignIn(String username, RepoCallback<Session> callback) {
        callback.onResult(Result.of(Result.Status.NOT_ALLOWED, null));
    }

    /**
     * Clears the local session first, so the phone is signed out even if the network is down.
     * The server revocation is sent with the token captured before clearing, and its outcome
     * does not change what the user sees.
     */
    @Override
    public void signOut(Runnable done) {
        String token = tokens.token();
        tokens.clear();
        SessionManager.signOut();
        clearCachedStudentData();

        if (token != null) {
            api.logout("Bearer " + token).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                }
            });
        }
        if (done != null) {
            done.run();
        }
    }

    @Override
    public LiveData<Boolean> sessionExpired() {
        return sessionExpired;
    }

    // ----- Internals -----

    private Callback<AuthDto.LoginResponse> sessionCallback(RepoCallback<Session> callback) {
        return new Callback<AuthDto.LoginResponse>() {
            @Override
            public void onResponse(Call<AuthDto.LoginResponse> call,
                                   Response<AuthDto.LoginResponse> response) {
                AuthDto.LoginResponse body = response.body();
                if (response.isSuccessful() && body != null && body.token != null && body.user != null) {
                    callback.onResult(Result.of(Result.Status.SUCCESS, startSession(body)));
                } else {
                    callback.onResult(failure(response));
                }
            }

            @Override
            public void onFailure(Call<AuthDto.LoginResponse> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        };
    }

    private Session startSession(AuthDto.LoginResponse body) {
        String role = "lecturer".equalsIgnoreCase(body.user.role)
                ? Session.ROLE_LECTURER
                : Session.ROLE_STUDENT;
        // Students are identified in the app by their student record, as before. Lecturers by their account.
        String accountId = Session.ROLE_STUDENT.equals(role) && body.user.studentId != null
                ? body.user.studentId
                : (body.user.id == null ? "" : body.user.id);
        long expiresAt = System.currentTimeMillis() + body.expiresIn * 1000L;
        tokens.save(body.token, expiresAt, accountId, role, body.user.fullName, body.user.email);

        Session session = new Session(accountId, role);
        SessionManager.signIn(session);
        return session;
    }

    /** Maps a failed reply to the status the screens already know how to show. */
    private <T> Result<T> failure(Response<?> response) {
        AuthDto.ErrorBody error = parseError(response);
        String code = error == null || error.error == null ? "" : error.error;
        switch (code) {
            case "INVALID_CREDENTIALS":
                return Result.of(Result.Status.BAD_CREDENTIALS, null);
            case "ACCOUNT_LOCKED":
                return Result.of(Result.Status.LOCKED_OUT, null);
            case "EMAIL_NOT_VERIFIED":
                return Result.of(Result.Status.NOT_VERIFIED, null);
            case "STAFF_SIGN_IN_REQUIRED":
                return Result.of(Result.Status.USE_STAFF_SIGN_IN, null);
            case "CODE_ALREADY_SENT":
                return Result.of(Result.Status.CODE_TOO_SOON, null);
            case "CODE_NOT_SENT":
                return Result.of(Result.Status.CODE_NOT_SENT, null);
            case "CODE_INCORRECT":
                return Result.of(Result.Status.WRONG_CODE, null);
            case "CODE_INVALID":
                return Result.of(Result.Status.CODE_EXPIRED, null);
            case "VALIDATION_FAILED":
                return Result.of(Result.Status.INVALID, null);
            case "DUPLICATE_EMAIL":
                return Result.of(Result.Status.DUPLICATE_EMAIL, null);
            case "DUPLICATE_NUMBER":
                return Result.of(Result.Status.DUPLICATE_NUMBER, null);
            case "TOKEN_REVOKED":
            case "INVALID_TOKEN":
                sessionExpired.postValue(true);
                return Result.of(Result.Status.SESSION_EXPIRED, null);
            default:
                break;
        }
        if (response.code() == 429) {
            return Result.of(Result.Status.RATE_LIMITED, null);
        }
        return Result.of(Result.Status.ERROR, null);
    }

    private AuthDto.ErrorBody parseError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            return gson.fromJson(response.errorBody().string(), AuthDto.ErrorBody.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    private static Result.Status offlineOrError(Throwable t) {
        return t instanceof IOException ? Result.Status.OFFLINE : Result.Status.ERROR;
    }

    private static String clean(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Same as LocalAuthRepository: the roster cache and any unsent draft belong to the signed-out user. */
    private void clearCachedStudentData() {
        AppExecutors.get().diskIO().execute(() -> {
            AppDatabase.getInstance(appContext).clearAllTables();
            appContext.getSharedPreferences(LocalStudentRepository.DRAFT_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().apply();
        });
    }
}
