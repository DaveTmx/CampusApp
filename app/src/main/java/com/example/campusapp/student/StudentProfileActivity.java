package com.example.campusapp.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.RoleSelectActivity;
import com.example.campusapp.data.Programs;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.util.Validation;
import com.example.campusapp.viewmodel.AuthViewModel;
import com.example.campusapp.viewmodel.ProfileViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * The student's own profile. A student may change their own name and programme directly;
 * a group change or a student-number correction is a request the lecturer approves.
 */
public class StudentProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        ViewModelProvider provider = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this));
        viewModel = provider.get(ProfileViewModel.class);
        authViewModel = provider.get(AuthViewModel.class);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.MAP, CampusMapActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.tvEditProfile).setOnClickListener(v -> promptForDetails());
        findViewById(R.id.tvGroupChange).setOnClickListener(v -> promptForGroup());
        findViewById(R.id.tvNumberCorrection).setOnClickListener(v -> promptForNumber());
        findViewById(R.id.tvReportIssue).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIssueActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        viewModel.getProfile().observe(this, this::bindStudent);
        viewModel.getMessage().observe(this, event -> {
            Integer id = event == null ? null : event.getIfNotHandled();
            if (id != null) {
                Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The lecturer may have approved a request since this screen was last shown.
        viewModel.syncNow();
    }

    private void bindStudent(Student student) {
        if (student == null) return;

        String group = student.confirmedGroup == null ? "Unassigned" : "Group " + student.confirmedGroup;
        if (student.pendingGroup != null) {
            group += " (requested: " + student.pendingGroup + ")";
        }
        ((TextView) findViewById(R.id.tvName)).setText(student.name);
        ((TextView) findViewById(R.id.tvNumber)).setText("Student No: " + student.studentNumber);
        ((TextView) findViewById(R.id.tvGroup)).setText(group);
        ((TextView) findViewById(R.id.tvProgramme)).setText("BSc in " + Programs.toLabel(student.programme));
    }

    /** Name and programme: the two things a student may change without asking the lecturer. */
    private void promptForDetails() {
        Student student = viewModel.getProfile().getValue();
        if (student == null) return;

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad, pad, 0);

        EditText etName = new EditText(this);
        etName.setHint("Full name");
        etName.setText(student.name);
        form.addView(etName);

        Spinner spProgram = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Programs.ALL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);
        int position = adapter.getPosition(Programs.toLabel(student.programme));
        if (position >= 0) spProgram.setSelection(position);
        form.addView(spProgram);

        new AlertDialog.Builder(this)
                .setTitle("Edit name / programme")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    Integer problem = Validation.nameError(name);
                    if (problem != null) {
                        Toast.makeText(this, problem, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.updateDetails(name, Programs.CODES[spProgram.getSelectedItemPosition()]);
                })
                .show();
    }

    private void promptForGroup() {
        EditText input = newInput("New group (G01 to G04)", "");
        new AlertDialog.Builder(this)
                .setTitle("Request group change")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit request", (dialog, which) -> {
                    String group = input.getText().toString().trim().toUpperCase();
                    if (group.isEmpty() || Validation.groupError(group) != null) {
                        Toast.makeText(this, R.string.error_group_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.requestGroupChange(group);
                })
                .show();
    }

    private void promptForNumber() {
        EditText input = newInput("Correct student number", "");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("Request number correction")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit request", (dialog, which) -> {
                    String number = input.getText().toString();
                    Integer problem = Validation.numberError(number);
                    if (problem != null) {
                        Toast.makeText(this, problem, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.requestNumberCorrection(number);
                })
                .show();
    }

    private EditText newInput(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(value);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        return input;
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
            authViewModel.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.show();
    }
}
