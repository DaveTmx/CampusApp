# CampusApp – fixes applied

## Round 3b: build output outside the project folder

- Root `build.gradle.kts` sends all build output to `%USERPROFILE%\.campusapp-build\CampusApp-<code>\`. With the project inside OneDrive, OneDrive locked files in `app\build` while uploading them and Clean/Rebuild failed with "Unable to delete directory". The APK is now at `%USERPROFILE%\.campusapp-build\CampusApp-<code>\app\outputs\apk\debug\app-debug.apk`; Run in Android Studio finds it automatically.

## Round 3 (12 Oct 2026): one timetable per programme, map from the timetable

Verified: `gradlew assembleDebug testDebugUnitTest lintDebug` pass (29 unit tests), and on the emulator: an IT student sees the IT timetable and IT venues; switching the profile to Data Science switches both; the lecturer can pick any programme.

- **Four timetables** (`data/Timetable.java`) transcribed from the EduRole PDFs: Computer Science (BScCS3), Information Technology (BScIT3), Data Science (BScDS3), Cyber Security (BScCybS3). `Timetable.VERSION` is now 3, so installed apps reload them once (this also replaces any classes a lecturer added earlier).
- Each stored class has a `program` code. Students see the timetable for the programme on their profile (chosen at registration, changeable on the Profile tab): Schedule tab ("<Programme> timetable"), Home "Next class" card and the map all follow it (`util/CurrentProgram`). Guests see Computer Science.
- **Lecturer schedule**: a Programme picker; Add class and Import CSV apply to the chosen programme only.
- **Campus map** (student and lecturer): lists every venue on the timetable with the classes held there (`util/VenueIndex`), a working search box, and "Open in Google Maps" for each venue. Tapping a class on the Schedule opens the map with its venue outlined at the top. Lecturers see all programmes' venues, with shared classes on one line ("CS, IT, CYB · ICS341 …"). Office list kept below.
- Exact map pins: add coordinates to `COORDINATES` in `student/CampusMapActivity.java`; until then Google Maps searches "<venue>, Mulungushi University, Kabwe".
- From the PDFs: the Cyber Security PDF cuts off the 15:00-17:00 column and the end of the Monday ICS331 room ("ICT Center Lab (Computer…"); Room 2 was assumed. Data Science's "SSS BLOC" (Wed ICT381) is SSS Block-E Rm 1, as on the other programmes.

## Round 2 (12 Oct 2026)

Verified: `gradlew assembleDebug testDebugUnitTest lintDebug` all pass (22 unit tests, no lint errors), and the flows below were exercised on a Pixel 8 emulator (Android 17).

- **Registration**: the Claim Code field is now **Email Address**. Tapping *Create account* checks the form, then emails a 6-digit code (OtpService/EmailJS). The account is only created after the code is entered in the *Verify your email* dialog (wrong code keeps the dialog open; *Resend code* sends a new one). The email is stored on the account.
- **Forgot password** now works for newly registered students (before, their account had no email, so reset said "no email on file"). Removed the duplicated "Forgot password" click handlers.
- **Programs**: Cyber Security (CYB) is in the drop-down, the fake repositories and the MySQL `programme_code` ENUM.
- **Lecturer login**: label "Staff Email or Username", empty field reads "Enter your email or username".
- **Role select**: buttons read "Student" / "Lecturer".
- **Student home**: greeting is "Hi, <name as on the profile>" ("Hi, Guest" for guests); today's date; the *Next class* card reads the stored timetable (`util/NextClass`) and shows e.g. "NEXT CLASS · In 15 mins" / "Tomorrow" / "Wednesday", refreshing every minute; latest notice and study group come from the data. Tapping the cards opens Schedule / Notices / Chat.
- **Notices bell** opens the Notices screen; its count clears once the student has viewed the notices (`AppData.markAllNoticesRead`). Badge is now solid red so the number is readable.
- **Edge-to-edge (Android 15+)**: new `CampusApp` Application class pads every screen for the status bar, gesture bar and keyboard. Before, headers sat under the clock and the bottom tabs under the gesture bar.
- **Buttons**: custom-drawable buttons get `app:backgroundTint="@null"`, so outline/red buttons (Save draft, Cancel, Log out…) are no longer repainted solid blue by the Material theme.
- **Lint errors fixed**: the "+" buttons on lecturer notices/roster pointed `layout_above` at a view in another container (so they floated at the top); unused `drawable-highdr` screenshot removed; duplicate `ResetPasswordActivity` manifest entry removed.
- EmailJS is still unconfigured: until the three values in `util/OtpConfig.java` are filled in, the code is shown on screen ("demo mode") instead of emailed.

Verified after the fixes: `gradlew assembleDebug` builds the debug APK, and the 9 plain unit tests pass (`LecturerRosterViewModelTest` 8/8, `ExampleUnitTest` 1/1). The 6 Robolectric tests in `RealStudentRepositoryTest` failed on Robolectric 4.13; see section 5 for the upgrade. Their re-run on 4.16.1 had not finished when this zip was made.
Every `require()` in the Node backend resolves to a real file, and every backend `.js` file passes `node --check`.

## 1. Package / folder structure (Android)

The last commit ("Resolve merge conflicts and directory renames") renamed the `Data` package to `data`, but on Windows the folder stayed `Data`. That left the project split three ways:
- some files declared `package com.example.campusapp.data` while sitting in a `Data/` folder;
- other files still declared `...Data` and imported `...Data.*`;
- git itself tracks 25 files under `data/` and 7 under `Data/`.

Fixes:
- Renamed the folder `app/src/main/java/com/example/campusapp/Data` → `data`.
- Renamed the test folder `app/src/test/java/com/example/campusapp/Data` → `data`.
- Changed every remaining `com.example.campusapp.Data…` package declaration and import to `com.example.campusapp.data…`. Files touched:
  - `data/local/dao/StudentDao.java`, `data/local/entities/StudentEntity.java`
  - `data/remote/ApiClient.java`, `data/remote/ApiService.java`, `data/remote/StudentPageDto.java`
  - `data/repository/FakeStudentRosterRepository.java`, `data/repository/RealStudentRepository.java`, `data/repository/StudentRosterRepository.java`
  - `auth/StudentRegisterActivity.java`, `lecturer/LecturerNoticesActivity.java`, `NoticeBell.java`, `student/StudentHomeActivity.java`
  - `ui/lecturer/LecturerRosterViewModel.java`, `ui/lecturer/LecturerRosterViewModelFactory.java`
  - tests: `data/RealStudentRepositoryTest.java`, `ui/lecturer/LecturerRosterViewModelTest.java`
- `util/appexecutors.java` → `util/AppExecutors.java`. A public class must live in a file with the same name, so this was a compile error.
- `data/local/AppDatabase.java` had been emptied by the merge. Restored the Room database class, with the package changed to `data.local`.
- Removed empty placeholder files that had no `.java` extension and duplicated real folders:
  - `data/repository/local/appdatabase`, `DAOs`, `entities`
  - `data/repository/local/remote/ApiClient`, `ApiServices`
  - `sync/syncworker`

## 2. Unresolved merge-conflict markers (`<<<<<<<` / `=======` / `>>>>>>>`)

- `data/AppData.java`: kept the incoming version. It uses short type names and `Programs.DATA_SCIENCE`, and its 9-digit demo numbers match `LocalAuthRepository`. The other side referred to `Programs.CYBER_SECURITY`, which no longer exists.
- `data/repository/StudentPageRepository.java`: kept the `StudentPageRepository` interface. That is the side that matches the file name and is implemented by `data/repository/FakeStudentRepository`.
- `ui/lecturer/LecturerRosterViewModel.java`: kept the version that `LecturerRosterViewModelTest` and `LecturerRosterViewModelFactory` use (`StudentRosterRepository` + `Debouncer`).
- `gradle/wrapper/gradle-wrapper.properties`: removed the markers around the timestamp comment.

## 3. Java compile errors

- `ui/lecturer/LecturerRosterViewModel.java`: removed broken stub methods:
  - `getFromCache()` had no return statement;
  - `getUiState()` used a nonexistent `UiState` type;
  - `Factory` didn't implement `create()`.

  Also removed unused imports.
- `lecturer/LecturerRosterActivity.java`: rewritten against the real ViewModel API (`getState()`, `onQueryChanged()`, `onLoadNextPage()`, `retry()`). It now:
  - uses `LecturerRosterViewModelFactory` + `FakeStudentRosterRepository`;
  - filters by group chip on screen;
  - builds the share summary itself.

  Before, it imported `viewmodel.FakeStudentRosterRepository`, which doesn't exist, and called methods that didn't exist.
- Two classes were named `StatusMessages`:
  - `util/StatusMessages` returned a `String`;
  - `viewmodel/StatusMessages` returns a string resource ID.

  `AuthViewModel` imported the util one and pushed a `String` into a `LiveData<Integer>`. Removed that import and deleted the unused `util/StatusMessages.java`.
- `viewmodel/EditorViewModel.java`: removed two stray `Object StatusMessages;` lines. They hid the real class and were used uninitialised.
- `student/StudentHomeActivity.java`: removed a duplicate `AppData` import and the `AppData AppData = null;` variable that shadowed the class.
- `viewmodel/AuthViewModel.java`: fixed brace indentation.

## 4. Resources / manifest

- `values/colors.xml`: added the missing `logo_navy` (`#0A2B56`, sampled from `splash_logo.jpg`). It is used by `activity_splash.xml`, `ic_launcher.xml` and `ic_launcher_round.xml`; without it the resource merge fails.
- `values/strings.xml`: `notices_bell_description` contained a whole `<string>` tag nested inside CDATA. It is now `Notices, %1$d unread`.
- `AndroidManifest.xml`: registered `.ui.auth.ResetPasswordActivity`. Opening it would otherwise crash.
- `auth/StudentLoginActivity.java`, `auth/LecturerLoginActivity.java`: the "Forgot password?" text (`tvForgot`) now opens `ResetPasswordActivity`. Before, it did nothing.

## 5. Gradle

- `gradle/libs.versions.toml`: restored the `room-common`, `room-runtime` and `room-compiler` entries (2.8.5). The merge deleted them, but `app/build.gradle.kts` still uses them, so Gradle sync failed.
- `app/build.gradle.kts`:
  - removed `implementation(libs.datastore.core)`: its catalog entry was deleted and nothing uses DataStore;
  - `targetSdk` 37 → 36: it must not be higher than `compileSdk` (36);
  - moved `testOptions { … }` out of `defaultConfig` into `android { }`, where it belongs.
- Robolectric `4.13` → `4.16.1`. 4.13 cannot simulate SDK 36 (`targetSdkVersion=36 > maxSdkVersion`) and crashed during cleanup on the JDK 25 test runtime, so all 6 `RealStudentRepositoryTest` tests failed. 4.16.1 supports SDK 36.
- Removed `local.properties`: it pointed at another person's SDK (`C:\Users\david\...`), and Android Studio regenerates it. Also left out the `build/`, `app/build/` and `.gradle/` output folders.

## 6. Node backend

`server.js` and `backend/tests/*` load everything from the root `src/` and `database/`, but the new files had been placed in `backend/src/`. The server crashed at start-up with `MODULE_NOT_FOUND`.

- Moved:
  - `backend/src/controllers/studentsController.js` → `src/controllers/`
  - `backend/src/controllers/operationsController.js` → `src/controllers/`
  - `backend/src/routes/studentRoutes.js` → `src/routes/`
  - `backend/src/routes/operationsRoutes.js` → `src/routes/`
  - `backend/src/middleware/auth.middleware.js` → `src/middleware/auth.js` (every file requires `../middleware/auth`)
  - `backend/src/db/seed-test-fixture.sql` → `database/seed-test-fixture.sql` (the path the concurrency test reads)
- Deleted the empty `backend/src/app.js`.
- Applied `backend/src/controllers/authController.patch.js` to `src/controllers/authController.js`: `login` now returns a signed JWT (`token`) and `role`. Without it, every route protected by `requireAuth` was unreachable. Removed the patch note afterwards.
- Applied `backend/package.json.patch.md` to `package.json`:
  - added `jest` and `supertest` to `devDependencies`;
  - `"test": "jest backend/tests --runInBand"`;
  - also added `start` and `dev` scripts, `"main": "server.js"` (was the nonexistent `index.js`) and a proper package name.

  Removed the patch note afterwards.
- Added `.env.example` with the variables the note listed (`.env` itself is gitignored).
- `server.js`: loads `dotenv` first, before anything reads `process.env`.

## Not changed (flagged for you)

- `src/controllers/groupController.js` reads from `lab_groups` but `createGroup` inserts into `study_groups`, which `database/schema.sql` never creates. Decide which table is meant.
- `database/schema.sql` doesn't add the `users.role` column. Login falls back to `'student'` until you add it (see the comment in the schema).
- Unused duplicates, which compile fine:
  - `ui/student/NoticesActivity` and `ui/student/StudentScheduleActivity` (not in the manifest; the real screens are in `student/`);
  - `data/model/*`, `data/repository/FakeStudentRepository` and `StudentPageRepository` (nothing uses them now);
  - `viewmodel/FakeStudentRepository`, a test double in production code.
- `layout/activity_main.xml` was deleted in your version, and `MainActivity` now uses `activity_splash`, which is fine.
- `RegisterViewModelTest` was deleted in your version, so `RegisterViewModel` has no tests.
- `package-lock.json` hasn't been regenerated. Run `npm install` once so it picks up jest and supertest.
