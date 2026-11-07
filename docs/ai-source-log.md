# AI and External Source Log
ICT361 Mobile Application Development | Group Lab Project
Project: **CampusApp – Android Student Registration & Lab Group Management System**

This log documents all AI assistance, tools, and external reference documentation used during the design, development, debugging, and verification of CampusApp.

---

## AI & Tool Assistance Log

| Tool / Source | Question / Task Description | Code / Module Affected | Changes Made | Verification Method |
|---|---|---|---|---|
| **Android Studio AI / Gemini** | Architecture & directory cleanup after merge conflicts | Package `com.example.campusapp.data`, `AppDatabase.java` | Standardized package structure to `data`, restored Room database class and fixed case sensitivity on Windows | Built Android project (`./gradlew assembleDebug`), verified no package errors |
| **Android Studio AI / Gemini** | Edge-to-edge padding and gesture insets for Android 15+ | `CampusApp.java`, `styles.xml` | Implemented `ActivityLifecycleCallbacks` to automatically apply system bar padding to every activity | Visual verification on Pixel emulator (Android 15 / SDK 36) |
| **Android Studio AI / Gemini** | Password security hardening for staff & student accounts | `LocalAuthRepository.java`, `authController.js` | Applied PBKDF2-HMAC-SHA256 password hashing with 120,000 iterations and salt | Verified hash generation and login password validation |
| **Android Studio AI / Gemini** | Concurrent group capacity enforcement in MySQL | `src/controllers/studentsController.js`, `database/schema.sql` | Applied InnoDB row-level locking (`SELECT ... FOR UPDATE`) in MySQL transactions | Ran `backend/tests/concurrency.test.js` (20 simultaneous request races passed) |
| **Android Studio AI / Gemini** | Idempotency engine for offline outbox sync | `src/controllers/operationsController.js`, `OperationSyncWorker.java` | Enforced UUID-based operation receipts storing replayed HTTP status and payload fingerprints | Ran `backend/tests/idempotency.test.js` (replay tests passed) |

---

## Code Verification & Responsible AI Attestation

1. **Privacy & Security**: No real student data, real credentials, or private keys were uploaded to any AI tool or remote service. Fictitious data only was used.
2. **Individual Code Competency**: Every team member reviewed, executed, and verified all AI-assisted code additions, ensuring full understanding of the data flow and system architecture for the live demonstration.
3. **Verification**: All generated code underwent automated static analysis, unit testing (`./gradlew testDebugUnitTest`), and backend integration testing (`npm test`).
