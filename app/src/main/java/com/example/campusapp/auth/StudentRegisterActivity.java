package com.example.campusapp.auth;

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

public class StudentRegisterActivity extends AppCompatActivity {

    private static final String PLACEHOLDER = "Select your program";

    private RegisterViewModel viewModel;
    private EditText etFullName;
    private EditText etStudentNumber;
    private EditText etClaimCode;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Spinner spProgram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(RegisterViewModel.class);

        etFullName = findViewById(R.id.etFullName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etClaimCode = findViewById(R.id.etClaimCode);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spProgram = findViewById(R.id.spProgram);
        Button btnCreate = findViewById(R.id.btnCreateAccount);
        Button btnDraft = findViewById(R.id.btnSaveDraft);

        // Drop-down: a "Select your program" prompt first, then the three programs.
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

        btnCreate.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            if (spProgram.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select your program", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(etConfirmPassword.getText().toString())) {
                etConfirmPassword.setError("Passwords don't match");
                return;
            }
            pushFieldsToViewModel();
            viewModel.submit(password);
        });

        // Field errors: each one is a string resource ID, or null when the field is fine.
        viewModel.getNameError().observe(this, id -> etFullName.setError(text(id)));
        viewModel.getNumberError().observe(this, id -> etStudentNumber.setError(text(id)));
        viewModel.getClaimError().observe(this, id -> etClaimCode.setError(text(id)));
        viewModel.getPasswordError().observe(this, id -> etPassword.setError(text(id)));

        viewModel.getLoading().observe(this, loading -> {
            boolean idle = !Boolean.TRUE.equals(loading);
            btnCreate.setEnabled(idle);
            btnDraft.setEnabled(idle);
        });

        viewModel.getMessage().observe(this, event -> {
            Integer id = event == null ? null : event.getIfNotHandled();
            if (id != null) {
                Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getOutcome().observe(this, event -> {
            Result.Status status = event == null ? null : event.getIfNotHandled();
            if (status == Result.Status.SUCCESS || status == Result.Status.PENDING
                    || status == Result.Status.GROUP_FULL) {
                // The account exists now, so move on to sign-in.
                startActivity(new Intent(this, StudentLoginActivity.class));
                finish();
            }
        });
    }

    /** The ViewModel keeps what was typed, so it survives the app being killed in the background. */
    private void pushFieldsToViewModel() {
        viewModel.setName(etFullName.getText().toString());
        viewModel.setNumber(etStudentNumber.getText().toString());
        viewModel.setClaimCode(etClaimCode.getText().toString());
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
        int position = adapter.getPosition(Programs.toLabel(draft.programme));
        if (position > 0) {
            spProgram.setSelection(position);
        }
    }

    private String text(Integer resId) {
        return resId == null ? null : getString(resId);
    }
}
