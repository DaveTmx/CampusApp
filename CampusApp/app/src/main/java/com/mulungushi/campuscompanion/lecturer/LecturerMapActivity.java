package com.mulungushi.campuscompanion.lecturer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;

import java.util.HashMap;
import java.util.Map;

public class LecturerMapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_simple);

        ((TextView) findViewById(R.id.tvTitle)).setText("Find Offices");
        ((TextView) findViewById(R.id.tvSubtitle)).setText("Campus map navigation");
        ((TextView) findViewById(R.id.tvBody)).setText(
                "Academic Registry — Main Admin Block, 1st Floor\nDepartment of Computer Science — Faculty Block B");

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.MAP, destinations);
    }
}
