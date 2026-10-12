package com.example.campusapp.student;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusapp.BottomNavHelper;
import com.example.campusapp.NavTab;
import com.example.campusapp.NoticeBell;
import com.example.campusapp.R;
import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Notice;
import com.example.campusapp.data.Student;
import com.example.campusapp.model.Session;
import com.example.campusapp.util.CurrentProgram;
import com.example.campusapp.util.NextClass;
import com.example.campusapp.util.ServiceLocator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentHomeActivity extends AppCompatActivity {

    /** Re-checks the next class every minute so "In 15 mins" stays correct. */
    private static final long TICK_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            showNextClass();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private AppData appData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        appData = AppData.getInstance(this);

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

        findViewById(R.id.cardNextClass).setOnClickListener(v ->
                startActivity(new Intent(this, StudentScheduleActivity.class)));

        findViewById(R.id.cardLatestNotice).setOnClickListener(v -> openNotices());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Everything here can change on other screens (profile name, timetable, notices),
        // so it is re-read every time the home screen comes back.
        showGreeting();
        showLatestNotice();
        showStudyGroup();
        handler.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void openNotices() {
        startActivity(new Intent(this, NoticesActivity.class));
    }

    /** The signed-in student, or null for a guest. */
    private Student me() {
        Session session = ServiceLocator.authRepository(this).currentSession();
        if (session == null || session.isLecturer()) {
            return null;
        }
        return appData.findStudentById(session.accountId);
    }

    private void showGreeting() {
        Student me = me();
        String name = me == null || me.name == null || me.name.trim().isEmpty()
                ? "Guest" : me.name.trim();
        ((TextView) findViewById(R.id.tvGreeting)).setText("Hi, " + name);

        String today = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        ((TextView) findViewById(R.id.tvDate)).setText(today);
    }

    private void showNextClass() {
        TextView badge = findViewById(R.id.tvNextBadge);
        TextView name = findViewById(R.id.tvNextName);
        TextView details = findViewById(R.id.tvNextDetails);

        String programme = CurrentProgram.code(this); // the student's own programme timetable
        NextClass next = NextClass.find(day -> appData.getClassesForDay(programme, day),
                Calendar.getInstance());
        if (next == null) {
            badge.setText("NEXT CLASS");
            name.setText("No upcoming classes");
            details.setText("Your timetable is empty");
            return;
        }
        badge.setText("NEXT CLASS · " + next.whenLabel());
        name.setText(next.session.name);
        details.setText(next.session.time + " • " + next.session.room);
    }

    private void showLatestNotice() {
        List<Notice> notices = appData.getNotices();
        TextView label = findViewById(R.id.tvLatestLabel);
        TextView title = findViewById(R.id.tvLatestTitle);
        TextView body = findViewById(R.id.tvLatestBody);

        if (notices.isEmpty()) {
            label.setText("LATEST NOTICE");
            title.setText("No notices yet");
            body.setText("");
        } else {
            Notice latest = notices.get(0); // newest first
            label.setText(latest.isNew ? "LATEST NOTICE · NEW" : "LATEST NOTICE");
            title.setText(latest.title);
            body.setText(latest.body);
        }

        NoticeBell.bind(findViewById(R.id.noticeBell), NoticeBell.unreadCount(notices),
                this::openNotices);
    }

    private void showStudyGroup() {
        Student me = me();
        TextView group = findViewById(R.id.tvGroupName);
        if (me == null || me.group == null || me.group.isEmpty()
                || "Unassigned".equalsIgnoreCase(me.group)) {
            group.setText("No group yet");
        } else {
            group.setText("Group " + me.group);
        }
    }
}
