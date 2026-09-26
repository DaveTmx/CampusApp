package com.mulungushi.campusapp.data.model;

import java.util.Collections;
import java.util.List;

/**
 * One page of student results, tagged with the requestId that produced it.
 * The requestId is the key piece for Activity D's "cancel stale search
 * responses" requirement: the ViewModel remembers the id of the *latest*
 * request it made, and only applies a response to the UI if its id still
 * matches. A slow, older response arriving late is simply dropped.
 */
public class StudentPageResponse {

    private final long requestId;
    private final List<Student> students;
    private final int page;
    private final boolean hasMore;
    private final boolean isError;
    private final String errorMessage;

    public StudentPageResponse(long requestId, List<Student> students, int page,
                               boolean hasMore, boolean isError, String errorMessage) {
        this.requestId = requestId;
        this.students = students == null ? Collections.emptyList() : students;
        this.page = page;
        this.hasMore = hasMore;
        this.isError = isError;
        this.errorMessage = errorMessage;
    }

    public long getRequestId() { return requestId; }
    public List<Student> getStudents() { return students; }
    public int getPage() { return page; }
    public boolean hasMore() { return hasMore; }
    public boolean isError() { return isError; }
    public String getErrorMessage() { return errorMessage; }
}