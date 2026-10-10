package com.example.campusapp.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etOtp, etNewPassword;
    private LinearLayout layoutEmailStep, layoutOtpStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        layoutEmailStep = findViewById(R.id.layoutEmailStep);
        layoutOtpStep = findViewById(R.id.layoutOtpStep);
        Button btnSendOtp = findViewById(R.id.btnSendOtp);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);

        btnSendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Proceed to OTP step
            layoutEmailStep.setVisibility(View.GONE);
            layoutOtpStep.setVisibility(View.VISIBLE);
            Toast.makeText(this, "OTP sent to " + email, Toast.LENGTH_LONG).show();
        });

        btnResetPassword.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();

            if (otp.isEmpty() || otp.length() < 6) {
                etOtp.setError("Enter valid 6-digit OTP");
                etOtp.requestFocus();
                return;
            }
            if (newPassword.isEmpty() || newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                etNewPassword.requestFocus();
                return;
            }

            Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
