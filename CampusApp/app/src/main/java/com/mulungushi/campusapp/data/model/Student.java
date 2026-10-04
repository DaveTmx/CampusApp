package com.mulungushi.campusapp.data.model;

/**
 * Plain data holder for one student row. Same shape whether it came from
 * the fake repository or the real Retrofit/Room-backed one later.
 */
public class Student {

    private final String studentId;      // immutable server-assigned id
    private final String studentNumber;  // 9-digit string, e.g. "000123456"
    private final String fullName;
    private final String programme;      // "CS", "IT", or "DS"
    private final String labGroup;       // "G01"..."G04", or "UNASSIGNED"

    public Student(String studentId, String studentNumber, String fullName,
                   String programme, String labGroup) {
        this.studentId = studentId;
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.programme = programme;
        this.labGroup = labGroup;
    }

    public String getStudentId() { return studentId; }
    public String getStudentNumber() { return studentNumber; }
    public String getFullName() { return fullName; }
    public String getProgramme() { return programme; }
    public String getLabGroup() { return labGroup; }
}
