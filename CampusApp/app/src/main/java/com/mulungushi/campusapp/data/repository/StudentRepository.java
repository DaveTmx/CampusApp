package com.mulungushi.campusapp.data.repository;

import androidx.lifecycle.LiveData;

import com.mulungushi.campusapp.data.model.StudentPageResponse;

/**
 * The contract the Integration/Testing team codes against.
 * Agree on this interface with the Backend team EARLY (it mirrors the real
 * API's query params: query, page, pageSize). Once agreed, this team can
 * build the whole roster/pagination/search screen against
 * {@link FakeStudentRepository} without waiting for a single real endpoint.
 * When the real backend is ready, a RealStudentRepository implements this
 * same interface using Retrofit + Room, and the ViewModel/UI code above it
 * does not need to change at all.
 */
public interface StudentRepository {

    /**
     * Fetches one page of students, optionally filtered by name/number.
     * @param query     search text; empty string means "no filter"
     * @param page      1-based page index
     * @param pageSize  number of students per page (e.g. 20)
     * @return a LiveData that will emit exactly one {@link StudentPageResponse}
     *  once the (simulated or real) network call completes
     */
    LiveData<StudentPageResponse> getStudents(String query, int page, int pageSize);

    /** Cancels whichever getStudents() call is currently in flight, if any. */
    void cancelPendingRequest();
}