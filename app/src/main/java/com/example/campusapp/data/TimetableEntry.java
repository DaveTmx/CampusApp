package com.example.campusapp.data;

/** A class in the stored timetable: a ClassSession plus its day and a stable id. */
public class TimetableEntry extends ClassSession {
    public final long id;
    /** 0 = Mon ... 4 = Fri, the same as Timetable.DAYS. */
    public final int day;

    public TimetableEntry(long id, int day, String name, String time, String room, String type) {
        super(name, time, room, type);
        this.id = id;
        this.day = day;
    }
}
