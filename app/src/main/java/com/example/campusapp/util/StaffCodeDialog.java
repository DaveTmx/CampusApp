package com.example.campusapp.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.campusapp.R;

/**
 * Asks a lecturer for the code the server emailed. The server creates and checks the code; this
 * dialog only collects it. A wrong code keeps the dialog open with the error on the field.
 */
public final class StaffCodeDialog {

    public interface Listener {
        /** The user entered six characters and pressed Verify. */
        void onSubmit(String code);

        /** The user pressed Cancel. */
        void onCancel();
    }

    private final AlertDialog dialog;
    private final EditText etCode;

    private StaffCodeDialog(AlertDialog dialog, EditText etCode) {
        this.dialog = dialog;
        this.etCode = etCode;
    }

    public static StaffCodeDialog show(Activity activity, String email, Listener listener) {
        View body = activity.getLayoutInflater().inflate(R.layout.dialog_otp, null);
        TextView tvMessage = body.findViewById(R.id.tvOtpMessage);
        EditText etCode = body.findViewById(R.id.etOtpCode);
        TextView tvResend = body.findViewById(R.id.tvResendOtp);

        tvMessage.setText(activity.getString(R.string.staff_code_message, mask(email)));
        // A new code needs the password again, so the user goes back to the sign-in form instead.
        tvResend.setVisibility(View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Enter your sign-in code")
                .setView(body)
                .setCancelable(false)
                .setPositiveButton("Verify", null) // set below so a wrong code keeps the dialog open
                .setNegativeButton("Cancel", (d, which) -> listener.onCancel())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String code = etCode.getText().toString().trim();
                    if (code.length() != 6) {
                        etCode.setError("Enter the 6-digit code");
                        return;
                    }
                    listener.onSubmit(code);
                }));
        dialog.show();
        return new StaffCodeDialog(dialog, etCode);
    }

    /** Shows an error under the code field. The dialog stays open. */
    public void showError(String message) {
        etCode.setError(message);
    }

    public void close() {
        dialog.dismiss();
    }

    /** chileshe@example.com -> ch******@example.com */
    private static String mask(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at < 3) {
            return email;
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 2; i < at; i++) {
            stars.append('*');
        }
        return email.substring(0, 2) + stars + email.substring(at);
    }
}
