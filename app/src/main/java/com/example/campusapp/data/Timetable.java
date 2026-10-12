package com.example.campusapp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The timetables the app starts with, one per programme (3rd year, Fulltime), from the
 * Mulungushi University information system (edurole.mu.ac.zm):
 *
 *   CS  - Bachelor of Science in Computer Science (BScCS3)
 *   IT  - Bachelor of Science in Information Technology (BScIT3)
 *   DS  - Bachelor of Science in Data Science (BScDS3)
 *   CYB - Bachelor of Science in Cyber Security (BScCybS3)
 *
 * Each class is shown as "course code (lecturer)". There are no classes on Saturday.
 * A student sees the timetable of the programme on their profile.
 *
 * Lecturers can change them in the app (add, remove or import a CSV); this is only the starting copy.
 */
public final class Timetable {

    public static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};

    /** Raise this when the starting timetables below change, so installed apps pick them up. */
    public static final int VERSION = 3;

    // Venue names, written once so the same room is spelled the same way in every timetable
    // (the campus map groups classes by venue).
    public static final String OLD_DINING_B = "Old Dinning Hall B";
    public static final String NEW_DINING = "New Dinning Hall";
    public static final String CHALABESA = "Chalabesa Hall";
    public static final String MULTI_PURPOSE = "Multi Purpose Hall (next to NLT)";
    public static final String SSS_A2 = "SSS Block-A Rm 2";
    public static final String SSS_B2 = "SSS Block-B Rm 2";
    public static final String SSS_C2 = "SSS Block-C Rm 2";
    public static final String SSS_E1 = "SSS Block-E Rm 1";
    public static final String ICT_LAB_1 = "ICT Centre Lab (Computer Room 1)";
    public static final String ICT_LAB_2 = "ICT Centre Lab (Computer Room 2)";
    public static final String SOE_BOARD = "SOE Board Room";
    public static final String F57 = "F 57";
    public static final String F60 = "F 60";
    public static final String F62 = "F 62";

    private Timetable() {}

    /** The programme codes that have a starting timetable, in Programs.CODES order. */
    public static final String[] PROGRAMS = {"CS", "IT", "DS", "CYB"};

    /**
     * programme: CS, IT, DS or CYB (anything else gets an empty list).
     * dayIndex: 0 = Mon ... 4 = Fri.
     */
    public static List<ClassSession> forProgram(String programme, int dayIndex) {
        if ("IT".equals(programme)) return informationTechnology(dayIndex);
        if ("DS".equals(programme)) return dataScience(dayIndex);
        if ("CYB".equals(programme)) return cyberSecurity(dayIndex);
        if ("CS".equals(programme)) return computerScience(dayIndex);
        return new ArrayList<>();
    }

    /** The Computer Science timetable (kept for older callers). */
    public static List<ClassSession> forDay(int dayIndex) {
        return computerScience(dayIndex);
    }

    private static ClassSession c(String name, String time, String room) {
        return new ClassSession(name, time, room, "Lecture");
    }

    private static List<ClassSession> computerScience(int day) {
        switch (day) {
            case 0: return Arrays.asList(
                    c("ICT361 (Nyirenda E.)", "15:00 - 17:00", OLD_DINING_B));
            case 1: return Arrays.asList(
                    c("ICS341 (Bwalya M.)", "07:00 - 09:00", NEW_DINING));
            case 2: return Arrays.asList(
                    c("ICT351 (Nyirenda E.)", "13:00 - 15:00", SSS_C2),
                    c("ICT381 (Mumba P.)", "15:00 - 17:00", SSS_E1));
            case 3: return Arrays.asList(
                    c("ICT381 (Mumba P.)", "07:00 - 09:00", MULTI_PURPOSE));
            case 4: return Arrays.asList(
                    c("ICT361 (Nyirenda E.)", "11:00 - 13:00", SSS_B2),
                    c("ICS341 (Bwalya M.)", "13:00 - 15:00", NEW_DINING),
                    c("ICT351 (Nyirenda E.)", "15:00 - 17:00", SSS_A2));
            default: return new ArrayList<>();
        }
    }

    private static List<ClassSession> informationTechnology(int day) {
        switch (day) {
            case 0: return Arrays.asList(
                    c("BMG241 (Mwaanga C.)", "11:00 - 13:00", CHALABESA),
                    c("ICT361 (Nyirenda E.)", "15:00 - 17:00", OLD_DINING_B));
            case 1: return Arrays.asList(
                    c("ICS341 (Bwalya M.)", "07:00 - 09:00", NEW_DINING));
            case 2: return Arrays.asList(
                    c("ICT381 (Mumba P.)", "15:00 - 17:00", SSS_E1));
            case 3: return Arrays.asList(
                    c("ICT381 (Mumba P.)", "07:00 - 09:00", MULTI_PURPOSE));
            case 4: return Arrays.asList(
                    c("ICT361 (Nyirenda E.)", "11:00 - 13:00", SSS_B2),
                    c("ICS341 (Bwalya M.)", "13:00 - 15:00", NEW_DINING));
            default: return new ArrayList<>();
        }
    }

    private static List<ClassSession> dataScience(int day) {
        switch (day) {
            case 0: return Arrays.asList(
                    c("ICD341 (Hambulo J.)", "11:00 - 13:00", SOE_BOARD));
            case 1: return Arrays.asList(
                    c("ICD311 (Nyirenda E.)", "11:00 - 13:00", ICT_LAB_2),
                    c("ICD331 (Mumba P.)", "15:00 - 17:00", F60));
            case 2: return Arrays.asList(
                    c("ICD341 (Hambulo J.)", "09:00 - 11:00", F57),
                    c("ICD331 (Mumba P.)", "11:00 - 13:00", F62),
                    c("ICT381 (Mumba P.)", "15:00 - 17:00", SSS_E1));
            case 3: return Arrays.asList(
                    c("ICT381 (Mumba P.)", "07:00 - 09:00", MULTI_PURPOSE),
                    c("ICD311 (Nyirenda E.)", "11:00 - 13:00", ICT_LAB_2));
            default: return new ArrayList<>(); // no Friday classes
        }
    }

    private static List<ClassSession> cyberSecurity(int day) {
        switch (day) {
            case 0: return Arrays.asList(
                    c("ICS321 (Bwalya M.)", "11:00 - 13:00", ICT_LAB_2),
                    // The printed timetable cuts this room off at "ICT Center Lab (Computer";
                    // Room 2 is where the other ICT Centre lab classes are held.
                    c("ICS331 (Kalunga J.)", "13:00 - 15:00", ICT_LAB_2));
            case 1: return Arrays.asList(
                    c("ICS341 (Bwalya M.)", "07:00 - 09:00", NEW_DINING),
                    c("ICS371 (Kalunga J.)", "11:00 - 13:00", F62));
            case 2: return Arrays.asList(
                    c("ICS331 (Kalunga J.)", "07:00 - 09:00", F60),
                    c("ICS351 (Bwalya M.)", "09:00 - 11:00", ICT_LAB_1));
            case 3: return Arrays.asList(
                    c("ICS351 (Bwalya M.)", "09:00 - 11:00", ICT_LAB_2));
            case 4: return Arrays.asList(
                    c("ICS321 (Bwalya M.)", "11:00 - 13:00", ICT_LAB_2),
                    c("ICS341 (Bwalya M.)", "13:00 - 15:00", NEW_DINING));
            default: return new ArrayList<>();
        }
    }
}
