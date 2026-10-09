package com.example.campusapp.model;

/** How many active students a lab group currently holds. */
public class GroupCount {

    public static final int CAPACITY = 15;

    public final String group;
    public final int count;

    public GroupCount(String group, int count) {
        this.group = group;
        this.count = count;
    }

    public boolean isFull() {
        return count >= CAPACITY;
    }
}
