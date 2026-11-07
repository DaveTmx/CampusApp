package com.example.campusapp.data.remote;

/**
 * Request and response bodies for /api/auth. Field names match the backend's JSON exactly,
 * so Gson can map them without annotations.
 */
public final class AuthDto {

    private AuthDto() {
    }

    /** Body for /staff/login. Lecturers always sign in with their email address. */
    public static final class LoginRequest {
        public final String email;
        public final String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    /** Body for /login. The identifier is the email address or the 9-digit student number. */
    public static final class StudentLoginRequest {
        public final String identifier;
        public final String password;

        public StudentLoginRequest(String identifier, String password) {
            this.identifier = identifier;
            this.password = password;
        }
    }

    /** Body for /staff/verify-code. */
    public static final class VerifyCodeRequest {
        public final String challengeId;
        public final String code;

        public VerifyCodeRequest(String challengeId, String code) {
            this.challengeId = challengeId;
            this.code = code;
        }
    }

    /** Body for /register. The server stores the email address and student number as typed (trimmed). */
    public static final class RegisterRequest {
        public final String fullName;
        public final String email;
        public final String password;
        public final String studentNumber;
        public final String programmeCode;

        public RegisterRequest(String fullName, String email, String password,
                               String studentNumber, String programmeCode) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.studentNumber = studentNumber;
            this.programmeCode = programmeCode;
        }
    }

    /** Body for /forgot-password. */
    public static final class EmailRequest {
        public final String email;

        public EmailRequest(String email) {
            this.email = email;
        }
    }

    /** Reply to /staff/login: the password was right and a code has been emailed. */
    public static final class ChallengeResponse {
        public String challengeId;
        public int expiresInSeconds;
    }

    /** The signed-in user, as the server describes them. */
    public static final class User {
        public String id;
        public String fullName;
        public String email;
        /** "student" or "lecturer", in lower case. */
        public String role;
        public String studentId;
    }

    /** Reply to a successful /login or /staff/verify-code. */
    public static final class LoginResponse {
        public String token;
        public int expiresIn;
        public User user;
    }

    /** Every error reply has this shape. "error" is the stable code the app switches on. */
    public static final class ErrorBody {
        public String error;
        public String message;
        public Integer retryAfterSeconds;
        public Integer attemptsLeft;
    }
}
