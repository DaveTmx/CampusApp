package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.PendingChange;

import java.util.HashMap;
import java.util.Map;

/** Lecturer privilege: approve or reject student-submitted change requests. */
public class SyncStatusActivity extends AppCompatActivity {

    private AppData appData;
    private LinearLayout pendingContainer;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_status);

        appData = AppData.getInstance(this);
        pendingContainer = findViewById(R.id.pendingContainer);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.btnSyncNow).setOnClickListener(v -> {
            appData.approveAllPendingChanges();
            Toast.makeText(this, "All requests approved", Toast.LENGTH_SHORT).show();
            renderQueue();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderQueue();
    }

    private void renderQueue() {
        pendingContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (PendingChange change : appData.getPendingChanges()) {
            View row = inflater.inflate(R.layout.item_pending_change, pendingContainer, false);

            ((TextView) row.findViewById(R.id.tvChangeTitle)).setText(
                    change.fieldLabel + " change: " + change.studentName);
            ((TextView) row.findViewById(R.id.tvChangeDetail)).setText(
                    "Requested new " + change.fieldLabel.toLowerCase() + ": " + change.newValue);

            Button btnApprove = row.findViewById(R.id.btnApprove);
            Button btnReject = row.findViewById(R.id.btnReject);

            btnApprove.setOnClickListener(v -> {
                appData.approvePendingChange(change.id);
                Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show();
                renderQueue();
            });

            btnReject.setOnClickListener(v -> {
                appData.rejectPendingChange(change.id);
                Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show();
                renderQueue();
            });

            pendingContainer.addView(row);
        }

        boolean empty = appData.getPendingChanges().isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnSyncNow).setEnabled(!empty);
    }
}
