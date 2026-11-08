# Campus Connect API contract

Base URL: `APP_URL` (for example `https://api.example.edu`). Every route is under `/api`.
All request and response bodies are JSON. The server is stateless; send the token as
`Authorization: Bearer <token>`.

## Conventions

- **Errors** always have the shape `{ "success": false, "error": "CODE", "message": "..." }`.
  Clients switch on `error`, never on `message`.
- **Roles**: `student` and `lecturer`. Lecturers can do everything a student can, plus the
  lecturer-only routes below. Only `scripts/create-lecturer.js` creates lecturers.
- **Token**: HS256 JWT, valid 12 hours, payload `{ id, email, role, studentId }`. `id` is the account id.
- **Field naming**: `GET /api/students` uses snake_case, matching the Android `StudentPageDto`.
  Other single-record responses use camelCase, including `currentRecord` in 409 replies.

## Auth

| Method | Path | Auth | Body / query | Success |
|---|---|---|---|---|
| POST | `/api/auth/register` | none | `{ fullName, email, password, studentNumber, programmeCode }` | 201 `{ status: "PENDING_VERIFICATION" }` |
| GET | `/api/auth/verify` | none | `?token=` (from the email) | 200 |
| POST | `/api/auth/resend-verification` | none | `{ email }` | 200, same wording whether or not the account exists |
| POST | `/api/auth/login` | none | `{ identifier, password }`, where `identifier` is the email address or the 9-digit student number. The older field name `email` is still accepted | 200 `{ token, expiresIn, user: { id, fullName, email, role, studentId } }`. Students only; a lecturer gets 403 `STAFF_SIGN_IN_REQUIRED`. Both identifiers share one lockout count |
| POST | `/api/auth/staff/login` | none | `{ email, password }` | 202 `{ challengeId, expiresInSeconds }` and a 6-digit code is emailed. Lecturers only |
| POST | `/api/auth/staff/verify-code` | none | `{ challengeId, code }` | 200 `{ token, expiresIn, user }` with `role: "lecturer"`, `studentId: null` |
| POST | `/api/auth/forgot-password` | none | `{ email }` | 200 with the same message whether or not the account exists. A single-use link (30 minutes) is emailed only to an active account. Each new request burns the older links |
| GET | `/api/auth/reset-password?token=...` | none | none | 200 HTML page with a form that posts to the endpoint below. Not used by the app |
| POST | `/api/auth/reset-password` | none | `{ token, password }` | 200 `{ success, message }`. 400 `INVALID_TOKEN` for a used, expired or unknown link; 400 `VALIDATION_FAILED` for a password of 8 to 72 bytes. Success ends every existing session |
| POST | `/api/auth/logout` | token | none | 200. Stateless: the client deletes its token |

Rules:
- `password` is 8 to 72 bytes.
- `studentNumber` is exactly 9 digits. `programmeCode` is `CS`, `IT`, `DS` or `CYB`.
- Verification links are single-use and valid for 24 hours. A new link invalidates older ones.
- Lecturer sign-in is two steps. The code is 6 digits, valid for 10 minutes, and burned after 5 wrong guesses. A new code can be requested after 60 seconds (429 `CODE_ALREADY_SENT`, with `retryAfterSeconds`).
- Wrong passwords and wrong codes share one lockout: 5 failures lock the account for 15 minutes (423 `ACCOUNT_LOCKED`).
- Errors from the staff flow: 401 `CODE_INCORRECT` (with `attemptsLeft`), 401 `CODE_INVALID` (expired, used or burned), 503 `CODE_NOT_SENT` (email failed).
- Resend is limited to one link per account per 60 seconds.
- Five wrong passwords lock the account for 15 minutes (423 `ACCOUNT_LOCKED`).

Login errors:

| HTTP | error | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Email or password missing |
| 401 | `INVALID_CREDENTIALS` | Wrong email or password (same reply for both) |
| 403 | `EMAIL_NOT_VERIFIED` | Correct password, but the link has not been opened yet |
| 423 | `ACCOUNT_LOCKED` | Too many failed attempts |

Register errors: 400 `VALIDATION_FAILED` (with `fields`), 409 `DUPLICATE_EMAIL`, 409 `DUPLICATE_NUMBER`.

## Students

| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/api/students` | lecturer | Query: `q`, `programme`, `group`, `unassigned=true`, `page` (default 1), `pageSize` (1-50, default 20). Response `{ students: [{ student_id, student_number, full_name, programme, confirmed_group, version }], page, pageSize, hasMore }` |
| POST | `/api/students` | lecturer | `{ studentName, studentNumber, programmeCode, groupCode? }`. 201. 409 `DUPLICATE_NUMBER`, or `GROUP_FULL` (rolls back, no student created) |
| GET | `/api/students/:id` | lecturer, or student for own record | 200 `{ student }`. 403 `NOT_OWNER`, 404, 410 `STUDENT_DELETED` |
| PUT | `/api/students/:id` | lecturer, or student for own record | `{ baseVersion, studentName?, programmeCode?, studentNumber? }`. Lecturers may change all three; `studentNumber` must be exactly 9 digits and unique. Students may change `studentName` only. 409 `CONFLICT` with `currentRecord`. 410 `STUDENT_DELETED`. 403 `FIELD_NOT_ALLOWED` / `NOT_OWNER` |
| DELETE | `/api/students/:id` | lecturer | Soft delete, idempotent. 200 `{ releasedGroup }` or `{ alreadyDeleted: true }` |
| POST | `/api/students/:id/unassign` | lecturer | Removes the student from their group. Idempotent. 200 `{ success, groupCode: null }`. 404 `STUDENT_NOT_FOUND`, 410 `STUDENT_DELETED` |

Concurrency: `baseVersion` must equal the record's current `version`. Every successful
change increments `version`. Group capacity (16) is enforced under a row lock, so two
concurrent assignments cannot overfill a group.

## Groups

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/api/groups` | lecturer | `{ name, description?, courseCode? }` creates a campus study group (`study_groups`). 201 `{ groupId }` |

## Operations (idempotent sync)

`POST /api/operations` (lecturer)

```json
{ "operationId": "3f0c...-uuid", "opType": "ASSIGN_GROUP", "payload": { "studentId": "...", "groupCode": "G02" } }
```

`opType` is `ASSIGN_GROUP` (payload `studentId`, `groupCode`) or `DELETE_STUDENT` (payload `studentId`).

- A first request runs the change and stores a receipt. The HTTP status matches the outcome: 200 applied, 409 `GROUP_FULL`, 404 `STUDENT_NOT_FOUND`.
- A retry with the same `operationId` and the same payload replays the stored result and status, with `"replayed": true`. The change is not applied again.
- The same `operationId` with a different payload gets 409 `OPERATION_ID_REUSED_WITH_DIFFERENT_CONTENT`.
- Clients generate a new UUID per user action and keep it until the server confirms it.

## Health

`GET /api/health` returns 200 when the database answers, and 500 otherwise.

## Not built yet

- Refresh tokens, or server-side logout. Tokens expire after 12 hours.
- Rate limiting by IP. Resend has a per-account cooldown only.
- Lecturer password reset.
- The Android app still signs in against local storage (`LocalAuthRepository`). It must be switched to `/api/auth` before these endpoints protect real data.
