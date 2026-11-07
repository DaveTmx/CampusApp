package com.example.campusapp.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusapp.BuildConfig;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.local.AppDatabase;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.AppExecutors;
import com.example.campusapp.util.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Sign-in for the app while there is no server. Accounts and the saved session live in
 * SharedPreferences on this phone.
 *
 * Passwords are stored with PBKDF2-HMAC-SHA256 and a random salt. Accounts from older installs
 * (single-pass SHA-256) are upgraded the next time their owner signs in.
 *
 * After five wrong passwords an account is locked for 15 minutes.
 *
 * Lecturer accounts never get a session from the password alone. The sign-in screen must also
 * confirm a one-time code emailed to the lecturer (see completeStaffSignIn).
 *
 * Every call answers on the thread that made it, so a ViewModel can use postValue() either way.
 * Hashing takes a moment, so sign-in should not run on a busy screen.
 */
public class LocalAuthRepository implements AuthRepository {

    /** Demo accounts, created in debug builds only. Never present in a release build. */
    public static final String DEMO_STUDENT_NUMBER = "202419080";
    public static final String DEMO_STUDENT_PASSWORD = "student123";
    public static final String DEMO_STUDENT_EMAIL = "chileshe.banda@example.com";
    public static final String DEMO_LECTURER_EMAIL = "muleba@gmail.com";
    public static final String DEMO_LECTURER_PASSWORD = "lecturer123";
    public static final String DEMO_LECTURER_ACCOUNT_ID = "lecturer-1";

    private static final String PREFS = "campus_accounts";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_SESSION_ACCOUNT = "session_account";
    private static final String KEY_SESSION_ROLE = "session_role";

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 15 * 60 * 1000L;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_BITS = 256;
    private static final String KDF_PBKDF2 = "pbkdf2";

    private final SharedPreferences prefs;
    private final Context appContext;
    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>(false);

    public LocalAuthRepository(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (BuildConfig.DEBUG) {
            if (!prefs.contains(KEY_ACCOUNTS)) {
                createAccount(DEMO_STUDENT_NUMBER, DEMO_STUDENT_PASSWORD,
                        AppData.CURRENT_STUDENT_ID, Session.ROLE_STUDENT, DEMO_STUDENT_EMAIL);
                createAccount(DEMO_LECTURER_EMAIL, DEMO_LECTURER_PASSWORD,
                        DEMO_LECTURER_ACCOUNT_ID, Session.ROLE_LECTURER, DEMO_LECTURER_EMAIL);
            } else {
                // Debug installs from before emails were stored: give the demo student one.
                AccountInfo demo = findAccount(DEMO_STUDENT_NUMBER);
                if (demo != null && demo.email.isEmpty()) {
                    setEmail(DEMO_STUDENT_NUMBER, DEMO_STUDENT_EMAIL);
                }
            }
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
        if (account == null || password == null) {
            callback.onResult(Result.of(Result.Status.BAD_CREDENTIALS, null));
            return;
        }
        long now = System.currentTimeMillis();
        if (account.optLong("lockedUntil", 0) > now) {
            callback.onResult(Result.of(Result.Status.LOCKED_OUT, null));
            return;
        }

        if (!checkPassword(account, password)) {
            int failures = account.optInt("failures", 0) + 1;
            try {
                if (failures >= MAX_FAILED_ATTEMPTS) {
                    account.put("failures", 0);
                    account.put("lockedUntil", now + LOCKOUT_MS);
                } else {
                    account.put("failures", failures);
                }
            } catch (JSONException ignored) {
                // Cannot happen for these keys; the account is still saved below.
            }
            saveAccount(account);
            boolean lockedNow = account.optLong("lockedUntil", 0) > now;
            callback.onResult(Result.of(lockedNow ? Result.Status.LOCKED_OUT
                    : Result.Status.BAD_CREDENTIALS, null));
            return;
        }

        try {
            account.put("failures", 0);
            account.put("lockedUntil", 0);
            if (!KDF_PBKDF2.equals(account.optString("kdf"))) {
                storeHash(account, password); // upgrade the old hash now that the password is known
            }
        } catch (JSONException ignored) {
            // Keep the old hash if the upgrade cannot be written.
        }
        saveAccount(account);

        Session session = new Session(account.optString("accountId"), account.optString("role"));
        if (session.isLecturer()) {
            // Staff must also confirm the emailed code. No session is started yet.
            callback.onResult(Result.of(Result.Status.NEEDS_CODE, session));
            return;
        }
        startSession(session);
        callback.onResult(Result.of(Result.Status.SUCCESS, session));
    }

    /**
     * Staff only. Call this after the emailed code has been confirmed. Starts the session.
     * Refuses anything that is not a lecturer account.
     */
    @Override
    public synchronized void completeStaffSignIn(String username, RepoCallback<Session> callback) {
        JSONObject account = find(normalise(username));
        if (account == null || !Session.ROLE_LECTURER.equals(account.optString("role"))) {
            callback.onResult(Result.of(Result.Status.NOT_ALLOWED, null));
            return;
        }
        Session session = new Session(account.optString("accountId"), account.optString("role"));
        startSession(session);
        callback.onResult(Result.of(Result.Status.SUCCESS, session));
    }

    @Override
    public synchronized void signOut(Runnable done) {
        prefs.edit().remove(KEY_SESSION_ACCOUNT).remove(KEY_SESSION_ROLE).apply();
        SessionManager.signOut();
        clearCachedStudentData();
        if (done != null) {
            done.run();
        }
    }

    /**
     * Removes what this phone kept from the signed-out user's session: the server roster
     * cache in Room and any unsent registration draft. The account store is kept, so the
     * user can sign in again. Room must not run on the main thread, so this is queued.
     */
    private void clearCachedStudentData() {
        AppExecutors.get().diskIO().execute(() -> {
            AppDatabase.getInstance(appContext).clearAllTables();
            appContext.getSharedPreferences(LocalStudentRepository.DRAFT_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().apply();
        });
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
        return createAccount(username, password, accountId, role, "");
    }

    /** Same, and remembers the email address the account's one-time codes are sent to. */
    public synchronized boolean createAccount(String username, String password,
                                              String accountId, String role, String email) {
        String key = normalise(username);
        if (key.isEmpty() || password == null || find(key) != null) {
            return false;
        }
        try {
            JSONObject account = new JSONObject();
            account.put("username", key);
            account.put("accountId", accountId);
            account.put("role", role);
            account.put("email", normalise(email));
            account.put("failures", 0);
            account.put("lockedUntil", 0);
            storeHash(account, password);
            JSONArray accounts = loadAccounts();
            accounts.put(account);
            saveAccounts(accounts);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /** What forgot-password needs to know about an account. */
    public static final class AccountInfo {
        public final String username;
        public final String email;

        AccountInfo(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }

    /** Finds an account by email, username or student number. Null when there is none. */
    public synchronized AccountInfo findAccount(String identifier) {
        String key = normalise(identifier);
        if (key.isEmpty()) {
            return null;
        }
        JSONArray accounts = loadAccounts();
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.optJSONObject(i);
            if (account == null) {
                continue;
            }
            String username = account.optString("username");
            String email = account.optString("email");
            if (email.isEmpty() && username.contains("@")) {
                email = username; // lecturers sign in with their email
            }
            if (key.equals(username) || key.equals(email)) {
                return new AccountInfo(username, email);
            }
        }
        return null;
    }

    /** Stores the email address for an existing account. */
    public synchronized void setEmail(String username, String email) {
        JSONObject account = find(normalise(username));
        if (account == null) {
            return;
        }
        try {
            account.put("email", normalise(email));
        } catch (JSONException ignored) {
            return;
        }
        saveAccount(account);
    }

    /** Sets a new password. The caller must already have checked the emailed code. */
    public synchronized boolean resetPassword(String username, String newPassword) {
        JSONObject account = find(normalise(username));
        if (account == null || newPassword == null) {
            return false;
        }
        try {
            storeHash(account, newPassword);
            account.put("failures", 0);
            account.put("lockedUntil", 0);
        } catch (JSONException e) {
            return false;
        }
        saveAccount(account);
        return true;
    }

    // ----- Internals -----

    private void startSession(Session session) {
        prefs.edit()
                .putString(KEY_SESSION_ACCOUNT, session.accountId)
                .putString(KEY_SESSION_ROLE, session.role)
                .apply();
        SessionManager.signIn(session);
    }

    private JSONArray loadAccounts() {
        try {
            return new JSONArray(prefs.getString(KEY_ACCOUNTS, "[]"));
        } catch (JSONException e) {
            return new JSONArray(); // unreadable list: treat as no accounts
        }
    }

    private void saveAccounts(JSONArray accounts) {
        prefs.edit().putString(KEY_ACCOUNTS, accounts.toString()).apply();
    }

    private JSONObject find(String key) {
        if (key.isEmpty()) {
            return null;
        }
        JSONArray accounts = loadAccounts();
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject account = accounts.optJSONObject(i);
            if (account != null && key.equals(account.optString("username"))) {
                return account;
            }
        }
        return null;
    }

    /** Replaces the stored copy of this account with the given one. */
    private void saveAccount(JSONObject updated) {
        String key = updated.optString("username");
        JSONArray accounts = loadAccounts();
        try {
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject account = accounts.optJSONObject(i);
                if (account != null && key.equals(account.optString("username"))) {
                    accounts.put(i, updated);
                    saveAccounts(accounts);
                    return;
                }
            }
        } catch (JSONException ignored) {
            // Index is always in range here; nothing else to do.
        }
    }

    private static void storeHash(JSONObject account, String password) throws JSONException {
        String salt = newSalt();
        account.put("salt", salt);
        account.put("hash", pbkdf2(salt, password));
        account.put("kdf", KDF_PBKDF2);
    }

    private static boolean checkPassword(JSONObject account, String password) {
        String salt = account.optString("salt");
        String stored = account.optString("hash");
        String computed = KDF_PBKDF2.equals(account.optString("kdf"))
                ? pbkdf2(salt, password)
                : legacySha256(salt, password);
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                computed.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    private static String pbkdf2(String saltHex, String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(saltHex),
                    PBKDF2_ITERATIONS, PBKDF2_KEY_BITS);
            try {
                byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec).getEncoded();
                return toHex(key);
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is always available on Android", e);
        }
    }

    /** Old hash format, kept only so existing accounts can still sign in once. */
    private static String legacySha256(String salt, String password) {
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

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
