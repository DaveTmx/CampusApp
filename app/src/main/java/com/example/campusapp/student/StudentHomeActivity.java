package com.example.campusapp.student;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;

import java.util.HashMap;
import java.util.Map;

public class StudentHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.MAP, CampusMapActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);
        destinations.put(NavTab.PROFILE, StudentProfileActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.HOME, destinations);

        findViewById(R.id.btnGroupChat).setOnClickListener(v ->
                startActivity(new Intent(this, GroupChatActivity.class)));

        findViewById(R.id.cardStudyGroup).setOnClickListener(v ->
                startActivity(new Intent(this, GroupChatActivity.class)));
    }
}
