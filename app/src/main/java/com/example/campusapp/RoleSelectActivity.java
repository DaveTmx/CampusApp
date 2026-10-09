package com.example.campusapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.auth.LecturerLoginActivity;
import com.example.campusapp.auth.StudentLoginActivity;
import com.example.campusapp.student.StudentHomeActivity;

public class RoleSelectActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        findViewById(R.id.btnStudent).setOnClickListener(v ->
                startActivity(new Intent(this, StudentLoginActivity.class)));

        findViewById(R.id.btnLecturer).setOnClickListener(v ->
                startActivity(new Intent(this, LecturerLoginActivity.class)));

        findViewById(R.id.btnGuest).setOnClickListener(v ->
                startActivity(new Intent(this, StudentHomeActivity.class)));
    }
}
