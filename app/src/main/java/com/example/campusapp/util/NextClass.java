package com.example.campusapp.util;

import com.example.campusapp.data.ClassSession;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Finds the next class on the timetable after a given moment, looking up to a week ahead.
 * Used by the student home screen's "Next class" card.
 */
public final class NextClass {

    /** Where the timetable comes from: dayIndex 0 = Mon ... 4 = Fri. */
    public interface Source {
        List<? extends ClassSession> classesForDay(int dayIndex);
    }

    public final ClassSession session;
    /** 0 = today, 1 = tomorrow, ... */
    public final int daysAhead;
    /** Minutes from now until it starts (can be large when it is days away). */
    public final int minutesUntil;
    /** 0 = Mon ... 4 = Fri. */
    public final int dayIndex;

    private NextClass(ClassSession session, int daysAhead, int minutesUntil, int dayIndex) {
        this.session = session;
        this.daysAhead = daysAhead;
        this.minutesUntil = minutesUntil;
        this.dayIndex = dayIndex;
    }

    private static final String[] DAY_NAMES =
            {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    /** The next class that starts after now, or null when the timetable is empty. */
    public static NextClass find(Source source, Calendar now) {
        int today = mondayIndex(now);
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (int ahead = 0; ahead <= 7; ahead++) {
            int day = (today + ahead) % 7;
            if (day > 4) {
                continue; // no classes on Saturday or Sunday
            }
            ClassSession best = null;
            int bestStart = Integer.MAX_VALUE;
            for (ClassSession s : source.classesForDay(day)) {
                int start = startMinutes(s.time);
                if (start < 0) {
                    continue; // a time we cannot read
                }
                if (ahead == 0 && start <= nowMinutes) {
                    continue; // already started or finished today
                }
                if (ahead == 7 && start > nowMinutes) {
                    continue; // a week from now, later than this moment: not "next" any more
                }
                if (start < bestStart) {
                    best = s;
                    bestStart = start;
                }
            }
            if (best != null) {
                int minutes = ahead * 24 * 60 + bestStart - nowMinutes;
                return new NextClass(best, ahead, minutes, day);
            }
        }
        return null;
    }

    /** "In 15 mins", "In 2 h 5 min", "Tomorrow", "Wednesday". */
    public String whenLabel() {
        if (daysAhead == 0) {
            if (minutesUntil < 60) {
                return "In " + minutesUntil + (minutesUntil == 1 ? " min" : " mins");
            }
            int h = minutesUntil / 60;
            int m = minutesUntil % 60;
            return m == 0 ? "In " + h + " h" : "In " + h + " h " + m + " min";
        }
        if (daysAhead == 1) {
            return "Tomorrow";
        }
        return DAY_NAMES[dayIndex];
    }

    /** Monday = 0 ... Sunday = 6. */
    static int mondayIndex(Calendar c) {
        int dow = c.get(Calendar.DAY_OF_WEEK); // Sunday = 1 ... Saturday = 7
        return (dow + 5) % 7;
    }

    /** "15:00 - 17:00" -> 900. Returns -1 when the start time cannot be read. */
    static int startMinutes(String time) {
        if (time == null) {
            return -1;
        }
        String start = time.split("[-–]")[0].trim().toLowerCase(Locale.ROOT);
        String[] parts = start.split("[:.h]");
        if (parts.length < 2) {
            return -1;
        }
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                return -1;
            }
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
