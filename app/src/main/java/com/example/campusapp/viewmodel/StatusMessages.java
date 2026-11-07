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
            case DUPLICATE_EMAIL:
                return R.string.error_email_taken;
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
            case LOCKED_OUT:
                return R.string.msg_locked_out;
            case NOT_VERIFIED:
                return R.string.msg_not_verified;
            case USE_STAFF_SIGN_IN:
                return R.string.msg_use_staff_sign_in;
            case WRONG_CODE:
                return R.string.msg_wrong_code;
            case CODE_EXPIRED:
                return R.string.msg_code_expired;
            case CODE_TOO_SOON:
                return R.string.msg_code_too_soon;
            case CODE_NOT_SENT:
                return R.string.msg_code_not_sent;
            case RATE_LIMITED:
                return R.string.msg_rate_limited;
            case CHECK_EMAIL:
                return R.string.msg_check_email;
            default:
                return R.string.msg_generic_error;
        }
    }
}
