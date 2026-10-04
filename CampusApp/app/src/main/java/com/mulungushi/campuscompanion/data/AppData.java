package com.mulungushi.campuscompanion.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app data store for the whole demo: the student roster, pending
 * student requests awaiting lecturer approval, and posted notices.
 *
 * There is no backend server here — everything lives in SharedPreferences
 * as JSON, so it persists between app runs on the same device but is not
 * shared between devices. This is intentionally simple (no Room/SQLite)
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
    private final List<Student> students = new ArrayList<>();
    private final List<PendingChange> pendingChanges = new ArrayList<>();
    private final List<Notice> notices = new ArrayList<>();
    private final List<ChatMessage> messages = new ArrayList<>();

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
        students.add(new Student(CURRENT_STUDENT_ID, "20241908", "Chileshe Banda", "G02", Programs.COMPUTER_SCIENCE));
        students.add(new Student("s-20242203", "20242203", "Mwansa Phiri", "G01", Programs.INFORMATION_TECHNOLOGY));
        students.add(new Student("s-20240891", "20240891", "Sipho Tembo", "G02", Programs.CYBER_SECURITY));

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
                "Almost done on my side, will share tonight.", now - 1_800_000));
    }

    // ---------- students (lecturer: add / edit / delete) ----------

    public List<Student> getStudents() {
        return students;
    }

    /** Creates a new roster entry with a fresh internal id. Used by the lecturer's "+" button. */
    public Student addStudent(String number, String name, String group, String program) {
        Student student = new Student("s-new-" + (nextStudentSeq++), number, name, group, program);
        students.add(student);
        save();
        return student;
    }

    /** Updates the student with this internal id in place (name/number/group can all change). */
    public void updateStudent(String id, String number, String name, String group, String program) {
        Student student = findStudentById(id);
        if (student != null) {
            student.number = number;
            student.name = name;
            student.group = group;
            student.program = program;
            save();
        }
    }

    public void deleteStudent(String id) {
        Student student = findStudentById(id);
        if (student != null) {
            students.remove(student);
            save();
        }
    }

    public Student findStudentByNumber(String number) {
        for (Student s : students) {
            if (s.number.equals(number)) return s;
        }
        return null;
    }

    public Student findStudentById(String id) {
        for (Student s : students) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    // ---------- pending changes (student requests -> lecturer approval) ----------

    public List<PendingChange> getPendingChanges() {
        return pendingChanges;
    }

    public void addPendingChange(String studentId, String studentName,
                                  String field, String fieldLabel, String newValue) {
        pendingChanges.add(new PendingChange(nextPendingId++, studentId, studentName,
                field, fieldLabel, newValue, "Pending"));
        save();
    }

    /** Applies the requested change to the matching student, then removes it from the queue. */
    public void approvePendingChange(long id) {
        PendingChange change = findPendingChange(id);
        if (change == null) return;

        Student student = findStudentById(change.studentId);
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
        PendingChange change = findPendingChange(id);
        if (change != null) {
            pendingChanges.remove(change);
            save();
        }
    }

    public void approveAllPendingChanges() {
        List<PendingChange> copy = new ArrayList<>(pendingChanges);
        for (PendingChange change : copy) {
            approvePendingChange(change.id);
        }
    }

    private PendingChange findPendingChange(long id) {
        for (PendingChange c : pendingChanges) {
            if (c.id == id) return c;
        }
        return null;
    }

    // ---------- notices (lecturer: post updates) ----------

    public List<Notice> getNotices() {
        return notices;
    }

    public void addNotice(String title, String body, String category) {
        notices.add(0, new Notice(nextNoticeId++, title, body, category, true));
        save();
    }

    // ---------- group chat (students: chat & share updates within their own group) ----------

    /** Returns only the messages posted to this group, oldest first. */
    public List<ChatMessage> getMessagesForGroup(String group) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (m.group.equals(group)) result.add(m);
        }
        return result;
    }

    public void addChatMessage(String group, String senderName, String text) {
        messages.add(new ChatMessage(nextMessageId++, group, senderName, text, System.currentTimeMillis()));
        save();
    }

    // ---------- persistence (SharedPreferences + JSON) ----------

    private void load() {
        try {
            JSONArray studentsArray = new JSONArray(prefs.getString(KEY_STUDENTS, "[]"));
            for (int i = 0; i < studentsArray.length(); i++) {
                JSONObject o = studentsArray.getJSONObject(i);
                students.add(new Student(o.getString("id"), o.getString("number"), o.getString("name"), o.getString("group"),
                        o.optString("program", Programs.DEFAULT)));
            }

            JSONArray pendingArray = new JSONArray(prefs.getString(KEY_PENDING, "[]"));
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject o = pendingArray.getJSONObject(i);
                long id = o.getLong("id");
                pendingChanges.add(new PendingChange(id, o.getString("studentId"), o.getString("studentName"),
                        o.getString("field"), o.getString("fieldLabel"), o.getString("newValue"), o.getString("status")));
                if (id >= nextPendingId) nextPendingId = id + 1;
            }

            JSONArray noticesArray = new JSONArray(prefs.getString(KEY_NOTICES, "[]"));
            for (int i = 0; i < noticesArray.length(); i++) {
                JSONObject o = noticesArray.getJSONObject(i);
                long id = o.getLong("id");
                notices.add(new Notice(id, o.getString("title"), o.getString("body"),
                        o.getString("category"), o.getBoolean("isNew")));
                if (id >= nextNoticeId) nextNoticeId = id + 1;
            }

            JSONArray messagesArray = new JSONArray(prefs.getString(KEY_MESSAGES, "[]"));
            for (int i = 0; i < messagesArray.length(); i++) {
                JSONObject o = messagesArray.getJSONObject(i);
                long id = o.getLong("id");
                messages.add(new ChatMessage(id, o.getString("group"), o.getString("senderName"),
                        o.getString("text"), o.getLong("timestamp")));
                if (id >= nextMessageId) nextMessageId = id + 1;
            }

            // Keep the "add student" sequence past anything already persisted.
            for (Student s : students) {
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
            for (Student s : students) {
                JSONObject o = new JSONObject();
                o.put("id", s.id);
                o.put("number", s.number);
                o.put("name", s.name);
                o.put("group", s.group);
                o.put("program", s.program);
                studentsArray.put(o);
            }

            JSONArray pendingArray = new JSONArray();
            for (PendingChange c : pendingChanges) {
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
            for (Notice n : notices) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("title", n.title);
                o.put("body", n.body);
                o.put("category", n.category);
                o.put("isNew", n.isNew);
                noticesArray.put(o);
            }

            JSONArray messagesArray = new JSONArray();
            for (ChatMessage m : messages) {
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
