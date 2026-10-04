package com.mulungushi.campuscompanion.lecturer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.data.AppData;
import com.mulungushi.campuscompanion.data.Student;

import java.util.HashMap;
import java.util.Map;

/** Lecturer privilege: view, edit, delete, and add students on the class roster. */
public class StudentRosterActivity extends AppCompatActivity {

    private AppData appData;
    private LinearLayout rosterContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_roster);

        appData = AppData.getInstance(this);
        rosterContainer = findViewById(R.id.rosterContainer);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditStudentActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRoster(); // re-read from AppData every time we come back (after add/edit/delete)
    }

    private void renderRoster() {
        rosterContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Student student : appData.getStudents()) {
            View row = inflater.inflate(R.layout.item_roster_row, rosterContainer, false);

            ((TextView) row.findViewById(R.id.tvName)).setText(student.name);
            ((TextView) row.findViewById(R.id.tvDetails)).setText(
                    "No: " + student.number + " • Assigned: " + student.group + " • " + student.program);

            Button btnEdit = row.findViewById(R.id.btnEdit);
            Button btnDelete = row.findViewById(R.id.btnDelete);

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditStudentActivity.class);
                intent.putExtra(EditStudentActivity.EXTRA_STUDENT_ID, student.id);
                startActivity(intent);
            });

            btnDelete.setOnClickListener(v -> confirmDelete(student));

            rosterContainer.addView(row);
        }
    }

    private void confirmDelete(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Remove student?")
                .setMessage("This will permanently remove " + student.name + " (" + student.number + ") from the roster.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> {
                    appData.deleteStudent(student.id);
                    renderRoster();
                })
                .show();
    }
}
