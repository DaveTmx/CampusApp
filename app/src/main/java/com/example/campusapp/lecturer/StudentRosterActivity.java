package com.example.campusapp.lecturer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.Programs;
import com.example.campusapp.data.repository.StudentRepository;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.EditorViewModel;
import com.example.campusapp.viewmodel.RosterViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecturer privilege: view, search, filter, edit, delete, and add students on the class roster.
 * All state (search text, filters, paging) lives in RosterViewModel.
 */
public class StudentRosterActivity extends AppCompatActivity {

    private RosterViewModel viewModel;
    private LinearLayout rosterContainer;

    private final Map<String, TextView> chips = new HashMap<>();
    private List<Student> students;
    private boolean loading;
    private boolean hasMore;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_roster);

        viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(RosterViewModel.class);
        rosterContainer = findViewById(R.id.rosterContainer);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, EditStudentActivity.class)));

        setUpSearch();
        setUpFilterChips();

        viewModel.getStudents().observe(this, list -> {
            students = list;
            render();
        });
        viewModel.getLoading().observe(this, isLoading -> {
            loading = Boolean.TRUE.equals(isLoading);
            render();
        });
        viewModel.getHasMore().observe(this, more -> {
            hasMore = Boolean.TRUE.equals(more);
            render();
        });
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
        // Coming back from the editor: the roster may have changed. The first resume is skipped
        // because the ViewModel has just loaded it.
        if (firstResume) {
            firstResume = false;
        } else {
            viewModel.refresh();
        }
    }

    // ----- Search and filters -----

    private void setUpSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s.toString());
            }
        });
        // The ViewModel remembers the query across rotation; show it again if the box is empty.
        viewModel.getQuery().observe(this, query -> {
            if (query != null && !query.isEmpty() && etSearch.getText().length() == 0) {
                etSearch.setText(query);
            }
        });
    }

    private void setUpFilterChips() {
        chips.put("ALL", findViewById(R.id.chipAll));
        chips.put("G01", findViewById(R.id.chipG01));
        chips.put("G02", findViewById(R.id.chipG02));
        chips.put("G03", findViewById(R.id.chipG03));
        chips.put("G04", findViewById(R.id.chipG04));
        chips.put(StudentRepository.UNASSIGNED, findViewById(R.id.chipUnassigned));

        for (Map.Entry<String, TextView> entry : chips.entrySet()) {
            final String key = entry.getKey();
            entry.getValue().setOnClickListener(v ->
                    viewModel.setGroupFilter("ALL".equals(key) ? null : key));
        }

        viewModel.getGroupFilter().observe(this, group -> {
            String selected = group == null ? "ALL" : group;
            for (Map.Entry<String, TextView> entry : chips.entrySet()) {
                boolean on = entry.getKey().equals(selected);
                TextView chip = entry.getValue();
                chip.setBackgroundResource(on ? R.drawable.bg_pill_selected : R.drawable.bg_button_outline);
                chip.setTextColor(ContextCompat.getColor(this, on ? R.color.white : R.color.text_dark));
            }
        });
    }

    // ----- The list -----

    private void render() {
        rosterContainer.removeAllViews();
        if (students == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Student student : students) {
            View row = inflater.inflate(R.layout.item_roster_row, rosterContainer, false);

            ((TextView) row.findViewById(R.id.tvName)).setText(student.name);
            String group = student.confirmedGroup == null ? "Unassigned" : student.confirmedGroup;
            ((TextView) row.findViewById(R.id.tvDetails)).setText(
                    "No: " + student.studentNumber + " • Assigned: " + group
                            + " • " + Programs.toLabel(student.programme));

            Button btnEdit = row.findViewById(R.id.btnEdit);
            Button btnDelete = row.findViewById(R.id.btnDelete);

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditStudentActivity.class);
                intent.putExtra(EditorViewModel.EXTRA_STUDENT_ID, student.studentId);
                startActivity(intent);
            });
            btnDelete.setOnClickListener(v -> confirmDelete(student));

            rosterContainer.addView(row);
        }

        if (students.isEmpty() && !loading) {
            rosterContainer.addView(infoText("No students found."));
        }
        if (loading) {
            rosterContainer.addView(infoText("Loading…"));
        } else if (hasMore) {
            Button more = new Button(this);
            more.setText("Load more");
            more.setAllCaps(false);
            more.setOnClickListener(v -> viewModel.loadNextPage());
            rosterContainer.addView(more);
        }
    }

    private TextView infoText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(android.view.Gravity.CENTER);
        view.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        view.setPadding(pad, pad, pad, pad);
        return view;
    }

    private void confirmDelete(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Remove student?")
                .setMessage("This will permanently remove " + student.name + " (" + student.studentNumber + ") from the roster.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> viewModel.delete(student.studentId))
                .show();
    }
}
