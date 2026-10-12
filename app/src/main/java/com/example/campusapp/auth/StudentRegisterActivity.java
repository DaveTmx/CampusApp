package com.example.campusapp.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.data.Programs;
import com.example.campusapp.data.repository.LocalStudentRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.OtpService;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.RegisterViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Student registration. The student fills in the form and taps Create account; a six-digit
 * code is then emailed to the address they gave, and the account is only created once that
 * code has been entered.
 */
public class StudentRegisterActivity extends AppCompatActivity {

    private static final String PLACEHOLDER = "Select your program";

    private RegisterViewModel viewModel;
    private OtpService otp;
    private EditText etFullName;
    private EditText etStudentNumber;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Spinner spProgram;
    private Button btnCreate;

    /** The open "enter your code" dialog, or null. */
    private AlertDialog otpDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(RegisterViewModel.class);
        otp = ServiceLocator.otpService();

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
                Toast.makeText(this, "Account created. You can now log in.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, StudentLoginActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (otpDialog != null) {
            otpDialog.dismiss();
        }
        super.onDestroy();
    }

    /** Step 1: check the form, then email the code. */
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

        // Catch the two "already registered" cases before an email is sent for nothing.
        String email = viewModel.currentEmail();
        if (ServiceLocator.authRepository(this).findAccount(email) != null) {
            viewModel.showEmailError(R.string.error_email_taken);
            return;
        }
        LocalStudentRepository students = ServiceLocator.studentRepository(this);
        if (students.numberExists(etStudentNumber.getText().toString().trim())) {
            viewModel.showNumberError(R.string.msg_duplicate_number);
            return;
        }

        sendCode(email);
    }

    private void sendCode(String email) {
        btnCreate.setEnabled(false);
        btnCreate.setText("Sending code…");
        otp.send(email, (emailed, demoCode, error) -> {
            if (isFinishing() || isDestroyed()) return;
            btnCreate.setEnabled(true);
            btnCreate.setText("Create account");
            if (error != null) {
                Toast.makeText(this, "Could not send the code: " + error, Toast.LENGTH_LONG).show();
                return;
            }
            showOtpDialog(email, emailed, demoCode);
        });
    }

    /** Step 2: the student types the code from their email. */
    private void showOtpDialog(String email, boolean emailed, String demoCode) {
        if (otpDialog != null) {
            otpDialog.dismiss();
        }
        View body = getLayoutInflater().inflate(R.layout.dialog_otp, null);
        TextView tvMessage = body.findViewById(R.id.tvOtpMessage);
        EditText etCode = body.findViewById(R.id.etOtpCode);
        TextView tvResend = body.findViewById(R.id.tvResendOtp);

        String message = "For your security we sent a 6-digit code to " + mask(email)
                + ". Enter it below to finish creating your account. The code expires in 10 minutes.";
        if (!emailed) {
            // EmailJS is not set up in OtpConfig yet, so nothing was emailed.
            message = "Email sending is not set up yet (see OtpConfig.java), so here is your code: "
                    + demoCode + "\n\nIt would normally be emailed to " + mask(email) + ".";
        }
        tvMessage.setText(message);

        otpDialog = new AlertDialog.Builder(this)
                .setTitle("Verify your email")
                .setView(body)
                .setCancelable(false)
                .setPositiveButton("Verify", null) // set below so a wrong code keeps the dialog open
                .setNegativeButton("Cancel", null)
                .create();

        otpDialog.setOnShowListener(d -> otpDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> verifyCode(email, etCode)));

        tvResend.setOnClickListener(v -> {
            otpDialog.dismiss();
            sendCode(email);
        });

        otpDialog.show();
    }

    private void verifyCode(String email, EditText etCode) {
        String code = etCode.getText().toString().trim();
        if (code.length() != 6) {
            etCode.setError("Enter the 6-digit code");
            return;
        }
        switch (otp.verify(email, code)) {
            case OK:
                otpDialog.dismiss();
                otpDialog = null;
                // Step 3: the email is confirmed, create the account.
                viewModel.submit(etPassword.getText().toString());
                break;
            case WRONG:
                etCode.setError("That code is not correct");
                break;
            default: // expired, too many tries, or no code
                otpDialog.dismiss();
                otpDialog = null;
                Toast.makeText(this, "That code can no longer be used. Tap Create account to get a new one.",
                        Toast.LENGTH_LONG).show();
                break;
        }
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

    /** chileshe@example.com -> ch******@example.com */
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at < 3) return email;
        StringBuilder stars = new StringBuilder();
        for (int i = 2; i < at; i++) stars.append('*');
        return email.substring(0, 2) + stars + email.substring(at);
    }

    private String text(Integer resId) {
        return resId == null ? null : getString(resId);
    }
}
