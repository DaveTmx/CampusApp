package com.example.campusapp.model;

import java.util.List;

/** One page of the lecturer roster. */
public class StudentPage {

    public final List<Student> students;
    /** True when there is another page to load. */
    public final boolean hasMore;
    /** True when the phone is offline and these are limited cached results. */
    public final boolean fromCache;

    public StudentPage(List<Student> students, boolean hasMore, boolean fromCache) {
        this.students = students;
        this.hasMore = hasMore;
        this.fromCache = fromCache;
    }
}
