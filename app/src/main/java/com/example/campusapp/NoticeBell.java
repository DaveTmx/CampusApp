package com.example.campusapp;

import android.view.View;
import android.widget.TextView;

import com.example.campusapp.Data.Notice;

import java.util.List;

/** Shows the orange bell and its red unread count. Use in any screen header. */
public final class NoticeBell {

    private NoticeBell() { }

    public static int unreadCount(List<Notice> notices) {
        int count = 0;
        for (Notice n : notices) {
            if (n.isNew) {
                count++;
            }
        }
        return count;
    }

    public static void bind(View bell, int unread, Runnable onClick) {
        TextView badge = bell.findViewById(R.id.bellBadge);
        if (unread <= 0) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            badge.setVisibility(View.VISIBLE);
        }
        bell.setContentDescription(
                bell.getContext().getString(R.string.notices_bell_description, unread));
        bell.setOnClickListener(v -> onClick.run());
    }
}