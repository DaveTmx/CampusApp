package com.example.campusapp.Data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Offline copy of one student for the roster. The server is the authority;
 * this table only serves cached results when the phone is offline.
 */
@Entity(tableName = "students",
        indices = {@Index(value = "studentNumber", unique = true)})
public class StudentEntity {

    @PrimaryKey
    @NonNull
    public String studentId;

    @NonNull
    public String studentNumber;

    @NonNull
    public String fullName;

    @NonNull
    public String programme;

    /** G01..G04, or null when Unassigned. */
    @Nullable
    public String confirmedGroup;

    public int version;

    /** When this row was last refreshed from the server (epoch millis). */
    public long cachedAt;
}
