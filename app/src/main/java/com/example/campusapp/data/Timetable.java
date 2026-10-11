package com.example.campusapp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The timetable the app starts with: Bachelor of Science in Computer Science (BScCS3),
 * Fulltime, from the Mulungushi University information system (edurole.mu.ac.zm).
 * Each class is shown as "course code (lecturer)". There are no classes on Saturday.
 *
 * Lecturers can change it in the app (add, remove or import a CSV); this is only the starting copy.
 */
public final class Timetable {

    public static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};

    /** Raise this when the starting timetable above changes, so installed apps pick it up. */
    public static final int VERSION = 2;

    private Timetable() {}

    /** dayIndex: 0 = Mon ... 4 = Fri. */
    public static List<ClassSession> forDay(int dayIndex) {
        switch (dayIndex) {
            case 0:
                return Arrays.asList(
                        new ClassSession("ICT361 (Nyirenda E.)", "15:00 - 17:00", "Old Dinning Hall B", "Lecture"));
            case 1:
                return Arrays.asList(
                        new ClassSession("ICS341 (Bwalya M.)", "07:00 - 09:00", "New Dinning Hall", "Lecture"));
            case 2:
                return Arrays.asList(
                        new ClassSession("ICT351 (Nyirenda E.)", "13:00 - 15:00", "SSS Block-C Rm 2", "Lecture"),
                        new ClassSession("ICT381 (Mumba P.)", "15:00 - 17:00", "SSS Block-E Rm 1", "Lecture"));
            case 3:
                return Arrays.asList(
                        new ClassSession("ICT381 (Mumba P.)", "07:00 - 09:00", "Multi Purpose Hall (next to NLT)", "Lecture"));
            case 4:
                return Arrays.asList(
                        new ClassSession("ICT361 (Nyirenda E.)", "11:00 - 13:00", "SSS Block-B Rm 2", "Lecture"),
                        new ClassSession("ICS341 (Bwalya M.)", "13:00 - 15:00", "New Dinning Hall", "Lecture"),
                        new ClassSession("ICT351 (Nyirenda E.)", "15:00 - 17:00", "SSS Block-A Rm 2", "Lecture"));
            default:
                return new ArrayList<>();
        }
    }
}
