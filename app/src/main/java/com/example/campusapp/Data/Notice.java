package com.example.campusapp.data;

/** A bulletin posted by a lecturer/admin and shown on both the student and lecturer Notices screens. */
public class Notice {
    public long id;
    public String title;
    public String body;
    public String category; // "Academic", "Admin", or "Urgent"
    public boolean isNew;

    public Notice(long id, String title, String body, String category, boolean isNew) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.category = category;
        this.isNew = isNew;
    }
}
