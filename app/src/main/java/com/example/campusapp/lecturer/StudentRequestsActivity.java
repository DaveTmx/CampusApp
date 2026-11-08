package com.example.campusapp.lecturer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.R;
import com.example.campusapp.data.remote.StudentDtos;
import com.example.campusapp.data.repository.StudentRequestRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.util.ServiceLocator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecturer approval of student requests: group changes and student-number corrections.
 * Nothing changes on a student's record until the lecturer approves it here.
 */
public class StudentRequestsActivity extends AppCompatActivity {

    private StudentRequestRepository repository;
    private LinearLayout requestsContainer;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_requests);

        repository = new StudentRequestRepository(ServiceLocator.apiService(this));
        requestsContainer = findViewById(R.id.requestsContainer);
        statusText = findViewById(R.id.tvRequestsStatus);
        findViewById(R.id.btnBackRequests).setOnClickListener(v -> finish());

        Map<NavTab, Class<?>> destinations = new HashMap<>();
        destinations.put(NavTab.HOME, LecturerHomeActivity.class);
        destinations.put(NavTab.SCHEDULE, LecturerScheduleActivity.class);
        destinations.put(NavTab.MAP, LecturerMapActivity.class);
        destinations.put(NavTab.NOTICES, LecturerNoticesActivity.class);
        destinations.put(NavTab.PROFILE, LecturerAccountActivity.class);
        new BottomNavHelper(this, findViewById(R.id.bottomNavInclude), NavTab.PROFILE, destinations);

        load();
    }

    private void load() {
        statusText.setText("Loading…");
        repository.listPending(result -> runOnUiThread(() -> {
            if (isFinishing()) return;
            if (!result.isSaved() || result.data == null) {
                statusText.setText(messageFor(result.status));
                requestsContainer.removeAllViews();
                return;
            }
            render(result.data);
        }));
    }

    private void render(List<StudentDtos.RequestDto> requests) {
        requestsContainer.removeAllViews();
        if (requests.isEmpty()) {
            statusText.setText("No pending requests.");
            return;
        }
        statusText.setText(requests.size() + " pending");
        for (StudentDtos.RequestDto request : requests) {
            requestsContainer.addView(buildRow(request));
        }
    }

    private View buildRow(StudentDtos.RequestDto request) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_button_outline);
        int pad = dp(14);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        row.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setText(request.studentName + "  •  No: " + request.studentNumber);
        name.setTextSize(15);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(name);

        TextView change = new TextView(this);
        change.setText(describe(request));
        change.setTextSize(13);
        change.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        change.setPadding(0, dp(4), 0, dp(10));
        row.addView(change);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button approve = new Button(this);
        approve.setText("Approve");
        approve.setAllCaps(false);
        Button reject = new Button(this);
        reject.setText("Reject");
        reject.setAllCaps(false);
        buttons.addView(approve, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(reject, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(buttons);

        approve.setOnClickListener(v -> decide(request.id, true, approve, reject));
        reject.setOnClickListener(v -> decide(request.id, false, approve, reject));
        return row;
    }

    private void decide(int requestId, boolean approve, Button approveBtn, Button rejectBtn) {
        approveBtn.setEnabled(false);
        rejectBtn.setEnabled(false);
        repository.decide(requestId, approve, result -> runOnUiThread(() -> {
            if (isFinishing()) return;
            if (result.isSaved()) {
                Toast.makeText(this, approve ? "Request approved." : "Request rejected.", Toast.LENGTH_SHORT).show();
                load();
            } else {
                approveBtn.setEnabled(true);
                rejectBtn.setEnabled(true);
                Toast.makeText(this, messageFor(result.status), Toast.LENGTH_LONG).show();
            }
        }));
    }

    private static String describe(StudentDtos.RequestDto request) {
        if ("GROUP_CHANGE".equals(request.type)) {
            return "Group change: " + request.currentGroup + " → " + request.requestedValue;
        }
        return "Number correction: " + request.studentNumber + " → " + request.requestedValue;
    }

    private static String messageFor(Result.Status status) {
        switch (status) {
            case OFFLINE: return "No connection. Try again when you are online.";
            case SESSION_EXPIRED: return "Your session has expired. Sign in again.";
            case GROUP_FULL: return "That group is full.";
            case DUPLICATE_NUMBER: return "That student number is already in use.";
            case CONFLICT: return "This request has already been decided.";
            case NOT_FOUND: return "This request or student no longer exists.";
            default: return "Could not update the request. Try again.";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
