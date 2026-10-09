package com.example.campusapp.model;

/** Who is signed in. The password is never part of this. */
public class Session {

    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_LECTURER = "LECTURER";

    public final String accountId;
    public final String role;

    public Session(String accountId, String role) {
        this.accountId = accountId;
        this.role = role;
    }

    public boolean isLecturer() {
        return ROLE_LECTURER.equals(role);
    }
}
