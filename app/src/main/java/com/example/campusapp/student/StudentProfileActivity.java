package com.example.campusapp.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.RoleSelectActivity;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Student;

import java.util.HashMap;
import java.util.Map;

public class StudentProfileActivity extends AppCompatActivity {

    private AppData appData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        appData = AppData.getInstance(this);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.MAP, CampusMapActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.tvEditProfile).setOnClickListener(v -> promptForField(
                "Edit name", "Full name", currentStudent() != null ? currentStudent().name : "",
                "name", "Name"));

        findViewById(R.id.tvGroupChange).setOnClickListener(v -> promptForField(
                "Request group change", "New group (e.g. G03)", currentStudent() != null ? currentStudent().group : "",
                "group", "Group"));

        findViewById(R.id.tvNumberCorrection).setOnClickListener(v -> promptForField(
                "Request number correction", "Correct student number", currentStudent() != null ? currentStudent().number : "",
                "number", "Student Number"));

        findViewById(R.id.tvReportIssue).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIssueActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindStudent();
    }

    private Student currentStudent() {
        return appData.findStudentById(AppData.CURRENT_STUDENT_ID);
    }

    private void bindStudent() {
        Student student = currentStudent();
        if (student == null) return; // shouldn't happen, but don't crash the demo if it does

        ((TextView) findViewById(R.id.tvName)).setText(student.name);
        ((TextView) findViewById(R.id.tvNumber)).setText("Student No: " + student.number);
        ((TextView) findViewById(R.id.tvGroup)).setText("Group " + student.group);
        ((TextView) findViewById(R.id.tvProgramme)).setText("BSc in " + student.program);
    }

    /**
     * Shows a simple text-input dialog and, on confirm, submits the change as a
     * PendingChange for the lecturer to approve — the student can't edit their
     * own record directly.
     */
    private void promptForField(String title, String hint, String currentValue, String field, String fieldLabel) {
        Student student = currentStudent();
        if (student == null) return;

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(currentValue);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit request", (dialog, which) -> {
                    String newValue = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newValue)) {
                        Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    appData.addPendingChange(student.id, student.name, field, fieldLabel, newValue);
                    Toast.makeText(this, "Request submitted for lecturer approval", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showLogoutDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.show();
    }
}
