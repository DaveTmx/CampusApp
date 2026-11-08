package com.example.campusapp.data.model;

/** How many active students a lab group currently holds. */
public class GroupCount {

    public static final int CAPACITY = 16;

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
