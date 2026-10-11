package com.example.campusapp.data;

import android.content.Context;
import android.content.SharedPreferences;

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
import com.example.campusapp.Data.Programs;
import com.example.campusapp.Data.Student;

=======
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app data store for the whole demo: the student roster, pending
 * student requests awaiting lecturer approval, and posted notices.
 *
 * as JSON, so it persists between app runs on the same device but is not
 * shared between devices. This is intentionally simple (no Room/SQLite)
 * There is no backend server here — everything lives in SharedPreferences
 * so the whole thing stays easy to read for a coursework project; swapping
 * in a real database later only means rewriting the methods below.
 */
public class AppData {

    // The student currently "logged in" on the student side of the app.
    // (There's no real authentication in this project — see StudentLoginActivity.)
    // This is Student.id, which never changes even if the student's number does.
    public static final String CURRENT_STUDENT_ID = "s-20241908";

    private static final String PREFS = "campus_companion_data";
    private static final String KEY_STUDENTS = "students";
    private static final String KEY_PENDING = "pending_changes";
    private static final String KEY_NOTICES = "notices";
    private static final String KEY_MESSAGES = "chat_messages";

    private static AppData instance;

    private final SharedPreferences prefs;
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    private final List<com.example.campusapp.Data.Student> students = new ArrayList<com.example.campusapp.Data.Student>();
    private final List<com.example.campusapp.Data.PendingChange> pendingChanges = new ArrayList<>();
    private final List<com.example.campusapp.Data.Notice> notices = new ArrayList<>();
    private final List<com.example.campusapp.Data.ChatMessage> messages = new ArrayList<>();
=======
    private final List<Student> students = new ArrayList<>();
    private final List<PendingChange> pendingChanges = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private final List<ChatMessage> messages = new ArrayList<>();
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java

    private long nextPendingId = 1;
    private long nextNoticeId = 1;
    private long nextStudentSeq = 1;
    private long nextMessageId = 1;

    private AppData(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
        if (students.isEmpty() && notices.isEmpty()) {
            seedDefaults();
            save();
        }
    }

    public static synchronized AppData getInstance(Context context) {
        if (instance == null) {
            instance = new AppData(context);
        }
        return instance;
    }

    // ---------- seed data (matches the original mockup) ----------

    private void seedDefaults() {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        students.add(new com.example.campusapp.Data.Student(CURRENT_STUDENT_ID, "20241908", "Chileshe Banda", "G02", com.example.campusapp.Data.Programs.COMPUTER_SCIENCE));
        students.add(new com.example.campusapp.Data.Student("s-20242203", "20242203", "Mwansa Phiri", "G01", Programs.INFORMATION_TECHNOLOGY));
        students.add(new com.example.campusapp.Data.Student("s-20240891", "20240891", "Sipho Tembo", "G02", Programs.CYBER_SECURITY));

        pendingChanges.add(new com.example.campusapp.Data.PendingChange(nextPendingId++, "s-20242203", "Mwansa Phiri",
                "group", "Group", "G03", "Pending"));

        notices.add(new com.example.campusapp.Data.Notice(nextNoticeId++, "Library Closed Saturday",
                "The main library will be closed for quarterly maintenance.", "Admin", true));
        notices.add(new com.example.campusapp.Data.Notice(nextNoticeId++, "Exam Timetable Posted",
                "The draft timetable for Semester 1 exams is now ready.", "Academic", false));
        notices.add(new com.example.campusapp.Data.Notice(nextNoticeId++, "Fee Deadline Extended",
                "Registration fee payment deadline extended to next Friday.", "Urgent", false));

        long now = System.currentTimeMillis();
        messages.add(new com.example.campusapp.Data.ChatMessage(nextMessageId++, "G02", "Sipho Tembo",
                "Hey team, did everyone finish the ER diagram for the DB project?", now - 3_600_000));
        messages.add(new com.example.campusapp.Data.ChatMessage(nextMessageId++, "G02", "Chileshe Banda",
=======
        students.add(new Student(CURRENT_STUDENT_ID, "202419080", "Chileshe Banda", "G02", Programs.COMPUTER_SCIENCE));
        students.add(new Student("s-20242203", "202422030", "Mwansa Phiri", "G01", Programs.INFORMATION_TECHNOLOGY));
        students.add(new Student("s-20240891", "202408910", "Sipho Tembo", "G02", Programs.DATA_SCIENCE));

        pendingChanges.add(new PendingChange(nextPendingId++, "s-20242203", "Mwansa Phiri",
                "group", "Group", "G03", "Pending"));

        notices.add(new Notice(nextNoticeId++, "Library Closed Saturday",
                "The main library will be closed for quarterly maintenance.", "Admin", true));
        notices.add(new Notice(nextNoticeId++, "Exam Timetable Posted",
                "The draft timetable for Semester 1 exams is now ready.", "Academic", false));
        notices.add(new Notice(nextNoticeId++, "Fee Deadline Extended",
                "Registration fee payment deadline extended to next Friday.", "Urgent", false));

        long now = System.currentTimeMillis();
        messages.add(new ChatMessage(nextMessageId++, "G02", "Sipho Tembo",
                "Hey team, did everyone finish the ER diagram for the DB project?", now - 3_600_000));
        messages.add(new ChatMessage(nextMessageId++, "G02", "Chileshe Banda",
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                "Almost done on my side, will share tonight.", now - 1_800_000));
    }

    // ---------- students (lecturer: add / edit / delete) ----------

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public List<com.example.campusapp.Data.Student> getStudents() {
=======
    public List<Student> getStudents() {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        return students;
    }

    /** Creates a new roster entry with a fresh internal id. Used by the lecturer's "+" button. */
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public com.example.campusapp.Data.Student addStudent(String number, String name, String group, String program) {
        com.example.campusapp.Data.Student student = new Student("s-new-" + (nextStudentSeq++), number, name, group, program);
=======
    public Student addStudent(String number, String name, String group, String program) {
        Student student = new Student("s-new-" + (nextStudentSeq++), number, name, group, program);
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        students.add(student);
        save();
        return student;
    }

    /** Updates the student with this internal id in place (name/number/group can all change). */
    public void updateStudent(String id, String number, String name, String group, String program) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        com.example.campusapp.Data.Student student = findStudentById(id);
=======
        Student student = findStudentById(id);
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        if (student != null) {
            student.number = number;
            student.name = name;
            student.group = group;
            student.program = program;
            save();
        }
    }

    public void deleteStudent(String id) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        com.example.campusapp.Data.Student student = findStudentById(id);
=======
        Student student = findStudentById(id);
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        if (student != null) {
            students.remove(student);
            save();
        }
    }

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public com.example.campusapp.Data.Student findStudentByNumber(String number) {
        for (com.example.campusapp.Data.Student s : students) {
=======
    public Student findStudentByNumber(String number) {
        for (Student s : students) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
            if (s.number.equals(number)) return s;
        }
        return null;
    }

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public com.example.campusapp.Data.Student findStudentById(String id) {
        for (com.example.campusapp.Data.Student s : students) {
=======
    public Student findStudentById(String id) {
        for (Student s : students) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    // ---------- pending changes (student requests -> lecturer approval) ----------

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public List<com.example.campusapp.Data.PendingChange> getPendingChanges() {
=======
    public List<PendingChange> getPendingChanges() {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        return pendingChanges;
    }

    public void addPendingChange(String studentId, String studentName,
                                  String field, String fieldLabel, String newValue) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        pendingChanges.add(new com.example.campusapp.Data.PendingChange(nextPendingId++, studentId, studentName,
=======
        pendingChanges.add(new PendingChange(nextPendingId++, studentId, studentName,
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                field, fieldLabel, newValue, "Pending"));
        save();
    }

    /** Applies the requested change to the matching student, then removes it from the queue. */
    public void approvePendingChange(long id) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        com.example.campusapp.Data.PendingChange change = findPendingChange(id);
        if (change == null) return;

        com.example.campusapp.Data.Student student = findStudentById(change.studentId);
=======
        PendingChange change = findPendingChange(id);
        if (change == null) return;

        Student student = findStudentById(change.studentId);
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        if (student != null) {
            if ("group".equals(change.field)) {
                student.group = change.newValue;
            } else if ("number".equals(change.field)) {
                student.number = change.newValue;
            } else if ("name".equals(change.field)) {
                student.name = change.newValue;
            }
        }
        pendingChanges.remove(change);
        save();
    }

    /** Discards the request without changing the student record. */
    public void rejectPendingChange(long id) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        com.example.campusapp.Data.PendingChange change = findPendingChange(id);
=======
        PendingChange change = findPendingChange(id);
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        if (change != null) {
            pendingChanges.remove(change);
            save();
        }
    }

    public void approveAllPendingChanges() {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        List<com.example.campusapp.Data.PendingChange> copy = new ArrayList<>(pendingChanges);
        for (com.example.campusapp.Data.PendingChange change : copy) {
=======
        List<PendingChange> copy = new ArrayList<>(pendingChanges);
        for (PendingChange change : copy) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
            approvePendingChange(change.id);
        }
    }

<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    private com.example.campusapp.Data.PendingChange findPendingChange(long id) {
        for (com.example.campusapp.Data.PendingChange c : pendingChanges) {
=======
    private PendingChange findPendingChange(long id) {
        for (PendingChange c : pendingChanges) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
            if (c.id == id) return c;
        }
        return null;
    }

    // ---------- notices (lecturer: post updates) ----------

    public List<Notice> getNotices() {
        return notices;
    }

    public void addNotice(String title, String body, String category) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        notices.add(0, new com.example.campusapp.Data.Notice(nextNoticeId++, title, body, category, true));
=======
        notices.add(0, new Notice(nextNoticeId++, title, body, category, true));
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        save();
    }

    // ---------- group chat (students: chat & share updates within their own group) ----------

    /** Returns only the messages posted to this group, oldest first. */
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
    public List<com.example.campusapp.Data.ChatMessage> getMessagesForGroup(String group) {
        List<com.example.campusapp.Data.ChatMessage> result = new ArrayList<>();
        for (com.example.campusapp.Data.ChatMessage m : messages) {
=======
    public List<ChatMessage> getMessagesForGroup(String group) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage m : messages) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
            if (m.group.equals(group)) result.add(m);
        }
        return result;
    }

    public void addChatMessage(String group, String senderName, String text) {
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
        messages.add(new com.example.campusapp.Data.ChatMessage(nextMessageId++, group, senderName, text, System.currentTimeMillis()));
=======
        messages.add(new ChatMessage(nextMessageId++, group, senderName, text, System.currentTimeMillis()));
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
        save();
    }

    // ---------- persistence (SharedPreferences + JSON) ----------

    private void load() {
        try {
            JSONArray studentsArray = new JSONArray(prefs.getString(KEY_STUDENTS, "[]"));
            for (int i = 0; i < studentsArray.length(); i++) {
                JSONObject o = studentsArray.getJSONObject(i);
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
                students.add(new com.example.campusapp.Data.Student(o.getString("id"), o.getString("number"), o.getString("name"), o.getString("group"),
=======
                students.add(new Student(o.getString("id"), o.getString("number"), o.getString("name"), o.getString("group"),
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                        o.optString("program", Programs.DEFAULT)));
            }

            JSONArray pendingArray = new JSONArray(prefs.getString(KEY_PENDING, "[]"));
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject o = pendingArray.getJSONObject(i);
                long id = o.getLong("id");
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
                pendingChanges.add(new com.example.campusapp.Data.PendingChange(id, o.getString("studentId"), o.getString("studentName"),
=======
                pendingChanges.add(new PendingChange(id, o.getString("studentId"), o.getString("studentName"),
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                        o.getString("field"), o.getString("fieldLabel"), o.getString("newValue"), o.getString("status")));
                if (id >= nextPendingId) nextPendingId = id + 1;
            }

            JSONArray noticesArray = new JSONArray(prefs.getString(KEY_NOTICES, "[]"));
            for (int i = 0; i < noticesArray.length(); i++) {
                JSONObject o = noticesArray.getJSONObject(i);
                long id = o.getLong("id");
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
                notices.add(new com.example.campusapp.Data.Notice(id, o.getString("title"), o.getString("body"),
=======
                notices.add(new Notice(id, o.getString("title"), o.getString("body"),
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                        o.getString("category"), o.getBoolean("isNew")));
                if (id >= nextNoticeId) nextNoticeId = id + 1;
            }

            JSONArray messagesArray = new JSONArray(prefs.getString(KEY_MESSAGES, "[]"));
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject o = messagesArray.getJSONObject(i);
                long id = o.getLong("id");
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
                messages.add(new com.example.campusapp.Data.ChatMessage(id, o.getString("group"), o.getString("senderName"),
=======
                messages.add(new ChatMessage(id, o.getString("group"), o.getString("senderName"),
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                        o.getString("text"), o.getLong("timestamp")));
                if (id >= nextMessageId) nextMessageId = id + 1;
            }

            // Keep the "add student" sequence past anything already persisted.
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
            for (com.example.campusapp.Data.Student s : students) {
=======
            for (Student s : students) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                if (s.id != null && s.id.startsWith("s-new-")) {
                    try {
                        long seq = Long.parseLong(s.id.substring("s-new-".length()));
                        if (seq >= nextStudentSeq) nextStudentSeq = seq + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (JSONException e) {
            // Malformed / first-run prefs: start with empty lists, seedDefaults() will fill them in.
        }
    }

    private void save() {
        try {
            JSONArray studentsArray = new JSONArray();
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
            for (com.example.campusapp.Data.Student s : students) {
=======
            for (Student s : students) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                JSONObject o = new JSONObject();
                o.put("id", s.id);
                o.put("number", s.number);
                o.put("name", s.name);
                o.put("group", s.group);
                o.put("program", s.program);
                studentsArray.put(o);
            }

            JSONArray pendingArray = new JSONArray();
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
            for (com.example.campusapp.Data.PendingChange c : pendingChanges) {
=======
            for (PendingChange c : pendingChanges) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("studentId", c.studentId);
                o.put("studentName", c.studentName);
                o.put("field", c.field);
                o.put("fieldLabel", c.fieldLabel);
                o.put("newValue", c.newValue);
                o.put("status", c.status);
                pendingArray.put(o);
            }

            JSONArray noticesArray = new JSONArray();
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
            for (com.example.campusapp.Data.Notice n : notices) {
=======
            for (Notice n : notices) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("title", n.title);
                o.put("body", n.body);
                o.put("category", n.category);
                o.put("isNew", n.isNew);
                noticesArray.put(o);
            }

            JSONArray messagesArray = new JSONArray();
<<<<<<< HEAD:app/src/main/java/com/example/campusapp/Data/AppData.java
            for (com.example.campusapp.Data.ChatMessage m : messages) {
=======
            for (ChatMessage m : messages) {
>>>>>>> 1fb9c74e30bb442d220357395ef3d7836e24c991:app/src/main/java/com/example/campusapp/data/AppData.java
                JSONObject o = new JSONObject();
                o.put("id", m.id);
                o.put("group", m.group);
                o.put("senderName", m.senderName);
                o.put("text", m.text);
                o.put("timestamp", m.timestamp);
                messagesArray.put(o);
            }

            prefs.edit()
                    .putString(KEY_STUDENTS, studentsArray.toString())
                    .putString(KEY_PENDING, pendingArray.toString())
                    .putString(KEY_NOTICES, noticesArray.toString())
                    .putString(KEY_MESSAGES, messagesArray.toString())
                    .apply();
        } catch (JSONException e) {
            // If this ever throws, the in-memory lists are still correct for the
            // rest of this session — only the persisted copy would be stale.
        }
    }
}
