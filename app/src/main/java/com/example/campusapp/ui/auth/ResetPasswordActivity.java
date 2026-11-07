package com.example.campusapp.ui.auth;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.R;
import com.example.campusapp.data.repository.AuthRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.util.ServiceLocator;

/**
 * Forgot password. The student gives their email address (or student number) and the server emails
 * a link. The link opens a page in the browser where the new password is chosen, so the phone never
 * handles a reset code. The reply is the same whether or not the account exists.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private LinearLayout layoutOtpStep;
    private Button btnSendOtp;

    private AuthRepository auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = ServiceLocator.remoteAuthRepository(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etEmail = findViewById(R.id.etEmail);
        layoutOtpStep = findViewById(R.id.layoutOtpStep);
        btnSendOtp = findViewById(R.id.btnSendOtp);

        // The code step from the old on-phone flow is no longer used.
        layoutOtpStep.setVisibility(View.GONE);
        btnSendOtp.setText("Send reset link");
        btnSendOtp.setOnClickListener(v -> sendLink());
    }

    /** Timeouts and no connection get their own message, so the student knows to try again. */
    private String messageFor(Result.Status status) {
        switch (status) {
            case OFFLINE:
                return getString(R.string.msg_reset_connection);
            case RATE_LIMITED:
                return getString(R.string.msg_rate_limited);
            default:
                return getString(R.string.msg_generic_error);
        }
    }

    private void sendLink() {
        String who = etEmail.getText().toString().trim();
        if (who.isEmpty()) {
            etEmail.setError("Enter your email address");
            return;
        }
        if (!who.contains("@")) {
            // Students can sign in with a student number, but a reset link needs the email address.
            etEmail.setError("Enter the email address your account was registered with");
            return;
        }

        btnSendOtp.setEnabled(false);
        auth.requestPasswordReset(who, result -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            btnSendOtp.setEnabled(true);
            if (result.status == Result.Status.SUCCESS) {
                new AlertDialog.Builder(this)
                        .setTitle("Check your email")
                        .setMessage(getString(R.string.msg_reset_sent))
                        .setCancelable(false)
                        .setPositiveButton("OK", (d, which) -> finish())
                        .show();
            } else {
                etEmail.setError(messageFor(result.status));
            }
        }));
    }
}
