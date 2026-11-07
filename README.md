# CampusApp – Android Student Registration & Lab Group Management System
ICT361 Mobile Application Development | Group Lab Project

CampusApp is a complete Android mobile application and Node.js/Express/MySQL backend developed for **ICT361 Mobile Application Development** at Mulungushi University.

---

## Key Features

### 1. Student Features
- **Registration & Verification**: Register with Full Name, 9-digit Student Number, Programme of Study (CS, IT, DS, CYB), and Lab Group (G01–G04).
- **Email OTP Verification**: Offline draft saving with mandatory online email OTP verification before account activation.
- **Profile & Occupancy**: View personal profile, assigned lab group, and real-time group capacity (out of 15 members).
- **Edit & Requests**: Edit permitted details (Name, Programme) and submit requests for lab group transfers or student number corrections.

### 2. Lecturer Features
- **Staff Access Protection**: Staff setup requiring pre-approved staff emails (`StaffAccess.java`) and 2-step email code verification.
- **Role-Based CRUD**: Add, view, edit, and soft-delete student records with confirmation and cancellation dialogs.
- **Filtering & Search**: Filter by Lab Group (G01–G04, Unassigned) and Programme (CS, IT, DS, CYB). Live search by student name or student number with debouncing.
- **Group Management**: Assign/transfer students with total count visibility, obeying the strict 15-member limit per lab group.
- **Android Sharesheet**: Share lecturer group summary (group labels and counts only, omitting personal student data).

### 3. Architecture & Sync
- **Android Architecture**: Clean Architecture with MVVM, Room Local Database, Retrofit/OkHttp, and WorkManager.
- **Offline Outbox & WorkManager Sync**: Outbox queue with unique `operationId` UUIDs for idempotent sync and retry backoff.
- **Concurrency & Conflict Resolution**: Backend row-level locking (`SELECT ... FOR UPDATE`) enforcing the 15-member group limit under concurrent requests. Base version comparison for optimistic offline conflict resolution.

---

## Project Structure

```
CampusApp/
├── app/                      # Android Studio Application Module (Java / Material / Room / Retrofit)
│   ├── src/main/java/com/example/campusapp/
│   │   ├── auth/            # Student & Lecturer Login/Register Activities
│   │   ├── data/            # Local Room DB, Entities, DAOs, DTOs, Repositories, WorkManager Workers
│   │   ├── lecturer/        # Lecturer Roster, Editor, Summary, Notices Activities
│   │   ├── student/         # Student Home, Profile, Schedule, Map, Chat Activities
│   │   ├── ui/ & viewmodel/ # ViewModels and ViewModel Factories
│   │   └── util/            # Validation, Staff Access, Otp, Timetables, Venue Index
│   └── src/test/java/       # Android JUnit & Repository Tests
├── backend/                  # Node.js & Jest Test Suites
│   └── tests/               # Backend tests (concurrency, conflict, auth, idempotency, unassign)
├── database/                 # MySQL Database Scripts
│   ├── schema.sql           # Database Table Schema
│   └── seed-test-fixture.sql# Seeding script for test fixtures
├── docs/                     # Project Documentation & Artifacts
│   ├── api-contract.md      # API REST Endpoints Specification
│   ├── er-diagram.md        # Database ER Diagram (Mermaid)
│   ├── test-checklist.md    # Test Execution Summary
│   ├── contribution-log.md  # Group Member Contribution Log
│   └── ai-source-log.md     # AI & External Source Attribution Log
├── src/                      # Express Controllers, Routes, Middleware, DB Pool
├── server.js                 # Express Application Entry Point
└── README.md                 # Project README
```

---

## Setup & Installation

### 1. Requirements
- **Android Studio**: Jellyfish or newer (JDK 17+)
- **Node.js**: v18 or newer
- **MySQL Server**: v8.0 or newer (InnoDB engine)

### 2. Database Setup
1. Open MySQL terminal or Workbench.
2. Execute `database/schema.sql`:
   ```sql
   SOURCE database/schema.sql;
   ```
3. (Optional) Seed test data using `database/seed-test-fixture.sql`:
   ```sql
   SOURCE database/seed-test-fixture.sql;
   ```

### 3. Backend Setup (Node.js/Express)
1. Navigate to the project root directory:
   ```bash
   npm install
   ```
2. Create a `.env` file from `.env.example`:
   ```env
   PORT=3000
   DB_HOST=localhost
   DB_USER=root
   DB_PASSWORD=your_password
   DB_NAME=campus_app
   JWT_SECRET=super_secret_jwt_key_at_least_32_characters_long
   ```
3. Start the Express server:
   ```bash
   npm start
   ```

### 4. Running Backend Unit & Concurrency Tests
Run the Jest test suite against the MySQL database:
```bash
cmd /c npx jest backend/tests --runInBand --testTimeout=30000
```

### 5. Android Application Setup
1. Open the project folder in **Android Studio**.
2. Perform a **Gradle Sync**.
3. Build the debug APK or run unit tests:
   ```bash
   ./gradlew assembleDebug testDebugUnitTest
   ```
4. Deploy `app-debug.apk` to an Android Emulator or connected device (Android 8.0+ / API 26+).

---

## Verification & Testing Summary

- **Backend Integration Tests**: 7 test suites passing (including 20 simultaneous concurrent request races for the 15th seat in `concurrency.test.js`, idempotency replay in `idempotency.test.js`, and offline stale conflict rejection in `conflict.test.js`).
- **Android Unit Tests**: 50 JVM unit tests passing (`ValidationTest`, `ProgramsTest`, `NextClassTest`, `LecturerRosterViewModelTest`, etc.).
- **Security & Integrity**: Passwords hashed with PBKDF2-HMAC-SHA256, student numbers validated for 9 digits, row locks protecting group occupancy limits, and soft deletes protecting student identity and sync state.
