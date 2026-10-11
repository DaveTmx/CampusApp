package com.example.campusapp.util;

import com.example.campusapp.model.Result;

public final class StatusMessages {

    private StatusMessages() { }

    public static String forStatus(Result.Status status) {
        if (status == null) {
            return "Something went wrong. Please try again.";
        }
        switch (status) {
            case SUCCESS:          return "Saved.";
            case PENDING:          return "Saved on this phone. It will sync when you are online.";
            case DUPLICATE_NUMBER: return "Another student already has this student number.";
            case GROUP_FULL:       return "This group already has 15 active students.";
            case CONFLICT:         return "Someone else changed this record first.";
            case INVALID:          return "Please check the highlighted fields.";
            case BAD_CREDENTIALS:  return "Wrong username, password or claim code.";
            case NOT_FOUND:        return "This student no longer exists.";
            case NOT_ALLOWED:      return "You are not allowed to do this.";
            case SESSION_EXPIRED:  return "Your session has ended. Please sign in again.";
            case OFFLINE:          return "No connection. This action cannot be saved offline.";
            default:               return "Something went wrong. Please try again.";
        }
    }
}