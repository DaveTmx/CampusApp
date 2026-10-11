package com.example.campusapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.lecturer.LecturerHomeActivity;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.AuthViewModel;

public class LecturerLoginActivity extends AppCompatActivity {

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

        btnLogIn.setOnClickListener(v -> viewModel.signIn(
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
                etEmail.setError(getString(resId));
            } else {
                etPassword.setError(getString(resId));
            }
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
            Intent intent = new Intent(this, LecturerHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
