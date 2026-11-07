package com.example.campusapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.lecturer.LecturerHomeActivity;
import com.example.campusapp.ui.auth.ResetPasswordActivity;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.util.StaffCodeDialog;
import com.example.campusapp.viewmodel.AuthViewModel;

/**
 * Staff sign-in. Step 1 is the password, checked by the server. Step 2 is the code the server
 * emails to the lecturer. No session starts until the server accepts that code.
 */
public class LecturerLoginActivity extends AppCompatActivity {

    /** The open code dialog, or null. */
    private StaffCodeDialog codeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_login);

        AuthViewModel viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(AuthViewModel.class);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogIn = findViewById(R.id.btnLogIn);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvForgot).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        btnLogIn.setOnClickListener(v -> viewModel.startStaffSignIn(
                etEmail.getText().toString(), etPassword.getText().toString()));

        viewModel.getLoading().observe(this, loading ->
                btnLogIn.setEnabled(!Boolean.TRUE.equals(loading)));

        viewModel.getError().observe(this, resId -> {
            etEmail.setError(null);
            etPassword.setError(null);
            if (resId == null) {
                return;
            }
            if (resId == R.string.error_username_required) {
                etEmail.setError("Enter your email address");
            } else {
                etPassword.setError(getString(resId));
            }
        });

        // The password was right and the server has emailed a code.
        viewModel.getCodeRequest().observe(this, event -> {
            String challengeId = event == null ? null : event.getIfNotHandled();
            if (challengeId == null) {
                return;
            }
            String email = etEmail.getText().toString().trim();
            codeDialog = StaffCodeDialog.show(this, email, new StaffCodeDialog.Listener() {
                @Override
                public void onSubmit(String code) {
                    viewModel.verifyStaffCode(challengeId, code);
                }

                @Override
                public void onCancel() {
                    codeDialog = null;
                }
            });
        });

        // A wrong code keeps the dialog open. Any other failure closes it and shows on the password field.
        viewModel.getCodeError().observe(this, event -> {
            Integer resId = event == null ? null : event.getIfNotHandled();
            if (resId == null) {
                return;
            }
            if (codeDialog != null && resId == R.string.msg_wrong_code) {
                codeDialog.showError(getString(resId));
                return;
            }
            if (codeDialog != null) {
                codeDialog.close();
                codeDialog = null;
            }
            etPassword.setError(getString(resId));
        });

        viewModel.getSession().observe(this, session -> {
            if (session == null) {
                return;
            }
            if (!session.isLecturer()) {
                // A student account cannot use the staff sign-in.
                viewModel.signOut();
                etPassword.setError(getString(R.string.msg_not_allowed));
                return;
            }
            if (codeDialog != null) {
                codeDialog.close();
                codeDialog = null;
            }
            Intent intent = new Intent(this, LecturerHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
