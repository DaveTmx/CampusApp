package com.example.campusapp.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The weekly timetable shown on the student Schedule tab.
 *
 * Monday matches the original design. Tuesday to Friday are SAMPLE entries —
 * replace them with your real timetable (or load them from AppData/a server).
 */
public final class Timetable {

    public static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};

    private Timetable() {}

    /** dayIndex: 0 = Mon ... 4 = Fri. */
    public static List<ClassSession> forDay(int dayIndex) {
        switch (dayIndex) {
            case 0:
                return Arrays.asList(
                        new ClassSession("Database Systems", "09:00 - 10:30", "Room 102", "Lecture"),
                        new ClassSession("Mobile Dev Lab", "11:00 - 12:30", "Lab 3", "Practical"),
                        new ClassSession("Computer Networks", "14:00 - 15:30", "Room 204", "Lecture"));
            case 1:
                return Arrays.asList(
                        new ClassSession("Software Engineering", "08:00 - 09:30", "Room 105", "Lecture"),
                        new ClassSession("Data Structures & Algorithms", "10:00 - 11:30", "Room 204", "Lecture"),
                        new ClassSession("Systems Analysis", "13:00 - 14:00", "Room 102", "Tutorial"));
            case 2:
                return Arrays.asList(
                        new ClassSession("Computer Networks Lab", "09:00 - 11:00", "Lab 2", "Practical"),
                        new ClassSession("Database Systems", "11:30 - 13:00", "Room 102", "Lecture"));
            case 3:
                return Arrays.asList(
                        new ClassSession("Mobile Development", "09:00 - 10:30", "Room 301", "Lecture"),
                        new ClassSession("Software Engineering Lab", "11:00 - 12:30", "Lab 3", "Practical"));
            case 4:
                return Arrays.asList(
                        new ClassSession("Computer Networks", "08:00 - 09:30", "Room 204", "Lecture"),
                        new ClassSession("Research Methods", "10:00 - 11:30", "Room 110", "Lecture"));
            default:
                return new ArrayList<>();
        }
    }
}
