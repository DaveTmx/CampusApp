package com.mulungushi.campuscompanion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires up the shared bottom navigation bar (layout_bottom_nav.xml) that is
 * included in every main screen, and highlights the active tab the same way
 * the design shows (selected = red icon/label, rest = light gray).
 *
 * destinations: map of each tab to the Activity class it should open.
 */
public class BottomNavHelper {

    private static class NavItemViews {
        final View row;
        final ImageView icon;
        final TextView label;

        NavItemViews(View row, ImageView icon, TextView label) {
            this.row = row;
            this.icon = icon;
            this.label = label;
        }
    }

    public BottomNavHelper(final Context context, View root, NavTab active, final Map<NavTab, Class<?>> destinations) {
        Map<NavTab, NavItemViews> items = new LinkedHashMap<>();
        items.put(NavTab.HOME, new NavItemViews(
                root.findViewById(R.id.navHome),
                root.findViewById(R.id.navHomeIcon),
                root.findViewById(R.id.navHomeLabel)));
        items.put(NavTab.SCHEDULE, new NavItemViews(
                root.findViewById(R.id.navSchedule),
                root.findViewById(R.id.navScheduleIcon),
                root.findViewById(R.id.navScheduleLabel)));
        items.put(NavTab.MAP, new NavItemViews(
                root.findViewById(R.id.navMap),
                root.findViewById(R.id.navMapIcon),
                root.findViewById(R.id.navMapLabel)));
        items.put(NavTab.NOTICES, new NavItemViews(
                root.findViewById(R.id.navNotices),
                root.findViewById(R.id.navNoticesIcon),
                root.findViewById(R.id.navNoticesLabel)));
        items.put(NavTab.PROFILE, new NavItemViews(
                root.findViewById(R.id.navProfile),
                root.findViewById(R.id.navProfileIcon),
                root.findViewById(R.id.navProfileLabel)));

        final int selectedColor = ContextCompat.getColor(context, R.color.primary_red);
        final int unselectedColor = ContextCompat.getColor(context, R.color.text_gray_light);

        for (final Map.Entry<NavTab, NavItemViews> entry : items.entrySet()) {
            final NavTab tab = entry.getKey();
            final NavItemViews views = entry.getValue();
            final boolean isActive = tab == active;

            views.icon.setColorFilter(isActive ? selectedColor : unselectedColor);
            views.label.setTextColor(isActive ? selectedColor : unselectedColor);

            views.row.setOnClickListener(v -> {
                if (!isActive) {
                    Class<?> target = destinations.get(tab);
                    if (target != null) {
                        context.startActivity(new Intent(context, target));
                        if (context instanceof Activity) {
                            Activity activity = (Activity) context;
                            activity.overridePendingTransition(0, 0);
                            activity.finish();
                        }
                    }
                }
            });
        }
    }
}
