package com.example.campusapp.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a six-digit one-time code for an email address, sends it, and later checks what the
 * user typed. A code lasts ten minutes and allows five wrong guesses.
 *
 * The code is emailed through EmailJS when OtpConfig is filled in. Until then the app is in
 * demo mode: nothing is emailed and the code is handed back so the screen can show it.
 */
public class OtpService {

    public enum Check { OK, WRONG, EXPIRED, TOO_MANY_TRIES, NONE }

    public interface SendCallback {
        /**
         * Called on the main thread.
         * emailed: the email was sent. demoCode: the code to show on screen (demo mode only).
         * error: why sending failed, or null.
         */
        void onDone(boolean emailed, String demoCode, String error);
    }

    private static final long LIFETIME_MS = 10 * 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final String ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send";

    private static class Pending {
        final String code;
        final long expiresAt;
        int attempts;

        Pending(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Pending> pending = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Handler main = new Handler(Looper.getMainLooper());

    public void send(String email, SendCallback callback) {
        String key = normalise(email);
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        synchronized (this) {
            pending.put(key, new Pending(code, System.currentTimeMillis() + LIFETIME_MS));
        }

        if (!OtpConfig.isConfigured()) {
            callback.onDone(false, code, null);
            return;
        }
        new Thread(() -> {
            String error = postToEmailJs(key, code);
            main.post(() -> {
                if (error != null) {
                    synchronized (this) {
                        pending.remove(key);
                    }
                    callback.onDone(false, null, error);
                } else {
                    callback.onDone(true, null, null);
                }
            });
        }).start();
    }

    public synchronized Check verify(String email, String typed) {
        String key = normalise(email);
        Pending p = pending.get(key);
        if (p == null) {
            return Check.NONE;
        }
        if (System.currentTimeMillis() > p.expiresAt) {
            pending.remove(key);
            return Check.EXPIRED;
        }
        if (p.attempts >= MAX_ATTEMPTS) {
            pending.remove(key);
            return Check.TOO_MANY_TRIES;
        }
        p.attempts++;
        if (p.code.equals(typed == null ? "" : typed.trim())) {
            pending.remove(key); // a code works once
            return Check.OK;
        }
        if (p.attempts >= MAX_ATTEMPTS) {
            pending.remove(key);
            return Check.TOO_MANY_TRIES;
        }
        return Check.WRONG;
    }

    private static String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Runs off the main thread. Returns null when the email was accepted, otherwise a reason. */
    private static String postToEmailJs(String email, String code) {
        HttpURLConnection connection = null;
        try {
            JSONObject params = new JSONObject();
            params.put("to_email", email);
            params.put("otp_code", code);
            params.put("app_name", "Campus Connect");

            JSONObject body = new JSONObject();
            body.put("service_id", OtpConfig.EMAILJS_SERVICE_ID);
            body.put("template_id", OtpConfig.EMAILJS_TEMPLATE_ID);
            body.put("user_id", OtpConfig.EMAILJS_PUBLIC_KEY);
            body.put("template_params", params);

            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            return status == 200 ? null : "The email service answered " + status;
        } catch (IOException e) {
            return "No internet connection";
        } catch (JSONException e) {
            return "Could not build the email";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
