package com.example.campusapp.ui.student;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.R;

public class StudentScheduleActivity extends AppCompatActivity {

    private TextView tabMon, tabTue, tabWed, tabThu, tabFri;
    private LinearLayout classesContainer;
    private String currentDay = "Mon";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_schedule);

        tabMon = findViewById(R.id.tabMon);
        tabTue = findViewById(R.id.tabTue);
        tabWed = findViewById(R.id.tabWed);
        tabThu = findViewById(R.id.tabThu);
        tabFri = findViewById(R.id.tabFri);
        classesContainer = findViewById(R.id.classesContainer);

        View.OnClickListener dayListener = v -> {
            TextView tv = (TextView) v;
            currentDay = tv.getText().toString();
            updateTabSelection();
            loadScheduleForDay(currentDay);
        };

        if (tabMon != null) tabMon.setOnClickListener(dayListener);
        if (tabTue != null) tabTue.setOnClickListener(dayListener);
        if (tabWed != null) tabWed.setOnClickListener(dayListener);
        if (tabThu != null) tabThu.setOnClickListener(dayListener);
        if (tabFri != null) tabFri.setOnClickListener(dayListener);

        updateTabSelection();
        loadScheduleForDay("Mon");
    }

    private void updateTabSelection() {
        TextView[] tabs = {tabMon, tabTue, tabWed, tabThu, tabFri};
        for (TextView tab : tabs) {
            if (tab != null) {
                if (tab.getText().toString().equals(currentDay)) {
                    tab.setBackgroundResource(R.drawable.bg_pill_selected);
                    tab.setTextColor(Color.parseColor("#12469E"));
                } else {
                    tab.setBackgroundColor(Color.TRANSPARENT);
                    tab.setTextColor(Color.parseColor("#6B7280"));
                }
            }
        }
    }

    private void loadScheduleForDay(String day) {
        if (classesContainer == null) return;
        classesContainer.removeAllViews();

        // Add sample schedule items for the selected day
        addClassItem("ICT372 - Advanced Android Dev", "08:00 - 10:00 • Lab 3", "Lecture", "#12469E");
        addClassItem("ICT381 - Software Engineering", "10:30 - 12:30 • Hall B", "Practical", "#C31432");
        addClassItem("MSS241 - Numerical Methods", "14:00 - 16:00 • Room 12", "Tutorial", "#2E7D32");
    }

    private void addClassItem(String title, String details, String type, String colorHex) {
        View view = getLayoutInflater().inflate(R.layout.item_class, classesContainer, false);
        View barColor = view.findViewById(R.id.barColor);
        TextView tvClassName = view.findViewById(R.id.tvClassName);
        TextView tvClassDetails = view.findViewById(R.id.tvClassDetails);
        TextView tvClassType = view.findViewById(R.id.tvClassType);

        if (barColor != null) barColor.setBackgroundColor(Color.parseColor(colorHex));
        if (tvClassName != null) tvClassName.setText(title);
        if (tvClassDetails != null) tvClassDetails.setText(details);
        if (tvClassType != null) {
            tvClassType.setText(type);
            tvClassType.setTextColor(Color.parseColor(colorHex));
        }

        classesContainer.addView(view);
    }
}
