FIREBASE REMOVED: the app talks to your Node.js + MySQL server again

1. Copy the five files in this zip into your project, each to the SAME path, and choose Replace:
     build.gradle.kts                  (project root)
     settings.gradle.kts               (project root)
     gradle/libs.versions.toml
     app/build.gradle.kts
     app/src/main/java/com/example/campusapp/util/ServiceLocator.java
2. DELETE these three files by hand (they are not in the zip):
     app/src/main/java/com/example/campusapp/data/repository/FirebaseAuthRepository.java
     google-services.json              (project root)
     app/google-services.json
3. In Android Studio: File > Sync Project with Gradle Files, then Build > Clean Project.
4. The server address now lives in ONE place: the line  val apiBaseUrl = "..."  at the top of
   app/build.gradle.kts. Change only that line, then Sync Now and Run again.
     USB phone:  "http://localhost:5000/"   and run once:  adb reverse tcp:5000 tcp:5000
     Emulator:   "http://10.0.2.2:5000/"
     Tunnel:     "https://your-address.trycloudflare.com/"
   The address must end with a slash.
