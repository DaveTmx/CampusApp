package com.mulungushi.campuscompanion.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.lecturer.LecturerHomeActivity;

public class LecturerLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_login);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnLogIn).setOnClickListener(v -> {
            startActivity(new Intent(this, LecturerHomeActivity.class));
            finish();
        });
    }
}
