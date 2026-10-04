package com.mulungushi.campuscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.auth.LecturerLoginActivity;
import com.mulungushi.campuscompanion.auth.StudentLoginActivity;
import com.mulungushi.campuscompanion.student.StudentHomeActivity;

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
