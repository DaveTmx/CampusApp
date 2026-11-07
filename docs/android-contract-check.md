# Android ↔ API contract check

Checked against `docs/api-contract.md` and the current Android code. Date: 2026-10-12.

## What the app actually calls today

| Android code | Endpoint | Status |
|---|---|---|
| `ApiService.getStudents(q, page, pageSize)` via `RealStudentRepository.loadPage` | `GET /api/students` | Matches the server |

Nothing else calls the server yet. Sign-in, registration, edits, group assignment, deletes and
operations all use the local store (`LocalAuthRepository`, `LocalStudentRepository`, `AppData`).
So there are no other live mismatches to find. The rest of this table lists what must be checked
when each call is switched over.

## `GET /api/students` details

| Check | Result |
|---|---|
| Query names `q`, `page`, `pageSize` | Match the server's parameters |
| Paging starts at 1 | Yes (`LecturerRosterViewModel.nextPage = 1`); the server rejects `page < 1` |
| Page size | 20 from the app; the server allows 1–50 |
| Empty search | The app sends `q=""`; the server treats a blank `q` as no filter |
| Response fields | `students`, `page`, `hasMore` match `StudentPageDto` |
| Student fields | `student_id`, `student_number`, `full_name`, `programme`, `confirmed_group`, `version` match `StudentDto` |
| `programme` value | A code (`CS`, `IT`, `DS`, `CYB`) on both sides |
| `confirmed_group` | `null` for unassigned on both sides |
| Auth | Bearer token from the auth interceptor; 401 maps to `SESSION_EXPIRED`, 403 to `NOT_ALLOWED` |
| Failures | Non-2xx and malformed bodies map to `ERROR`; no connection maps to `OFFLINE` from the cache |

## Things to check when each call is switched over

These are the places where the two sides are most likely to disagree.

1. **Login and register.** The app's `Result.Status` values (`BAD_CREDENTIALS`, `NEEDS_CODE`,
   `LOCKED_OUT`) do not match the server's error codes (`INVALID_CREDENTIALS`,
   `EMAIL_NOT_VERIFIED`, `ACCOUNT_LOCKED`). The mapping layer has to be written explicitly.
   The app's staff OTP step has no server equivalent; the server only does email verification.
2. **Field naming.** The server uses `studentNumber`, `programmeCode` and `baseVersion` in request
   bodies. The list endpoint uses snake_case, and single records use camelCase. The DTOs must
   follow the same split.
3. **Edits.** The app's `updateStudent` sends the full record. The server needs `baseVersion`
   and accepts only the fields listed in the contract. Student-role edits may change `studentName`
   only, so the app must not send a programme change from a student account.
4. **Group assignment and deletes.** These must go through `POST /api/operations` with an
   `operationId`, not directly, so retries are safe.
5. **Student numbers.** The app enforces 9 digits (`Validation.numberError`). The server
   enforces the same rule on register, create and edit. Keep the two in step if the rule changes.
6. **Programme codes.** The app's `Programs` class uses the codes `CS`, `IT`, `DS`, `CYB`.
   The server's `PROGRAMMES` list matches. Labels (for example "Cyber Security") are for display
   only and must never be sent to the server.
7. **Token storage.** The app must keep the JWT somewhere private and attach it to every request.
   The server's logout is client-side only, so the app must delete the token on logout.

## Known gaps on the server side

- `POST /api/groups` and the operations endpoint return the same error shape, but the app has no
  code that reads the `status` field of a replayed operation yet.
- The lab-group and campus-group features use different tables (`lab_groups` and `study_groups`).
  The app's group screens must say which one they mean.
