package com.mulungushi.campuscompanion.lecturer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanion.BottomNavHelper;
import com.mulungushi.campuscompanion.NavTab;
import com.mulungushi.campuscompanion.R;
import com.mulungushi.campuscompanion.RoleSelectActivity;

import java.util.HashMap;
import java.util.Map;

public class LecturerAccountActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_account);

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);

        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        findViewById(R.id.tvStudentRoster).setOnClickListener(v ->
                startActivity(new Intent(this, StudentRosterActivity.class)));

        findViewById(R.id.tvGroups).setOnClickListener(v ->
                startActivity(new Intent(this, GroupsActivity.class)));

        findViewById(R.id.tvSyncStatus).setOnClickListener(v ->
                startActivity(new Intent(this, SyncStatusActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.dialog_logout, null);
            AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            view.findViewById(R.id.btnCancel).setOnClickListener(v2 -> dialog.dismiss());
            view.findViewById(R.id.btnConfirmLogout).setOnClickListener(v2 -> {
                dialog.dismiss();
                Intent intent = new Intent(this, RoleSelectActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            dialog.show();
        });
    }
}
