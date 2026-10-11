package com.example.campusapp.util;

import com.example.campusapp.R;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The field rules from the lab manual, in one place so the registration form
 * and the lecturer editor cannot disagree. Each check returns a string
 * resource ID for the error, or null when the value is acceptable.
 * The server must still repeat these checks.
 */
public final class Validation {

    public static final List<String> PROGRAMMES = Arrays.asList("CS", "IT", "DS");
    public static final List<String> GROUPS = Arrays.asList("G01", "G02", "G03", "G04");
    public static final int MIN_PASSWORD_LENGTH = 8;

    // Starts with a letter; then letters (including accented ones), spaces,
    // full stops, apostrophes and hyphens.
    private static final Pattern NAME =
            Pattern.compile("[\\p{L}\\p{M}][\\p{L}\\p{M} .'’-]*");

    // [0-9] and not \d: on Android \d also accepts digits from other scripts.
    private static final Pattern NUMBER = Pattern.compile("[0-9]{9}");

    private Validation() {
    }

    /** Null-safe trim of the outer whitespace. */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static Integer nameError(String name) {
        String n = clean(name);
        if (n.length() < 2 || n.length() > 100 || !NAME.matcher(n).matches()) {
            return R.string.error_name_invalid;
        }
        return null;
    }

    /** Exactly nine digits after trimming; a space in the middle is rejected. */
    public static Integer numberError(String number) {
        if (!NUMBER.matcher(clean(number)).matches()) {
            return R.string.error_number_invalid;
        }
        return null;
    }

    public static Integer programmeError(String programme) {
        return PROGRAMMES.contains(programme) ? null : R.string.error_programme_required;
    }

    /** null or "" means Unassigned, which is allowed. */
    public static Integer groupError(String group) {
        if (group == null || group.isEmpty() || GROUPS.contains(group)) {
            return null;
        }
        return R.string.error_group_invalid;
    }

    public static Integer passwordError(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return R.string.error_password_short;
        }
        return null;
    }
}
