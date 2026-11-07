# Test checklist

Status key: **Written** = the test file exists in the project. **Run** = it has passed in a real
environment. Items marked "not run" are waiting for the MySQL database or the Android SDK.

## 1. Backend (Node.js, Jest, MySQL)

Run with `npm test` against a database built from `database/schema.sql` and
`database/seed-test-fixture.sql`.

| Suite | Covers | Written | Run |
|---|---|---|---|
| `backend/tests/concurrency.test.js` | 20 races for the last place in G01; capacity holds | Yes | Not run |
| `backend/tests/conflict.test.js` | Stale edit gets 409 `CONFLICT`; full group gets 409 `GROUP_FULL`; deleted student gets 410; repeat delete is idempotent | Yes | Not run |
| `backend/tests/idempotency.test.js` | Same `operationId` replays once; different content gets 409 | Yes | Not run |
| `backend/tests/auth.test.js` | Register, verify once, login refused before verification, lockout after 5 failures, resend cooldown, lecturer protection, student ownership, JSON errors | Yes | Not run |
| `backend/tests/auth.test.js` (lecturer section) | Lecturer refused by `/login`; password then emailed code; correct code once; wrong code counts down; 5 wrong codes burn the challenge; 60 s resend cooldown; students refused by staff sign-in; lockout from wrong passwords and from wrong codes | Yes | Not run |

Before the first run, check:

- [ ] `JWT_SECRET` is set in `.env` (at least 32 characters). Tests use a built-in test secret when `NODE_ENV=test`.
- [ ] The schema loads without error on a fresh database.
- [ ] The fixture seeds all four programmes, including `CYB`.
- [ ] Student numbers in the fixture are all 9 digits.

Server checks that do not need the database:

- [x] Every backend file passes `node --check`.
- [x] The app loads and registers all 14 routes.
- [x] The server refuses to start without `JWT_SECRET` outside test mode.
- [x] The schema and fixture parse as MySQL.

## 2. Android unit tests (JVM, no device)

Run with `./gradlew testDebugUnitTest`.

| Test | Covers | Written | Run |
|---|---|---|---|
| `NextClassTest` | Next-class calculation | Yes | Not run |
| `LecturerRosterViewModelTest` | Loading, empty, offline, expired session, stale-response guard, search cancellation, debounce, paging | Yes | Not run |
| `RealStudentRepositoryTest` | Mapping of the list response; 401, 403 and offline handling (MockWebServer) | Yes | Not run |
| `ValidationTest` | 9-digit student numbers (rejects 8, 10, letters, inner spaces, other-script digits); the four programme codes; groups; password length | Yes | **Passed, 22 tests with `ProgramsTest`** (plain JUnit 4, run outside Gradle with a stub `R` class) |
| `ProgramsTest` | Labels and codes round-trip for all four programmes, including `CYB` / "Cyber Security" | Yes | **Passed** (same run) |

Not yet written:

- [ ] `LocalStudentRepository`: `register`, `createStudent` and `updateStudent` return `INVALID` for a bad student number.
- [ ] `StoredStudent` persistence through `AppData`. Needs a real `org.json` (the compile stub does not parse), so it has to run under Gradle.
- [ ] Room migration test. `exportSchema` is on, but the first `app/schemas/1.json` is only produced by a Gradle build, and the test waits on version 2.
- [ ] `LocalStudentRepository`: `register`, `createStudent` and `updateStudent` return `INVALID` for a bad student number.
- [ ] `StoredStudent` persistence through `AppData` (save and reload keeps the programme label).
- [ ] Room migration test, once `exportSchema` is turned on.

## 3. Data layer compile check (no Android SDK needed)

- [x] `data/*.java` and `model/*.java` compile against compile-only stubs for `android.content` and `org.json`. Script: `compile-data-layer.sh` in the scratchpad.
- [ ] The Android-dependent packages (`ui`, `viewmodel`, `auth`, `student`, `lecturer`) still need a full Gradle build.

## 4. Manual checks on a device or emulator

Not yet run.

- [ ] Register a student with an 8-digit number: the form shows the error and does not save.
- [ ] Register a student with a Cyber Security programme: the choice is saved and shown on the profile.
- [ ] Lecturer roster shows a Cyber Security student (seed data).
- [ ] Lecturer edit: changing a student's number to one already in use shows "already exists".
- [ ] Student profile: a number correction request is queued for the lecturer, not applied.
- [ ] Offline roster: last cached results are shown and labelled offline.
- [ ] Session expiry from the server: the app asks the user to sign in again.

## 5. End-to-end (once auth is switched to the API)

- [ ] Register, receive the email link, open it, then log in.
- [ ] Lecturer account created by the script can log in after verification.
- [ ] Two devices edit the same student: the second gets the conflict screen.
- [ ] Offline assignment queued, then sent after reconnecting, and not applied twice.
