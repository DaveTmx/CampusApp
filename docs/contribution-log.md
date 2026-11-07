# Group Contribution Log
ICT361 Mobile Application Development | Competency Based Group Lab Project
Project: **CampusApp – Android Student Registration & Lab Group Management System**

This log tracks task assignments, code contributions, commits, testing evidence, and individual competencies for all **15 project group members** divided into five sub-teams of three members.

---

## Sub-Team Breakdown & Task Assignment

### Team 1: UI/UX & Accessibility
| Member Name | Student Number | Assigned Role / Tasks | Key Commits / Code Modules | Testing Evidence & Verification |
|---|---|---|---|---|
| Member 1 | 240000001 | Registration & Sign-In XML Forms, Material Theme | `activity_student_register.xml`, `activity_student_login.xml`, `styles.xml` | Form input retention test, 200% text scaling test |
| Member 2 | 240000002 | Student Profile & Schedule Screens, Edge-to-Edge Insets | `StudentProfileActivity.java`, `StudentScheduleActivity.java`, `CampusApp.java` | Profile occupancy display & schedule filter test |
| Member 3 | 240000003 | Lecturer Roster UI, Sharesheet Integration & TalkBack | `activity_lecturer_roster.xml`, `LecturerSummaryActivity.java`, `strings_accessibility.xml` | Sharesheet preview test, TalkBack label navigation test |

---

### Team 2: Android Architecture & ViewModels
| Member Name | Student Number | Assigned Role / Tasks | Key Commits / Code Modules | Testing Evidence & Verification |
|---|---|---|---|---|
| Member 4 | 240000004 | ViewModel Lifecycle & Auth ViewModel | `AuthViewModel.java`, `RegisterViewModel.java` | `ValidationTest` pass, rotation state retention test |
| Member 5 | 240000005 | Lecturer Roster ViewModel & Search Debouncer | `LecturerRosterViewModel.java`, `Debouncer.java` | `LecturerRosterViewModelTest` (8/8 unit tests pass) |
| Member 6 | 240000006 | Editor & Profile ViewModels, Process Death Handler | `EditorViewModel.java`, `ProfileViewModel.java` | SavedStateHandle survival test across activity restart |

---

### Team 3: Local Storage & Offline Sync
| Member Name | Student Number | Assigned Role / Tasks | Key Commits / Code Modules | Testing Evidence & Verification |
|---|---|---|---|---|
| Member 7 | 240000007 | Room AppDatabase, Student & Operation DAOs | `AppDatabase.java`, `StudentDao.java`, `OperationDao.java` | `AppDatabaseMigrationTest` pass |
| Member 8 | 240000008 | Offline Outbox Queue & Local Repositories | `OutboxQueue.java`, `LocalStudentRepository.java`, `AppData.java` | Local CRUD offline persistence test |
| Member 9 | 240000009 | WorkManager Sync Worker & Optimistic Concurrency | `OperationSyncWorker.java`, `RealStudentRepository.java` | Offline-to-online sync replay & conflict prompt test |

---

### Team 4: Backend API & Relational Database
| Member Name | Student Number | Assigned Role / Tasks | Key Commits / Code Modules | Testing Evidence & Verification |
|---|---|---|---|---|
| Member 10 | 240000010 | MySQL InnoDB Schema, Soft Delete & Constraints | `database/schema.sql`, `database/seed-test-fixture.sql` | Schema load & foreign key integrity test |
| Member 11 | 240000011 | Node.js Express Controllers & Row-Level Locking | `src/controllers/studentsController.js`, `src/db/tx.js` | `concurrency.test.js` (20 simultaneous request races pass) |
| Member 12 | 240000012 | Operations Controller & Idempotency Engine | `src/controllers/operationsController.js`, `src/middleware/auth.js` | `idempotency.test.js` & `conflict.test.js` pass |

---

### Team 5: Testing, Integration & Quality Assurance
| Member Name | Student Number | Assigned Role / Tasks | Key Commits / Code Modules | Testing Evidence & Verification |
|---|---|---|---|---|
| Member 13 | 240000013 | Backend Jest Test Suite & Concurrency Rig | `backend/tests/concurrency.test.js`, `backend/tests/auth.test.js` | All 7 Jest backend test suites pass |
| Member 14 | 240000014 | Android Unit Tests & Data Layer Stubs | `app/src/test/java/com/example/campusapp/` | 50 JUnit tests pass |
| Member 15 | 240000015 | End-to-End System Verification & Contract Audit | `docs/api-contract.md`, `docs/test-checklist.md`, `README.md` | API contract audit & end-to-end flow verification |

---

## Member Responsibility Rotation & Live Demonstration Log

- **Checkpoint 1 (UI/UX & Architecture)**: Rotation verified for form accessibility, ViewModel state retention, and database schema creation.
- **Checkpoint 2 (Local Sync & Backend Integration)**: Rotation verified for Room transaction queuing, WorkManager backoff, and Express parameterized queries.
- **Checkpoint 3 (Concurrency & Conflict Testing)**: Live testing of 15-student limit under 20 concurrent request races, operation receipt idempotency, and offline conflict resolution.
