package com.example.campusapp;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Apps that target Android 15+ are drawn edge to edge: without this, every screen's header
 * slides under the clock and the bottom tabs sit under the gesture bar. After each screen is
 * created this pads its content by the system bars (and the keyboard), and paints the space
 * they occupy in the app's navy, so no screen has to handle it itself.
 */
public class CampusApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
                fitSystemBars(activity);
            }

            @Override public void onActivityStarted(@NonNull Activity activity) { }
            @Override public void onActivityResumed(@NonNull Activity activity) { }
            @Override public void onActivityPaused(@NonNull Activity activity) { }
            @Override public void onActivityStopped(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle out) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }

    private static void fitSystemBars(Activity activity) {
        // Same on every Android version: we draw behind the bars and pad the content ourselves.
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View decor = activity.getWindow().getDecorView();
        WindowCompat.getInsetsController(activity.getWindow(), decor)
                .setAppearanceLightStatusBars(false); // white clock and icons on navy

        int navy = ContextCompat.getColor(activity, R.color.navy_dark);
        int page = ContextCompat.getColor(activity, R.color.background);
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // Navy behind the bars, the usual page colour inside them.
            LayerDrawable background = new LayerDrawable(new Drawable[]{
                    new ColorDrawable(navy),
                    new InsetDrawable(new ColorDrawable(page),
                            bars.left, bars.top, bars.right, bars.bottom)});
            v.setBackground(background);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
