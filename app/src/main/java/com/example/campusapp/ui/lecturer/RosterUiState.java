package com.example.campusapp.ui.lecturer;

import com.example.campusapp.model.Student;

import java.util.Collections;
import java.util.List;

/** Everything the roster screen needs to draw itself. The Activity only renders this. */
public final class RosterUiState {

    public enum Status {
        /** First page is loading. Show a spinner. */
        LOADING,
        /** Students are shown. */
        SUCCESS,
        /** Search returned no students. */
        EMPTY,
        /** No connection. Students shown are cached; say so. */
        OFFLINE,
        /** Session ended. Show the sign-in prompt. */
        SESSION_EXPIRED,
        /** Request failed. Offer retry. */
        ERROR
    }

    public final Status status;
    /** Every page loaded so far for the current query. */
    public final List<Student> students;
    /** True when another page can be requested. */
    public final boolean hasMore;
    /** True while a later page is loading. Show a footer spinner. */
    public final boolean loadingMore;
    public final String query;

    public RosterUiState(Status status, List<Student> students, boolean hasMore,
                         boolean loadingMore, String query) {
        this.status = status;
        this.students = students == null ? Collections.emptyList() : students;
        this.hasMore = hasMore;
        this.loadingMore = loadingMore;
        this.query = query;
    }
}
