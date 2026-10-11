package com.example.campusapp.lecturer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.R;
import com.example.campusapp.data.Programs;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.EditorViewModel;

/**
 * Handles both "add a new student" and "edit an existing student" — the lecturer's editing
 * privilege on the roster. Start it with EditorViewModel.EXTRA_STUDENT_ID to edit an existing
 * record; leave the extra out to create a new one. All state lives in EditorViewModel.
 */
public class EditStudentActivity extends AppCompatActivity {

    /** Kept so older callers still compile; it is the same key the ViewModel reads. */
    public static final String EXTRA_STUDENT_ID = EditorViewModel.EXTRA_STUDENT_ID;

    private EditorViewModel viewModel;
    private AlertDialog conflictDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student);

        viewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(EditorViewModel.class);

        EditText etName = findViewById(R.id.etName);
        EditText etNumber = findViewById(R.id.etNumber);
        EditText etGroup = findViewById(R.id.etGroup);
        Spinner spProgram = findViewById(R.id.spProgram);
        Button btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Programs.ALL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);

        if (viewModel.isNewStudent()) {
            ((TextView) findViewById(R.id.tvHeader)).setText("Add Student");
            ((TextView) findViewById(R.id.tvSubheader)).setText("Create a new roster entry");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> viewModel.save());

        // Typing goes into the ViewModel; the ViewModel's values come back into the fields.
        // Each side only writes when the value really differs, so the two cannot loop.
        etName.addTextChangedListener(new SimpleWatcher(s -> viewModel.setName(s)));
        etNumber.addTextChangedListener(new SimpleWatcher(s -> viewModel.setNumber(s)));
        etGroup.addTextChangedListener(new SimpleWatcher(s -> viewModel.setGroup(s)));

        viewModel.getName().observe(this, value -> setIfDifferent(etName, value));
        viewModel.getNumber().observe(this, value -> setIfDifferent(etNumber, value));
        viewModel.getGroup().observe(this, value -> setIfDifferent(etGroup, value));
        viewModel.getProgramme().observe(this, code -> {
            int position = adapter.getPosition(Programs.toLabel(code));
            if (position >= 0 && spProgram.getSelectedItemPosition() != position) {
                spProgram.setSelection(position);
            }
        });
        spProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setProgramme(Programs.CODES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        viewModel.getNameError().observe(this, resId -> etName.setError(resId == null ? null : getString(resId)));
        viewModel.getNumberError().observe(this, resId -> etNumber.setError(resId == null ? null : getString(resId)));
        viewModel.getBusy().observe(this, busy -> btnSave.setEnabled(!Boolean.TRUE.equals(busy)));

        viewModel.getMessage().observe(this, event -> {
            Integer id = event == null ? null : event.getIfNotHandled();
            if (id != null) {
                Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getFinished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) {
                finish();
            }
        });
        viewModel.getConflict().observe(this, this::showConflict);
    }

    @Override
    protected void onDestroy() {
        if (conflictDialog != null && conflictDialog.isShowing()) {
            conflictDialog.dismiss();
        }
        super.onDestroy();
    }

    /** Someone else changed the record first: show both versions and let the lecturer choose. */
    private void showConflict(Student server) {
        if (server == null) {
            return;
        }
        if (conflictDialog != null && conflictDialog.isShowing()) {
            return;
        }
        String serverGroup = server.confirmedGroup == null ? "Unassigned" : server.confirmedGroup;
        conflictDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.msg_conflict)
                .setMessage("Saved version:\n" + server.name + "\nNo: " + server.studentNumber
                        + "\n" + Programs.toLabel(server.programme) + " • " + serverGroup)
                .setCancelable(false)
                .setNegativeButton("Use saved version", (d, which) -> viewModel.useServerVersion())
                .setPositiveButton("Keep my changes", (d, which) -> viewModel.keepMyChanges())
                .show();
    }

    private static void setIfDifferent(EditText field, String value) {
        String wanted = value == null ? "" : value;
        if (!wanted.equals(field.getText().toString())) {
            field.setText(wanted);
            field.setSelection(field.length());
        }
    }

    private interface TextListener {
        void onText(String text);
    }

    private static class SimpleWatcher implements TextWatcher {
        private final TextListener listener;

        SimpleWatcher(TextListener listener) {
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            listener.onText(s.toString());
        }
    }
}
