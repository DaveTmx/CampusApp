package com.example.campusapp.ui.auth;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.R;
import com.example.campusapp.data.repository.LocalAuthRepository;
import com.example.campusapp.util.OtpService;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.util.Validation;

/**
 * Forgot password. Step 1: say who you are, and a six-digit code is emailed to the address on that
 * account. Step 2: enter the code and a new password.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etOtp;
    private EditText etNewPassword;
    private LinearLayout layoutEmailStep;
    private LinearLayout layoutOtpStep;
    private Button btnSendOtp;

    private LocalAuthRepository auth;
    private OtpService otp;

    /** The account the code was sent for. Null until step 1 succeeds. */
    private LocalAuthRepository.AccountInfo account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = ServiceLocator.authRepository(this);
        otp = ServiceLocator.otpService();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        layoutEmailStep = findViewById(R.id.layoutEmailStep);
        layoutOtpStep = findViewById(R.id.layoutOtpStep);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        Button btnReset = findViewById(R.id.btnResetPassword);

        btnSendOtp.setOnClickListener(v -> sendCode());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void sendCode() {
        String who = etEmail.getText().toString().trim();
        if (who.isEmpty()) {
            etEmail.setError("Enter your email, username or student number");
            return;
        }
        LocalAuthRepository.AccountInfo found = auth.findAccount(who);
        if (found == null) {
            etEmail.setError("No account found with that email or number");
            return;
        }
        if (found.email.isEmpty()) {
            etEmail.setError("This account has no email address on file");
            return;
        }

        btnSendOtp.setEnabled(false);
        otp.send(found.email, (emailed, demoCode, error) -> {
            if (isFinishing() || isDestroyed()) return;
            btnSendOtp.setEnabled(true);
            if (error != null) {
                Toast.makeText(this, "Could not send the code: " + error, Toast.LENGTH_LONG).show();
                return;
            }
            account = found;
            layoutEmailStep.setVisibility(View.GONE);
            layoutOtpStep.setVisibility(View.VISIBLE);
            if (emailed) {
                Toast.makeText(this, "Code sent to " + mask(found.email), Toast.LENGTH_LONG).show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Demo mode")
                        .setMessage("Email sending is not set up yet, so here is your code:\n\n"
                                + demoCode + "\n\nIt would normally arrive at " + mask(found.email) + ".")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void resetPassword() {
        if (account == null) {
            return;
        }
        String code = etOtp.getText().toString().trim();
        String password = etNewPassword.getText().toString();

        if (code.length() != 6) {
            etOtp.setError("Enter the 6-digit code");
            return;
        }
        Integer problem = Validation.passwordError(password);
        if (problem != null) {
            etNewPassword.setError(getString(problem));
            return;
        }

        OtpService.Check result = otp.verify(account.email, code);
        switch (result) {
            case OK:
                if (auth.resetPassword(account.username, password)) {
                    Toast.makeText(this, "Password changed. Please sign in.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Could not change the password.", Toast.LENGTH_LONG).show();
                }
                break;
            case WRONG:
                etOtp.setError("That code is not correct");
                break;
            default: // expired, too many tries, or no code: start again
                Toast.makeText(this, "That code can no longer be used. Request a new one.", Toast.LENGTH_LONG).show();
                account = null;
                etOtp.setText("");
                layoutOtpStep.setVisibility(View.GONE);
                layoutEmailStep.setVisibility(View.VISIBLE);
                break;
        }
    }

    /** chileshe@example.com -> ch******@example.com */
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at < 3) return email;
        StringBuilder stars = new StringBuilder();
        for (int i = 2; i < at; i++) stars.append('*');
        return email.substring(0, 2) + stars + email.substring(at);
    }
}
