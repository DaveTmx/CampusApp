package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;

import java.util.HashMap;
import java.util.Map;

public class LecturerHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_simple);

        ((TextView) findViewById(R.id.tvTitle)).setText("Welcome back");
        ((TextView) findViewById(R.id.tvSubtitle)).setText("Mulungushi University Portal");
        ((TextView) findViewById(R.id.tvBody)).setText(
                "Mr. E. Nyirenda\nDepartment of Computer Science\n\nUse the Profile tab to manage your rosters, groups, and sync status.");

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.HOME, destinations);
    }
}
