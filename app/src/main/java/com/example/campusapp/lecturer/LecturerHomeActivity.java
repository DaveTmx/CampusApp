package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.remote.TokenStore;

import java.util.HashMap;
import java.util.Map;

public class LecturerHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_simple);

        ((TextView) findViewById(R.id.tvTitle)).setText("Welcome back");
        ((TextView) findViewById(R.id.tvSubtitle)).setText("Mulungushi University Portal");
        String name = new TokenStore(this).fullName();
        String greeting = (name == null || name.isEmpty() ? "" : name + "\n\n")
                + "Use the Profile tab to manage your rosters, groups, and sync status.";
        ((TextView) findViewById(R.id.tvBody)).setText(greeting);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.HOME, destinations);
    }
}
