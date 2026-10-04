package com.mulungushi.campuscompanion.lecturer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.data.AppData;
import com.mulungushi.campuscompanion.data.Programs;
import com.mulungushi.campuscompanion.data.Student;

/**
 * Handles both "add a new student" and "edit an existing student" — the
 * lecturer's editing privilege on the roster. Pass EXTRA_STUDENT_ID to edit
 * an existing record; leave it out to create a new one.
 */
public class EditStudentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "student_id";

    private AppData appData;
    private String editingId; // null when adding a new student

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student);

        appData = AppData.getInstance(this);
        editingId = getIntent().getStringExtra(EXTRA_STUDENT_ID);

        EditText etName = findViewById(R.id.etName);
        EditText etNumber = findViewById(R.id.etNumber);
        EditText etGroup = findViewById(R.id.etGroup);
        Spinner spProgram = findViewById(R.id.spProgram);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Programs.ALL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);

        if (editingId != null) {
            Student existing = appData.findStudentById(editingId);
            if (existing != null) {
                etName.setText(existing.name);
                etNumber.setText(existing.number);
                etGroup.setText(existing.group);
                int position = adapter.getPosition(existing.program);
                if (position >= 0) spProgram.setSelection(position);
            }
        } else {
            ((TextView) findViewById(R.id.tvHeader)).setText("Add Student");
            ((TextView) findViewById(R.id.tvSubheader)).setText("Create a new roster entry");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String number = etNumber.getText().toString().trim();
            String group = etGroup.getText().toString().trim();
            String program = (String) spProgram.getSelectedItem();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number) || TextUtils.isEmpty(group)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editingId != null) {
                appData.updateStudent(editingId, number, name, group, program);
                Toast.makeText(this, "Student record updated", Toast.LENGTH_SHORT).show();
            } else {
                appData.addStudent(number, name, group, program);
                Toast.makeText(this, "Student added to roster", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }
}
