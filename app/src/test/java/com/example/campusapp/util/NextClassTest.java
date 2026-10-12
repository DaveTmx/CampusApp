package com.example.campusapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.campusapp.data.ClassSession;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class NextClassTest {

    /** Mon: 15:00 class. Wed: 13:00 and 15:00. Other days empty. */
    private static final NextClass.Source TIMETABLE = day -> {
        switch (day) {
            case 0:
                return Collections.singletonList(new ClassSession("ICT361", "15:00 - 17:00", "Hall B", "Lecture"));
            case 2:
                return Arrays.asList(
                        new ClassSession("ICT381", "15:00 - 17:00", "Rm 1", "Lecture"),
                        new ClassSession("ICT351", "13:00 - 15:00", "Rm 2", "Lecture"));
            default:
                return new ArrayList<ClassSession>();
        }
    };

    /** 2026-10-12 is a Monday. */
    private static Calendar at(int dayOfMonth, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.OCTOBER, dayOfMonth, hour, minute);
        return c;
    }

    @Test
    public void laterTodayIsShownWithMinutes() {
        NextClass next = NextClass.find(TIMETABLE, at(12, 14, 45));
        assertNotNull(next);
        assertEquals("ICT361", next.session.name);
        assertEquals("In 15 mins", next.whenLabel());
    }

    @Test
    public void afterTodaysLastClassMovesToNextTeachingDay() {
        NextClass next = NextClass.find(TIMETABLE, at(12, 16, 0)); // Mon class already started
        assertNotNull(next);
        assertEquals("ICT351", next.session.name); // earliest on Wednesday
        assertEquals("Wednesday", next.whenLabel());
    }

    @Test
    public void weekendLooksAheadToMonday() {
        NextClass next = NextClass.find(TIMETABLE, at(18, 10, 0)); // Sunday
        assertNotNull(next);
        assertEquals("ICT361", next.session.name);
        assertEquals("Tomorrow", next.whenLabel());
    }

    @Test
    public void sameDayNextWeekWhenOnlyOneClass() {
        NextClass.Source onlyMonday = day -> day == 0
                ? Collections.singletonList(new ClassSession("A", "09:00 - 10:00", "R", "Lecture"))
                : new ArrayList<ClassSession>();
        NextClass next = NextClass.find(onlyMonday, at(12, 11, 0));
        assertNotNull(next);
        assertEquals(7, next.daysAhead);
    }

    @Test
    public void emptyTimetableGivesNull() {
        List<ClassSession> none = new ArrayList<>();
        assertNull(NextClass.find(day -> none, at(12, 9, 0)));
    }

    @Test
    public void readsStartTimes() {
        assertEquals(15 * 60, NextClass.startMinutes("15:00 - 17:00"));
        assertEquals(7 * 60 + 30, NextClass.startMinutes("7:30-9:00"));
        assertEquals(-1, NextClass.startMinutes("TBA"));
    }

    @Test
    public void emailRule() {
        assertNull(Validation.emailError("chileshe@gmail.com"));
        assertNull(Validation.emailError("  a.b+c@mu.ac.zm "));
        assertNotNull(Validation.emailError(""));
        assertNotNull(Validation.emailError("chileshe@gmail"));
        assertNotNull(Validation.emailError("no-at-sign.com"));
    }
}
