package com.mulungushi.campuscompanion.data;

/** The programs a student can be enrolled in. Shared by the registration and lecturer edit screens. */
public final class Programs {
    public static final String COMPUTER_SCIENCE = "Computer Science";
    public static final String INFORMATION_TECHNOLOGY = "Information Technology";
    public static final String CYBER_SECURITY = "Cyber Security";

    public static final String[] ALL = {
            COMPUTER_SCIENCE, INFORMATION_TECHNOLOGY, CYBER_SECURITY
    };

    /** Used for records saved before programs existed. */
    public static final String DEFAULT = COMPUTER_SCIENCE;

    private Programs() {}
}
