package com.example.campusapp.util;

/**
 * Settings for emailing one-time codes through EmailJS (emailjs.com), which sends email straight
 * from the app with no server of your own. Fill in the three values from your EmailJS account.
 * While any of them is empty the app runs in demo mode and shows the code on screen instead.
 *
 * In your EmailJS email template use these variables: {{to_email}}, {{otp_code}}, {{app_name}}.
 * In EmailJS, Account > Security, allow API calls from non-browser apps.
 */
public final class OtpConfig {
    public static final String EMAILJS_SERVICE_ID = "";
    public static final String EMAILJS_TEMPLATE_ID = "";
    public static final String EMAILJS_PUBLIC_KEY = "";

    private OtpConfig() {
    }

    public static boolean isConfigured() {
        return !EMAILJS_SERVICE_ID.isEmpty() && !EMAILJS_TEMPLATE_ID.isEmpty()
                && !EMAILJS_PUBLIC_KEY.isEmpty();
    }
}
