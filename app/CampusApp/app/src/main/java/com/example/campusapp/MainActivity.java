package com.example.campusapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.campusapp.sync.SyncWorker;
import com.example.campusapp.ui.auth.ResetPasswordActivity;
import com.example.campusapp.ui.student.NoticesActivity;
import com.example.campusapp.ui.student.StudentScheduleActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister, btnNotices, btnSchedule, btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();
        scheduleSyncWorker();
    }

    private void initViews() {
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnNotices = findViewById(R.id.btnNotices);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> 
            Toast.makeText(this, "Sign In selected", Toast.LENGTH_SHORT).show()
        );

        btnRegister.setOnClickListener(v -> 
            Toast.makeText(this, "Register Student selected", Toast.LENGTH_SHORT).show()
        );

        btnNotices.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoticesActivity.class);
            startActivity(intent);
        });

        btnSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StudentScheduleActivity.class);
            startActivity(intent);
        });

        btnResetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void scheduleSyncWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
