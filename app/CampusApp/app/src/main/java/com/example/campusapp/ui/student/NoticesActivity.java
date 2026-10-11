package com.example.campusapp.ui.student;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.R;

public class NoticesActivity extends AppCompatActivity {

    private TextView chipAll, chipAcademic, chipAdmin, chipUrgent;
    private LinearLayout noticesContainer;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);

        chipAll = findViewById(R.id.chipAll);
        chipAcademic = findViewById(R.id.chipAcademic);
        chipAdmin = findViewById(R.id.chipAdmin);
        chipUrgent = findViewById(R.id.chipUrgent);
        noticesContainer = findViewById(R.id.noticesContainer);

        View.OnClickListener filterListener = v -> {
            TextView tv = (TextView) v;
            currentFilter = tv.getText().toString();
            updateChipSelection();
            loadNotices(currentFilter);
        };

        if (chipAll != null) chipAll.setOnClickListener(filterListener);
        if (chipAcademic != null) chipAcademic.setOnClickListener(filterListener);
        if (chipAdmin != null) chipAdmin.setOnClickListener(filterListener);
        if (chipUrgent != null) chipUrgent.setOnClickListener(filterListener);

        updateChipSelection();
        loadNotices("All");
    }

    private void updateChipSelection() {
        TextView[] chips = {chipAll, chipAcademic, chipAdmin, chipUrgent};
        for (TextView chip : chips) {
            if (chip != null) {
                if (chip.getText().toString().equals(currentFilter)) {
                    chip.setBackgroundResource(R.drawable.bg_pill_selected);
                    chip.setTextColor(Color.parseColor("#12469E"));
                } else {
                    chip.setBackgroundColor(Color.TRANSPARENT);
                    chip.setTextColor(Color.parseColor("#6B7280"));
                }
            }
        }
    }

    private void loadNotices(String filter) {
        if (noticesContainer == null) return;
        noticesContainer.removeAllViews();

        if (filter.equals("All") || filter.equals("Academic")) {
            addNoticeItem("Academic", "Mid-Semester Examinations Schedule Released", "Exams are scheduled to begin on 20th October 2026. Please check your student portal for your specific venue and timetable.", true);
        }
        if (filter.equals("All") || filter.equals("Admin")) {
            addNoticeItem("Admin", "Library Operating Hours Extended", "The main university library will remain open until 22:00 hrs during the examination preparation period.", false);
        }
        if (filter.equals("All") || filter.equals("Urgent")) {
            addNoticeItem("Urgent", "System Maintenance Notice", "Campus Connect online services will undergo routine maintenance tonight from 00:00 to 03:00 hrs.", true);
        }
    }

    private void addNoticeItem(String category, String title, String body, boolean isNew) {
        View view = getLayoutInflater().inflate(R.layout.item_notice, noticesContainer, false);
        TextView tvCategory = view.findViewById(R.id.tvCategory);
        TextView tvNewBadge = view.findViewById(R.id.tvNewBadge);
        TextView tvNoticeTitle = view.findViewById(R.id.tvNoticeTitle);
        TextView tvNoticeBody = view.findViewById(R.id.tvNoticeBody);

        if (tvCategory != null) tvCategory.setText(category);
        if (tvNewBadge != null) tvNewBadge.setVisibility(isNew ? View.VISIBLE : View.GONE);
        if (tvNoticeTitle != null) tvNoticeTitle.setText(title);
        if (tvNoticeBody != null) tvNoticeBody.setText(body);

        noticesContainer.addView(view);
    }
}
