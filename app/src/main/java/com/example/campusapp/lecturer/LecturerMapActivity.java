package com.example.campusapp.lecturer;

import com.example.campusapp.NavTab;
import com.example.campusapp.student.CampusMapActivity;

import java.util.HashMap;
import java.util.Map;

/** The lecturer's campus map: the same screen as the students', with every programme's venues. */
public class LecturerMapActivity extends CampusMapActivity {

    @Override
    protected boolean allProgrammes() {
        return true;
    }

    @Override
    protected Map<NavTab, Class<?>> destinations() {
        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);
        return destinations;
    }
}
