package com.mulungushi.campuscompanion.student;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;

import java.util.HashMap;
import java.util.Map;

public class CampusMapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_map);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);
        destinations.put(NavTab.PROFILE, StudentProfileActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.MAP, destinations);
    }
}
