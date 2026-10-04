package com.mulungushi.campuscompanion.data;

/** One timetable entry: a class on a given day. */
public class ClassSession {
    public final String name;
    public final String time;   // e.g. "09:00 - 10:30"
    public final String room;   // e.g. "Room 102"
    public final String type;   // "Lecture", "Practical" or "Tutorial"

    public ClassSession(String name, String time, String room, String type) {
        this.name = name;
        this.time = time;
        this.room = room;
        this.type = type;
    }
}
