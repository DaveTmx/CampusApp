package com.example.campusapp.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.ClassSession;
import com.example.campusapp.data.Timetable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentScheduleActivity extends AppCompatActivity {

    private TextView[] dayTabs;
    private LinearLayout classesContainer;
    private int selectedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_schedule);

        classesContainer = findViewById(R.id.classesContainer);
        dayTabs = new TextView[]{
                findViewById(R.id.tabMon), findViewById(R.id.tabTue), findViewById(R.id.tabWed),
                findViewById(R.id.tabThu), findViewById(R.id.tabFri)
        };

        for (int i = 0; i < dayTabs.length; i++) {
            final int day = i;
            dayTabs[i].setOnClickListener(v -> selectDay(day));
        }

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.MAP, CampusMapActivity.class);
        destinations.put(NavTab.NOTICES, NoticesActivity.class);
        destinations.put(NavTab.PROFILE, StudentProfileActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.SCHEDULE, destinations);

        selectDay(todayIndex());
    }

    /** Today's tab on weekdays; Monday on weekends (there are no Sat/Sun classes). */
    private int todayIndex() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // Sunday=1 ... Saturday=7
        int index = dayOfWeek - Calendar.MONDAY;                           // Monday=0 ... Friday=4
        return (index >= 0 && index <= 4) ? index : 0;
    }

    private void selectDay(int day) {
        selectedDay = day;

        for (int i = 0; i < dayTabs.length; i++) {
            boolean selected = i == selectedDay;
            if (selected) {
                dayTabs[i].setBackgroundResource(R.drawable.bg_pill_selected);
                dayTabs[i].setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                dayTabs[i].setBackground(null);
                dayTabs[i].setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            }
        }

        renderClasses();
    }

    private void renderClasses() {
        classesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<ClassSession> sessions = Timetable.forDay(selectedDay);
        if (sessions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No classes scheduled for this day.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_gray_light));
            empty.setTextSize(13);
            classesContainer.addView(empty);
            return;
        }

        for (ClassSession session : sessions) {
            View card = inflater.inflate(R.layout.item_class, classesContainer, false);

            ((TextView) card.findViewById(R.id.tvClassName)).setText(session.name);
            ((TextView) card.findViewById(R.id.tvClassDetails)).setText(session.time + " • " + session.room);

            TextView tvType = card.findViewById(R.id.tvClassType);
            tvType.setText(session.type);

            View bar = card.findViewById(R.id.barColor);
            switch (session.type) {
                case "Practical":
                    bar.setBackgroundResource(R.drawable.bg_progress_fill_navy);
                    tvType.setBackgroundResource(R.drawable.bg_pill_green);
                    tvType.setTextColor(ContextCompat.getColor(this, R.color.badge_green));
                    break;
                case "Tutorial":
                    bar.setBackgroundResource(R.drawable.bg_progress_fill_red);
                    tvType.setBackgroundResource(R.drawable.bg_pill_orange);
                    tvType.setTextColor(ContextCompat.getColor(this, R.color.badge_orange));
                    break;
                default: // Lecture
                    bar.setBackgroundResource(R.drawable.bg_progress_fill_blue);
                    tvType.setBackgroundResource(R.drawable.bg_pill_blue);
                    tvType.setTextColor(ContextCompat.getColor(this, R.color.badge_blue));
                    break;
            }

            classesContainer.addView(card);
        }
    }
}
