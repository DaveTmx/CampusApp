package com.example.campusapp.data;

/**
 * The programs a student can be enrolled in. Shared by the registration and lecturer edit screens.
 * The screens show the full name; the ViewModels and the rules in Validation use the short code.
 */
public final class Programs {
    public static final String COMPUTER_SCIENCE = "Computer Science";
    public static final String INFORMATION_TECHNOLOGY = "Information Technology";
    public static final String DATA_SCIENCE = "Data Science";
    public static final String CYBER_SECURITY = "Cyber Security";

    public static final String[] ALL = {
            COMPUTER_SCIENCE, INFORMATION_TECHNOLOGY, DATA_SCIENCE, CYBER_SECURITY
    };

    /** Same order as ALL: CS, IT, DS, CYB. */
    public static final String[] CODES = {"CS", "IT", "DS", "CYB"};

    /** Used for records saved before programs existed. */
    public static final String DEFAULT = COMPUTER_SCIENCE;

    private Programs() {}

    /** "Computer Science" -> "CS". An unknown name is returned unchanged so it is never lost. */
    public static String toCode(String label) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].equals(label)) return CODES[i];
        }
        return label;
    }

    /** "CS" -> "Computer Science". An unknown code is returned unchanged. */
    public static String toLabel(String code) {
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(code)) return ALL[i];
        }
        return code;
    }
}
