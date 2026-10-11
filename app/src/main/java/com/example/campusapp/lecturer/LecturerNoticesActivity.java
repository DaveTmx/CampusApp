package com.example.campusapp.lecturer;

import android.content.Intent;
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
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Notice;

import java.util.HashMap;
import java.util.Map;

/** Lecturer privilege: post updates that students see on their Notices tab. */
public class LecturerNoticesActivity extends AppCompatActivity {

    private AppData appData;
    private LinearLayout noticesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_notices);

        appData = AppData.getInstance(this);
        noticesContainer = findViewById(R.id.noticesContainer);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.NOTICES, destinations);

        findViewById(R.id.fabAddNotice).setOnClickListener(v ->
                startActivity(new Intent(this, PostNoticeActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNotices();
    }

    private void renderNotices() {
        noticesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Notice notice : appData.getNotices()) {
            View row = inflater.inflate(R.layout.item_notice, noticesContainer, false);

            TextView tvCategory = row.findViewById(R.id.tvCategory);
            tvCategory.setText(notice.category);
            applyCategoryStyle(tvCategory, notice.category);

            row.findViewById(R.id.tvNewBadge).setVisibility(notice.isNew ? View.VISIBLE : View.GONE);
            ((TextView) row.findViewById(R.id.tvNoticeTitle)).setText(notice.title);
            ((TextView) row.findViewById(R.id.tvNoticeBody)).setText(notice.body);

            noticesContainer.addView(row);
        }
    }

    private void applyCategoryStyle(TextView chip, String category) {
        int bg;
        int color;
        switch (category) {
            case "Admin":
                bg = R.drawable.bg_pill_blue;
                color = R.color.badge_blue;
                break;
            case "Urgent":
                bg = R.drawable.bg_pill_red;
                color = R.color.badge_red;
                break;
            default: // Academic
                bg = R.drawable.bg_pill_green;
                color = R.color.badge_green;
                break;
        }
        chip.setBackgroundResource(bg);
        chip.setTextColor(ContextCompat.getColor(this, color));
    }
}
