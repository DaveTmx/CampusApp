package com.mulungushi.campuscompanion.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.data.AppData;
import com.mulungushi.campuscompanion.data.Programs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudentRegisterActivity extends AppCompatActivity {

    private static final String PLACEHOLDER = "Select your program";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        AppData appData = AppData.getInstance(this);

        EditText etFullName = findViewById(R.id.etFullName);
        EditText etStudentNumber = findViewById(R.id.etStudentNumber);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Spinner spProgram = findViewById(R.id.spProgram);

        // Drop-down: a "Select your program" prompt first, then the three programs.
        List<String> options = new ArrayList<>();
        options.add(PLACEHOLDER);
        options.addAll(Arrays.asList(Programs.ALL));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });

        findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String number = etStudentNumber.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number)
                    || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spProgram.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select your program", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (appData.findStudentByNumber(number) != null) {
                Toast.makeText(this, "That student number is already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            String program = (String) spProgram.getSelectedItem();
            // New students start "Unassigned" until a lecturer places them in a group.
            appData.addStudent(number, name, "Unassigned", program);

            Toast.makeText(this, "Account created — please log in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });
    }
}
