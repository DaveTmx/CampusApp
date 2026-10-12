package com.example.campusapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.student.StudentHomeActivity;
import com.example.campusapp.ui.auth.ResetPasswordActivity;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.AuthViewModel;

public class StudentLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        AuthViewModel viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(AuthViewModel.class);

        EditText etNumber = findViewById(R.id.etStudentNumber);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogIn = findViewById(R.id.btnLogIn);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvForgot).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        btnLogIn.setOnClickListener(v -> viewModel.signIn(
                etNumber.getText().toString(), etPassword.getText().toString()));

        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, StudentRegisterActivity.class)));

        viewModel.getLoading().observe(this, loading ->
                btnLogIn.setEnabled(!Boolean.TRUE.equals(loading)));

        // Shown on the field itself, so it is still correct after a rotation.
        viewModel.getError().observe(this, resId -> {
            etNumber.setError(null);
            etPassword.setError(null);
            if (resId == null) {
                return;
            }
            if (resId == R.string.error_username_required) {
                etNumber.setError(getString(resId));
            } else {
                etPassword.setError(getString(resId));
            }
        });

        viewModel.getSession().observe(this, session -> {
            if (session == null) {
                return;
            }
            if (session.isLecturer()) {
                // A lecturer account cannot use the student sign-in.
                viewModel.signOut();
                etPassword.setError(getString(R.string.msg_not_allowed));
                return;
            }
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
