package com.example.campusapp.viewmodel;

import com.example.campusapp.R;
import com.example.campusapp.model.Result;

/** Turns a repository outcome into the string resource the screen should show. */
final class StatusMessages {

    private StatusMessages() {
    }

    static int forStatus(Result.Status status) {
        switch (status) {
            case SUCCESS:
                return R.string.msg_saved_synced;
            case PENDING:
                return R.string.msg_saved_pending;
            case DUPLICATE_NUMBER:
                return R.string.msg_duplicate_number;
            case GROUP_FULL:
                return R.string.msg_group_full;
            case CONFLICT:
                return R.string.msg_conflict;
            case INVALID:
                return R.string.msg_server_rejected;
            case BAD_CREDENTIALS:
                return R.string.msg_login_failed;
            case NOT_FOUND:
                return R.string.msg_student_deleted;
            case NOT_ALLOWED:
                return R.string.msg_not_allowed;
            case SESSION_EXPIRED:
                return R.string.msg_session_expired;
            case OFFLINE:
                return R.string.msg_offline;
            default:
                return R.string.msg_generic_error;
        }
    }
}
