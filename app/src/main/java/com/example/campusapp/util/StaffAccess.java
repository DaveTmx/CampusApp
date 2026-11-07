package com.example.campusapp.util;

import com.example.campusapp.BuildConfig;

import java.util.Locale;

/**
 * Who may set up a lecturer account. Only addresses on this list can create one, and creating
 * it also needs a code emailed to that address, so a stranger cannot claim a staff account.
 *
 * The release list is supplied by the university IT department before the app is issued. It is
 * empty until then, which means nobody can set up a lecturer account in a release build.
 */
public final class StaffAccess {

    /** Release list: lower-case staff email addresses approved by IT. */
    private static final String[] APPROVED_STAFF_EMAILS = {};

    /** The demo lecturer. Counted as approved in debug builds only. */
    private static final String[] DEBUG_DEMO_STAFF = {"muleba@gmail.com"};

    private StaffAccess() {
    }

    public static boolean isApproved(String email) {
        String key = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return false;
        }
        for (String approved : APPROVED_STAFF_EMAILS) {
            if (approved.equals(key)) {
                return true;
            }
        }
        if (BuildConfig.DEBUG) {
            for (String demo : DEBUG_DEMO_STAFF) {
                if (demo.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }
}
