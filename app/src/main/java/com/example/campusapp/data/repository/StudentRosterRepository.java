package com.example.campusapp.Data.repository;

import com.example.campusapp.model.Result;
import com.example.campusapp.model.StudentPage;

/**
 * What the lecturer roster needs from data. Each request carries an id so the
 * ViewModel can cancel a superseded request and ignore any late reply.
 * Callbacks are always delivered on the main thread.
 */
public interface StudentRosterRepository {

    /** Starts one-page request. The result arrives through the callback. */
    void loadPage(long requestId, String query, int page, int pageSize, Callback callback);

    /** Cancels a request still in flight. Does nothing if it already finished. */
    void cancel(long requestId);

    interface Callback {
        void onResult(long requestId, Result<StudentPage> result);
    }
}
