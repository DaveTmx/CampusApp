package com.example.campusapp.Data.remote;

import java.util.List;

/**
 * JSON shape returned by GET /api/students. Field names match the API
 * contract (snake_case) so Gson maps them without annotations.
 */
public class StudentPageDto {

    public List<StudentDto> students;
    public int page;
    public boolean hasMore;

    public static class StudentDto {
        public String student_id;
        public String student_number;
        public String full_name;
        public String programme;
        /** G01..G04, or null when the student is Unassigned. */
        public String confirmed_group;
        public int version;
    }
}
