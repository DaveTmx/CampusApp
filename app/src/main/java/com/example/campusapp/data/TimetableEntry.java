package com.example.campusapp.data;

/** A class in the stored timetable: a ClassSession plus its programme, day and a stable id. */
public class TimetableEntry extends ClassSession {
    public final long id;
    /** 0 = Mon ... 4 = Fri, the same as Timetable.DAYS. */
    public final int day;
    /** Programme code (CS, IT, DS or CYB) whose timetable this class belongs to. */
    public final String program;

    public TimetableEntry(long id, String program, int day, String name, String time, String room, String type) {
        super(name, time, room, type);
        this.id = id;
        this.program = program;
        this.day = day;
    }

    /** A class with no programme yet, e.g. a CSV row before the lecturer says which programme it is for. */
    public TimetableEntry(long id, int day, String name, String time, String room, String type) {
        this(id, "", day, name, time, room, type);
    }
}
