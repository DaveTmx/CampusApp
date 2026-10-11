package com.example.campusapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.lecturer.LecturerHomeActivity;
import com.example.campusapp.model.Session;
import com.example.campusapp.student.StudentHomeActivity;
import com.example.campusapp.util.ServiceLocator;
import com.example.campusapp.viewmodel.AuthViewModel;

/**
 * The launcher screen. It shows the logo for a moment, then opens the right place:
 * the student or lecturer home for someone who is already signed in, or the
 * role picker for everyone else.
 */
public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_MS = 1200;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openNextScreen = this::openNextScreen;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authViewModel = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
                .get(AuthViewModel.class);

        handler.postDelayed(openNextScreen, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openNextScreen);
        super.onDestroy();
    }

    private void openNextScreen() {
        Session session = authViewModel.getSession().getValue();
        Class<?> target;
        if (session == null) {
            target = RoleSelectActivity.class;
        } else if (session.isLecturer()) {
            target = LecturerHomeActivity.class;
        } else {
            target = StudentHomeActivity.class;
        }
        startActivity(new Intent(this, target));
        finish();
    }
}
