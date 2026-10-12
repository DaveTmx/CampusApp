package com.example.campusapp.util;

import android.content.Context;

import com.example.campusapp.data.AppData;
import com.example.campusapp.data.Programs;
import com.example.campusapp.model.Session;

/**
 * Which programme's timetable the student screens show: the programme on the signed-in
 * student's profile (chosen when registering, changeable on the Profile tab).
 * Guests get Computer Science.
 */
public final class CurrentProgram {

    private CurrentProgram() {
    }

    /** CS, IT, DS or CYB. */
    public static String code(Context context) {
        Session session = ServiceLocator.authRepository(context).currentSession();
        String studentId = session != null && !session.isLecturer() ? session.accountId : null;
        return AppData.getInstance(context).programCodeFor(studentId);
    }

    /** "Information Technology" and so on. */
    public static String label(Context context) {
        return Programs.toLabel(code(context));
    }
}
