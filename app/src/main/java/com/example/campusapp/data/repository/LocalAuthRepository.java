package com.example.campusapp.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusapp.data.AppData;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Sign-in for the app while there is no server. Accounts and the saved session live in
 * SharedPreferences on this phone. Passwords are stored as a salted SHA-256 hash, never as
 * plain text. When the real backend exists, a Retrofit-based class implements AuthRepository
 * instead and nothing above this layer changes.
 *
 * Every call answers on the thread that made it, so a ViewModel can use postValue() either way.
 */
public class LocalAuthRepository implements AuthRepository {

    /** Demo accounts that exist on first run, so the app can be tried straight away. */
    public static final String DEMO_STUDENT_NUMBER = "202419080";
    public static final String DEMO_STUDENT_PASSWORD = "student123";
    public static final String DEMO_LECTURER_EMAIL = "muleba@gmail.com";
    public static final String DEMO_LECTURER_PASSWORD = "lecturer123";
    public static final String DEMO_LECTURER_ACCOUNT_ID = "lecturer-1";

    private static final String PREFS = "campus_accounts";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_SESSION_ACCOUNT = "session_account";
    private static final String KEY_SESSION_ROLE = "session_role";

    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>(false);

    public LocalAuthRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_ACCOUNTS)) {
            createAccount(DEMO_STUDENT_NUMBER, DEMO_STUDENT_PASSWORD,
                    AppData.CURRENT_STUDENT_ID, Session.ROLE_STUDENT);
            createAccount(DEMO_LECTURER_EMAIL, DEMO_LECTURER_PASSWORD,
                    DEMO_LECTURER_ACCOUNT_ID, Session.ROLE_LECTURER);
        }
    }

    @Override
    public synchronized Session currentSession() {
        String account = prefs.getString(KEY_SESSION_ACCOUNT, null);
        String role = prefs.getString(KEY_SESSION_ROLE, null);
        if (account == null || role == null) {
            return null;
        }
        Session session = new Session(account, role);
        SessionManager.signIn(session);
        return session;
    }

    @Override
    public synchronized void signIn(String username, String password,
                                    RepoCallback<Session> callback) {
        JSONObject account = find(normalise(username));
        if (account == null || password == null
                || !account.optString("hash").equals(hash(account.optString("salt"), password))) {
            callback.onResult(Result.of(Result.Status.BAD_CREDENTIALS, null));
            return;
        }
        Session session = new Session(account.optString("accountId"), account.optString("role"));
        prefs.edit()
                .putString(KEY_SESSION_ACCOUNT, session.accountId)
                .putString(KEY_SESSION_ROLE, session.role)
                .apply();
        SessionManager.signIn(session);
        callback.onResult(Result.of(Result.Status.SUCCESS, session));
    }

    @Override
    public synchronized void signOut(Runnable done) {
        prefs.edit().remove(KEY_SESSION_ACCOUNT).remove(KEY_SESSION_ROLE).apply();
        SessionManager.signOut();
        if (done != null) {
            done.run();
        }
    }

    @Override
    public LiveData<Boolean> sessionExpired() {
        return sessionExpired;
    }

    /**
     * Adds an account. Returns false when the username is already taken.
     * Used when a student registers.
     */
    public synchronized boolean createAccount(String username, String password,
                                              String accountId, String role) {
        String key = normalise(username);
        if (key.isEmpty() || password == null || find(key) != null) {
            return false;
        }
        try {
            JSONArray accounts = new JSONArray(prefs.getString(KEY_ACCOUNTS, "[]"));
            String salt = newSalt();
            JSONObject account = new JSONObject();
            account.put("username", key);
            account.put("salt", salt);
            account.put("hash", hash(salt, password));
            account.put("accountId", accountId);
            account.put("role", role);
            accounts.put(account);
            prefs.edit().putString(KEY_ACCOUNTS, accounts.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    // ----- Internals -----

    private JSONObject find(String key) {
        try {
            JSONArray accounts = new JSONArray(prefs.getString(KEY_ACCOUNTS, "[]"));
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject account = accounts.getJSONObject(i);
                if (key.equals(account.optString("username"))) {
                    return account;
                }
            }
        } catch (JSONException ignored) {
            // Unreadable account list: treat as "no such account".
        }
        return null;
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on Android", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format(Locale.ROOT, "%02x", b));
        }
        return out.toString();
    }
}
