package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;

import java.util.HashMap;
import java.util.Map;

public class LecturerScheduleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_simple);

        ((TextView) findViewById(R.id.tvTitle)).setText("Teaching Schedule");
        ((TextView) findViewById(R.id.tvSubtitle)).setText("Your lecture and lab timetable");
        ((TextView) findViewById(R.id.tvBody)).setText(
                "Database Systems — Mon 09:00, Room 102\nComputer Networks — Wed 14:00, Room 204");

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.SCHEDULE, destinations);
    }
}
