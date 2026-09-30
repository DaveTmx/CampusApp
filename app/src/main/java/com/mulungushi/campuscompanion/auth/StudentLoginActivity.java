package com.mulungushi.campuscompanion.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.student.StudentHomeActivity;

public class StudentLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLogIn).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });

        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, StudentRegisterActivity.class)));
    }
}
