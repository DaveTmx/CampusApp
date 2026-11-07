package com.example.campusapp.lecturer;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Programs;
import com.example.campusapp.data.Timetable;
import com.example.campusapp.data.TimetableEntry;
import com.example.campusapp.util.TimetableCsv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The lecturer's timetable screen. Classes are stored in AppData, which is what the students'
 * Schedule tab reads. A lecturer can add a class with a form, remove one by tapping it,
 * or import a whole timetable from a CSV file.
 */
public class LecturerScheduleActivity extends AppCompatActivity {

    private static final int MAX_FILE_BYTES = 1024 * 1024;

    private AppData appData;
    private LinearLayout container;
    /** The programme whose timetable is shown and edited (CS, IT, DS or CYB). */
    private String programme = Programs.CODES[0];

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFilePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_schedule);

        appData = AppData.getInstance(this);
        container = findViewById(R.id.scheduleContainer);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);
        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.SCHEDULE, destinations);

        findViewById(R.id.btnAddClass).setOnClickListener(v -> showAddDialog());
        findViewById(R.id.btnImportCsv).setOnClickListener(v -> showImportHelp());

        // Each programme has its own timetable; students see the one for their programme.
        Spinner spProgram = findViewById(R.id.spScheduleProgram);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Programs.ALL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgram.setAdapter(adapter);
        spProgram.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                programme = Programs.CODES[position];
                render();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        render();
    }

    // ----- The list -----

    private void render() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int total = 0;

        for (int day = 0; day < Timetable.DAYS.length; day++) {
            List<TimetableEntry> classes = appData.getClassesForDay(programme, day);
            total += classes.size();
            if (classes.isEmpty()) continue;

            TextView heading = new TextView(this);
            heading.setText(Timetable.DAYS[day]);
            heading.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            heading.setTextSize(12);
            heading.setAllCaps(true);
            int pad = (int) (8 * getResources().getDisplayMetrics().density);
            heading.setPadding(0, pad * 2, 0, pad);
            container.addView(heading);

            for (TimetableEntry entry : classes) {
                View card = inflater.inflate(R.layout.item_class, container, false);
                ((TextView) card.findViewById(R.id.tvClassName)).setText(entry.name);
                ((TextView) card.findViewById(R.id.tvClassDetails)).setText(entry.time + " • " + entry.room);
                ((TextView) card.findViewById(R.id.tvClassType)).setText(entry.type);
                card.setOnClickListener(v -> confirmDelete(entry));
                container.addView(card);
            }
        }

        if (total == 0) {
            TextView empty = new TextView(this);
            empty.setText("No " + Programs.toLabel(programme)
                    + " classes yet. Add one, or import a CSV file.");
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            int pad = (int) (32 * getResources().getDisplayMetrics().density);
            empty.setPadding(pad, pad, pad, pad);
            container.addView(empty);
        } else {
            TextView hint = new TextView(this);
            hint.setText("Tap a class to remove it.");
            hint.setTextColor(ContextCompat.getColor(this, R.color.text_gray_light));
            hint.setTextSize(12);
            hint.setPadding(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
            container.addView(hint);
        }
    }

    private void confirmDelete(TimetableEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Remove class?")
                .setMessage(entry.name + "\n" + Timetable.DAYS[entry.day] + " " + entry.time)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (d, which) -> {
                    appData.deleteClass(entry.id);
                    render();
                })
                .show();
    }

    // ----- Add one class -----

    private void showAddDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad, pad, 0);

        Spinner spDay = new Spinner(this);
        spDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Timetable.DAYS));
        form.addView(spDay);

        EditText etName = field("Class name, e.g. Database Systems");
        EditText etTime = field("Time, e.g. 09:00 - 10:30");
        EditText etRoom = field("Room, e.g. Room 102");
        form.addView(etName);
        form.addView(etTime);
        form.addView(etRoom);

        Spinner spType = new Spinner(this);
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TimetableCsv.TYPES));
        form.addView(spType);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add " + Programs.toLabel(programme) + " class")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null) // set below so a mistake keeps the dialog open
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Enter the class name");
            } else if (!TimetableCsv.isValidTime(time)) {
                etTime.setError("Use a time like 09:00 - 10:30");
            } else if (room.isEmpty()) {
                etRoom.setError("Enter the room");
            } else {
                appData.addClass(programme, spDay.getSelectedItemPosition(), name,
                        time.replaceAll("\\s*-\\s*", " - "), room, TimetableCsv.TYPES[spType.getSelectedItemPosition()]);
                dialog.dismiss();
                render();
                Toast.makeText(this, "Class added", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        return e;
    }

    // ----- Import a CSV file -----

    private void showImportHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Import timetable (CSV)")
                .setMessage("One class per line:\n\nday,name,time,room,type\n"
                        + "Mon,Database Systems,09:00 - 10:30,Room 102,Lecture\n"
                        + "Tue,Mobile Dev Lab,11:00 - 12:30,Lab 3,Practical\n\n"
                        + "Day is Mon to Fri. Type is Lecture, Practical or Tutorial. "
                        + "You can save a spreadsheet as CSV from Excel or Google Sheets.\n\n"
                        + "Importing replaces the " + Programs.toLabel(programme)
                        + " timetable only. Choose the programme above first.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose file", (d, which) -> filePicker.launch(new String[]{"*/*"}))
                .show();
    }

    private void onFilePicked(Uri uri) {
        if (uri == null) return; // the lecturer backed out
        String text;
        try {
            text = readText(uri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not read that file.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            List<TimetableEntry> entries = TimetableCsv.parse(text);
            new AlertDialog.Builder(this)
                    .setTitle("Replace " + Programs.toLabel(programme) + " timetable?")
                    .setMessage(entries.size() + " classes found. This replaces the "
                            + Programs.toLabel(programme) + " timetable. Other programmes are not changed.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Replace", (d, which) -> {
                        appData.replaceTimetable(programme, entries);
                        render();
                        Toast.makeText(this, "Timetable imported", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        } catch (TimetableCsv.ParseException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Could not import")
                    .setMessage(e.getMessage() + "\n\nNothing was changed.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private String readText(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("No stream");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > MAX_FILE_BYTES) throw new IOException("File too large");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
