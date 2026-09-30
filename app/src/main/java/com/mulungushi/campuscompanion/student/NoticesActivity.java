package com.mulungushi.campuscompanion.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.data.AppData;
import com.mulungushi.campuscompanion.data.Notice;

import java.util.HashMap;
import java.util.Map;

public class NoticesActivity extends AppCompatActivity {

    private static final String FILTER_ALL = "All";

    private AppData appData;
    private LinearLayout noticesContainer;
    private TextView chipAll, chipAcademic, chipAdmin, chipUrgent;
    private String selectedFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);

        appData = AppData.getInstance(this);
        noticesContainer = findViewById(R.id.noticesContainer);

        chipAll = findViewById(R.id.chipAll);
        chipAcademic = findViewById(R.id.chipAcademic);
        chipAdmin = findViewById(R.id.chipAdmin);
        chipUrgent = findViewById(R.id.chipUrgent);

        chipAll.setOnClickListener(v -> selectFilter(FILTER_ALL));
        chipAcademic.setOnClickListener(v -> selectFilter("Academic"));
        chipAdmin.setOnClickListener(v -> selectFilter("Admin"));
        chipUrgent.setOnClickListener(v -> selectFilter("Urgent"));

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, StudentHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, StudentScheduleActivity.class);
        destinations.put(NavTab.MAP, CampusMapActivity.class);
        destinations.put(NavTab.PROFILE, StudentProfileActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.NOTICES, destinations);

        highlightChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNotices(); // picks up anything the lecturer just posted
    }

    private void selectFilter(String filter) {
        selectedFilter = filter;
        highlightChips();
        renderNotices();
    }

    private void highlightChips() {
        highlight(chipAll, FILTER_ALL.equals(selectedFilter));
        highlight(chipAcademic, "Academic".equals(selectedFilter));
        highlight(chipAdmin, "Admin".equals(selectedFilter));
        highlight(chipUrgent, "Urgent".equals(selectedFilter));
    }

    private void highlight(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_pill_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackground(null);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        }
    }

    private void renderNotices() {
        noticesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int shown = 0;

        for (Notice notice : appData.getNotices()) {
            if (!FILTER_ALL.equals(selectedFilter) && !selectedFilter.equals(notice.category)) {
                continue; // not in the selected category
            }

            View row = inflater.inflate(R.layout.item_notice, noticesContainer, false);

            TextView tvCategory = row.findViewById(R.id.tvCategory);
            tvCategory.setText(notice.category);
            applyCategoryStyle(tvCategory, notice.category);

            row.findViewById(R.id.tvNewBadge).setVisibility(notice.isNew ? View.VISIBLE : View.GONE);
            ((TextView) row.findViewById(R.id.tvNoticeTitle)).setText(notice.title);
            ((TextView) row.findViewById(R.id.tvNoticeBody)).setText(notice.body);

            noticesContainer.addView(row);
            shown++;
        }

        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("No " + selectedFilter.toLowerCase() + " notices right now.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_gray_light));
            empty.setTextSize(13);
            noticesContainer.addView(empty);
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
