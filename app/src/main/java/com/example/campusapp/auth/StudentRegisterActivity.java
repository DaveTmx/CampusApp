package com.example.campusapp.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.data.Programs;
import com.example.campusapp.data.repository.LocalStudentRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.RegisterViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Student registration. The student fills in the form and taps Create account. The server creates
 * the account and emails a link. The student opens that link, then signs in.
 */
public class StudentRegisterActivity extends AppCompatActivity {

    private static final String PLACEHOLDER = "Select your program";

    private RegisterViewModel viewModel;
    private EditText etFullName;
    private EditText etStudentNumber;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Spinner spProgram;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(RegisterViewModel.class);

        etFullName = findViewById(R.id.etFullName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spProgram = findViewById(R.id.spProgram);
        btnCreate = findViewById(R.id.btnCreateAccount);
        Button btnDraft = findViewById(R.id.btnSaveDraft);

        // Drop-down: a "Select your program" prompt first, then every program.
        List<String> options = new ArrayList<>();
        options.add(PLACEHOLDER);
        options.addAll(Arrays.asList(Programs.ALL));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);

        // A fresh start picks up a draft saved earlier. After a rotation the fields restore themselves.
        if (savedInstanceState == null) {
            restoreDraft(adapter);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });

        btnDraft.setOnClickListener(v -> {
            pushFieldsToViewModel();
            viewModel.saveDraft();
        });

        btnCreate.setOnClickListener(v -> onCreateAccount());

        // Field errors: each one is a string resource ID, or null when the field is fine.
        viewModel.getNameError().observe(this, id -> etFullName.setError(text(id)));
        viewModel.getNumberError().observe(this, id -> etStudentNumber.setError(text(id)));
        viewModel.getEmailError().observe(this, id -> etEmail.setError(text(id)));
        viewModel.getPasswordError().observe(this, id -> etPassword.setError(text(id)));

        viewModel.getLoading().observe(this, loading -> {
            boolean idle = !Boolean.TRUE.equals(loading);
            btnCreate.setEnabled(idle);
            btnDraft.setEnabled(idle);
            btnCreate.setText(idle ? "Create account" : "Creating account…");
        });

        viewModel.getMessage().observe(this, event -> {
            Integer id = event == null ? null : event.getIfNotHandled();
            if (id != null) {
                Toast.makeText(this, id, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getOutcome().observe(this, event -> {
            Result.Status status = event == null ? null : event.getIfNotHandled();
            if (status == Result.Status.CHECK_EMAIL) {
                // The account exists on the server but cannot be used until the link is opened.
                new AlertDialog.Builder(this)
                        .setTitle("Check your email")
                        .setMessage("We sent a link to " + viewModel.currentEmail()
                                + ". Open it to activate your account, then sign in.")
                        .setCancelable(false)
                        .setPositiveButton("OK", (d, which) -> {
                            startActivity(new Intent(this, StudentLoginActivity.class));
                            finish();
                        })
                        .show();
            }
        });
    }

    /** Checks the form, then asks the server to create the account. */
    private void onCreateAccount() {
        String password = etPassword.getText().toString();
        if (spProgram.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select your program", Toast.LENGTH_SHORT).show();
            return;
        }
        pushFieldsToViewModel();
        if (!viewModel.checkDetails(password)) {
            return;
        }
        if (!password.equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords don't match");
            return;
        }
        viewModel.submit(password);
    }

    /** The ViewModel keeps what was typed, so it survives the app being killed in the background. */
    private void pushFieldsToViewModel() {
        viewModel.setName(etFullName.getText().toString());
        viewModel.setNumber(etStudentNumber.getText().toString());
        viewModel.setEmail(etEmail.getText().toString());
        int position = spProgram.getSelectedItemPosition();
        viewModel.setProgramme(position <= 0 ? ""
                : Programs.toCode((String) spProgram.getSelectedItem()));
    }

    private void restoreDraft(ArrayAdapter<String> adapter) {
        LocalStudentRepository repository = ServiceLocator.studentRepository(this);
        Student draft = repository.getRegistrationDraft();
        if (draft == null) {
            return;
        }
        etFullName.setText(draft.name);
        etStudentNumber.setText(draft.studentNumber);
        etEmail.setText(draft.email);
        int position = adapter.getPosition(Programs.toLabel(draft.programme));
        if (position > 0) {
            spProgram.setSelection(position);
        }
    }

    private String text(Integer resId) {
        return resId == null ? null : getString(resId);
    }
}
