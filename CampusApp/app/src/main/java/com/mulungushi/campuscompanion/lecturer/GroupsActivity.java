package com.mulungushi.campuscompanion.lecturer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;

import java.util.HashMap;
import java.util.Map;

public class GroupsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);
    }
}
