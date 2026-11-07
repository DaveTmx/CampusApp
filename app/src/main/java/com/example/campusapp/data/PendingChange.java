package com.example.campusapp.data;

/**
 * A student-submitted request (e.g. "change my group", "correct my number")
 * that is queued until a lecturer approves or rejects it.
 */
public class PendingChange {
    public long id;
    public String studentId;    // Student.id this request applies to (stable, not the editable number)
    public String studentName;  // snapshot, for display in the lecturer's queue
    public String field;        // "name", "group", or "number"
    public String fieldLabel;   // "Name", "Group", or "Student Number" — shown to the lecturer
    public String newValue;
    public String status;       // "Pending", "Approved", "Rejected"

    public PendingChange(long id, String studentId, String studentName,
                          String field, String fieldLabel, String newValue, String status) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.field = field;
        this.fieldLabel = fieldLabel;
        this.newValue = newValue;
        this.status = status;
    }
}
