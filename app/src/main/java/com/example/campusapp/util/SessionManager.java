package com.example.campusapp.util;

import com.example.campusapp.model.Session;

/** Holds the currently signed-in Session. */
public class SessionManager {

    private static Session current;

    public static void signIn(Session session) { current = session; }

    public static void signOut() { current = null; }

    public static Session getCurrent() { return current; }

    public static boolean isSignedIn() { return current != null; }
}