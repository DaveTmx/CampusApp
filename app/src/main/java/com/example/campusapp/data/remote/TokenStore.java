package com.example.campusapp.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.campusapp.model.Session;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Keeps the backend token and who it belongs to. A token past its expiry is treated as absent.
 *
 * Stored in EncryptedSharedPreferences: keys and values are encrypted, and the key lives in the
 * Android Keystore. The old plain file from earlier builds is deleted; that means signing in once more.
 */
public final class TokenStore {

    private static final String PREFS = "campus_session_secure";
    private static final String LEGACY_PREFS = "campus_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_ACCOUNT = "account_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "full_name";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public TokenStore(Context context) {
        Context app = context.getApplicationContext();
        app.deleteSharedPreferences(LEGACY_PREFS); // unencrypted leftovers from earlier builds
        prefs = open(app);
    }

    private static SharedPreferences open(Context app) {
        try {
            MasterKey key = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    app,
                    PREFS,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            // Without secure storage the app must not keep a token in plain text.
            throw new IllegalStateException("Secure storage is unavailable on this device", e);
        }
    }

    public synchronized void save(String token, long expiresAtMillis, String accountId, String role,
                                  String fullName, String email) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_EXPIRES_AT, expiresAtMillis)
                .putString(KEY_ACCOUNT, accountId)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    /** The signed-in person's full name as the server returned it, or null when not known. */
    public synchronized String fullName() {
        if (token() == null) {
            return null;
        }
        return prefs.getString(KEY_NAME, null);
    }

    /** The signed-in person's email, or null when not known. */
    public synchronized String email() {
        if (token() == null) {
            return null;
        }
        return prefs.getString(KEY_EMAIL, null);
    }

    /** The token if one is saved and still valid, otherwise null. */
    public synchronized String token() {
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null) {
            return null;
        }
        if (System.currentTimeMillis() >= prefs.getLong(KEY_EXPIRES_AT, 0)) {
            clear();
            return null;
        }
        return token;
    }

    /** The saved session, or null when there is no valid token. */
    public synchronized Session session() {
        if (token() == null) {
            return null;
        }
        return new Session(
                prefs.getString(KEY_ACCOUNT, ""),
                prefs.getString(KEY_ROLE, Session.ROLE_STUDENT));
    }

    public synchronized void clear() {
        prefs.edit().clear().apply();
    }
}
