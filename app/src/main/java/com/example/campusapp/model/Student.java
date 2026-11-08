package com.example.campusapp.model;

/**
 * One student as the screens see it. Plain fields keep it easy to read;
 * the storage team can map this to and from their Room entity.
 */
public class Student {

    public static final String SAVED_LOCALLY = "SAVED_LOCALLY";
    public static final String PENDING = "PENDING";
    public static final String SYNCING = "SYNCING";
    public static final String SYNCED = "SYNCED";
    public static final String ACTION_REQUIRED = "ACTION_REQUIRED";

    /** Immutable ID. It is NOT the student number, so the number can be corrected later. */
    public String studentId;
    public String name;
    /** A String, never an int, so leading zeroes such as 012345678 are kept. */
    public String studentNumber;
    /** CS, IT or DS. */
    public String programme;
    /** Group confirmed by the server: G01..G04, or null for Unassigned. */
    public String confirmedGroup;
    /** Group that was requested but is not confirmed yet, or null. It does not reserve a place. */
    public String pendingGroup;
    /** One of the constants above. */
    public String syncStatus;
    /** Record version from the server, used to detect conflicting edits. */
    public int version;
}
