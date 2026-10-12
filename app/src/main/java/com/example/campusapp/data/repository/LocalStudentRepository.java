package com.example.campusapp.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusapp.data.AppData;
import com.example.campusapp.data.PendingChange;
import com.example.campusapp.data.Programs;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.Validation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * StudentRepository for the app while there is no server. AppData stays the single store for
 * the roster, so the lecturer screens, the group chat and the approval queue all see the same
 * students. This class translates between AppData's records and the model the ViewModels use
 * (programme codes, a confirmed group, a requested group, a record version).
 *
 * Every call answers on the thread that made it. LiveData is updated with postValue(),
 * which is safe from any thread.
 */
public class LocalStudentRepository implements StudentRepository {

    public static final int PAGE_SIZE = 20;

    /** What AppData stores for a student who has no group. */
    private static final String UNASSIGNED_LABEL = "Unassigned";

    private static final String DRAFT_PREFS = "campus_drafts";
    private static final String KEY_DRAFT = "registration_draft";

    private final AppData appData;
    private final LocalAuthRepository auth;
    private final SharedPreferences draftPrefs;

    private final MutableLiveData<Student> ownProfile = new MutableLiveData<>();
    private final MutableLiveData<List<GroupCount>> groupCounts = new MutableLiveData<>();

    /** Record versions, so a second edit of an old copy is reported as a conflict. */
    private final Map<String, Integer> versions = new HashMap<>();

    public LocalStudentRepository(Context context, LocalAuthRepository auth) {
        this.appData = AppData.getInstance(context);
        this.auth = auth;
        this.draftPrefs = context.getApplicationContext()
                .getSharedPreferences(DRAFT_PREFS, Context.MODE_PRIVATE);
        refresh();
    }

    // ----- Student: registration -----

    @Override
    public void saveRegistrationDraft(Student draft, String email,
                                      RepoCallback<Student> callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", nullToEmpty(draft.name));
            o.put("number", nullToEmpty(draft.studentNumber));
            o.put("programme", nullToEmpty(draft.programme));
            o.put("group", nullToEmpty(draft.pendingGroup));
            o.put("email", nullToEmpty(email));
            // The password is deliberately not kept.
            draftPrefs.edit().putString(KEY_DRAFT, o.toString()).apply();
            callback.onResult(Result.of(Result.Status.PENDING, draft));
        } catch (JSONException e) {
            callback.onResult(Result.of(Result.Status.ERROR, null));
        }
    }

    /** The saved registration draft, or null when there is none. */
    public Student getRegistrationDraft() {
        String json = draftPrefs.getString(KEY_DRAFT, null);
        if (json == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            Student s = new Student();
            s.name = o.optString("name");
            s.studentNumber = o.optString("number");
            s.programme = o.optString("programme");
            s.email = o.optString("email");
            String group = o.optString("group");
            s.pendingGroup = group.isEmpty() ? null : group;
            return s;
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public void register(Student draft, String email, String password,
                         RepoCallback<Student> callback) {
        if (email == null || email.trim().isEmpty() || auth.findAccount(email) != null) {
            callback.onResult(Result.of(Result.Status.DUPLICATE_EMAIL, null));
            return;
        }
        if (findByNumber(draft.studentNumber) != null) {
            callback.onResult(Result.of(Result.Status.DUPLICATE_NUMBER, null));
            return;
        }

        com.example.campusapp.data.Student created = appData.addStudent(
                draft.studentNumber, draft.name, UNASSIGNED_LABEL, Programs.toLabel(draft.programme));
        if (!auth.createAccount(draft.studentNumber, password, created.id,
                Session.ROLE_STUDENT, email)) {
            appData.deleteStudent(created.id); // never leave a profile that nobody can sign in to
            callback.onResult(Result.of(Result.Status.DUPLICATE_NUMBER, null));
            return;
        }
        versions.put(created.id, 1);
        draftPrefs.edit().remove(KEY_DRAFT).apply();

        // A group is only requested here. The lecturer places the student, so the group is
        // not confirmed and no place is reserved.
        Result.Status status = Result.Status.SUCCESS;
        String wanted = draft.pendingGroup;
        if (wanted != null && !wanted.isEmpty()) {
            if (isFull(wanted, null)) {
                status = Result.Status.GROUP_FULL;
            } else {
                appData.addPendingChange(created.id, created.name, "group", "Group", wanted);
                status = Result.Status.PENDING;
            }
        }
        refresh();
        callback.onResult(Result.of(status, toModel(created)));
    }

    /** True when a student with this number is already on the roster. */
    public boolean numberExists(String number) {
        return findByNumber(number) != null;
    }

    // ----- Student: own profile -----

    @Override
    public LiveData<Student> observeOwnProfile() {
        // Who is signed in may have changed since the last refresh (sign-in, sign-out),
        // so recompute before handing the screen its first value.
        refresh();
        return ownProfile;
    }

    @Override
    public void updateOwnDetails(String name, String programme, RepoCallback<Student> callback) {
        com.example.campusapp.data.Student me = currentStudent();
        if (me == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        appData.updateStudent(me.id, me.number, name, me.group, Programs.toLabel(programme));
        bump(me.id);
        refresh();
        callback.onResult(Result.of(Result.Status.SUCCESS, toModel(me)));
    }

    @Override
    public void requestGroupChange(String group, RepoCallback<Student> callback) {
        com.example.campusapp.data.Student me = currentStudent();
        if (me == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        if (group.equals(me.group)) {
            callback.onResult(Result.of(Result.Status.SUCCESS, toModel(me)));
            return;
        }
        if (isFull(group, me.id)) {
            callback.onResult(Result.of(Result.Status.GROUP_FULL, toModel(me)));
            return;
        }
        appData.addPendingChange(me.id, me.name, "group", "Group", group);
        refresh();
        callback.onResult(Result.of(Result.Status.PENDING, toModel(me)));
    }

    @Override
    public void requestNumberCorrection(String proposedNumber, RepoCallback<Void> callback) {
        com.example.campusapp.data.Student me = currentStudent();
        if (me == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        com.example.campusapp.data.Student other = findByNumber(proposedNumber);
        if (other != null && !other.id.equals(me.id)) {
            callback.onResult(Result.of(Result.Status.DUPLICATE_NUMBER, null));
            return;
        }
        appData.addPendingChange(me.id, me.name, "number", "Student Number", proposedNumber);
        refresh();
        callback.onResult(Result.of(Result.Status.PENDING, null));
    }

    // ----- Both roles -----

    @Override
    public LiveData<List<GroupCount>> observeGroupCounts() {
        return groupCounts;
    }

    /** The manual Sync action. There is no server yet, so this re-reads what is stored. */
    @Override
    public void syncNow() {
        refresh();
    }

    // ----- Lecturer -----

    @Override
    public void search(String query, String programme, String group, int page,
                       RepoCallback<StudentPage> callback) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Student> matches = new ArrayList<>();
        for (com.example.campusapp.data.Student s : appData.getStudents()) {
            Student m = toModel(s);
            if (!needle.isEmpty()
                    && !nullToEmpty(m.name).toLowerCase(Locale.ROOT).contains(needle)
                    && !nullToEmpty(m.studentNumber).contains(needle)) {
                continue;
            }
            if (programme != null && !programme.equals(m.programme)) {
                continue;
            }
            if (group != null) {
                boolean wantsUnassigned = UNASSIGNED.equals(group);
                if (wantsUnassigned ? m.confirmedGroup != null : !group.equals(m.confirmedGroup)) {
                    continue;
                }
            }
            matches.add(m);
        }
        Collections.sort(matches, (a, b) ->
                nullToEmpty(a.name).compareToIgnoreCase(nullToEmpty(b.name)));

        int from = Math.min(Math.max(page, 0) * PAGE_SIZE, matches.size());
        int to = Math.min(from + PAGE_SIZE, matches.size());
        List<Student> pageItems = new ArrayList<>(matches.subList(from, to));
        callback.onResult(Result.of(Result.Status.SUCCESS,
                new StudentPage(pageItems, to < matches.size(), false)));
    }

    @Override
    public void getStudent(String studentId, RepoCallback<Student> callback) {
        com.example.campusapp.data.Student s = appData.findStudentById(studentId);
        if (s == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
        } else {
            callback.onResult(Result.of(Result.Status.SUCCESS, toModel(s)));
        }
    }

    @Override
    public void createStudent(Student student, RepoCallback<Student> callback) {
        if (findByNumber(student.studentNumber) != null) {
            callback.onResult(Result.of(Result.Status.DUPLICATE_NUMBER, null));
            return;
        }
        com.example.campusapp.data.Student created = appData.addStudent(
                student.studentNumber, student.name, UNASSIGNED_LABEL,
                Programs.toLabel(student.programme));
        versions.put(created.id, 1);
        refresh();
        callback.onResult(Result.of(Result.Status.SUCCESS, toModel(created)));
    }

    @Override
    public void updateStudent(Student student, RepoCallback<Student> callback) {
        com.example.campusapp.data.Student current = appData.findStudentById(student.studentId);
        if (current == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        if (student.version != versionOf(current.id)) {
            callback.onResult(Result.conflict(student, toModel(current)));
            return;
        }
        com.example.campusapp.data.Student other = findByNumber(student.studentNumber);
        if (other != null && !other.id.equals(current.id)) {
            callback.onResult(Result.of(Result.Status.DUPLICATE_NUMBER, null));
            return;
        }
        appData.updateStudent(current.id, student.studentNumber, student.name,
                current.group, Programs.toLabel(student.programme));
        bump(current.id);
        refresh();
        callback.onResult(Result.of(Result.Status.SUCCESS, toModel(current)));
    }

    @Override
    public void assignGroup(String studentId, String group, RepoCallback<Student> callback) {
        com.example.campusapp.data.Student s = appData.findStudentById(studentId);
        if (s == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        if (group != null && !group.equals(s.group) && isFull(group, s.id)) {
            callback.onResult(Result.of(Result.Status.GROUP_FULL, toModel(s)));
            return;
        }
        appData.updateStudent(s.id, s.number, s.name,
                group == null ? UNASSIGNED_LABEL : group, s.program);
        bump(s.id);
        refresh();
        callback.onResult(Result.of(Result.Status.SUCCESS, toModel(s)));
    }

    @Override
    public void deleteStudent(String studentId, RepoCallback<Void> callback) {
        if (appData.findStudentById(studentId) == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        appData.deleteStudent(studentId);
        versions.remove(studentId);
        refresh();
        callback.onResult(Result.of(Result.Status.SUCCESS, null));
    }

    // ----- Internals -----

    /**
     * Re-reads the stored data into the LiveData the screens observe. Screens that can change
     * data behind this class's back (the approval queue) are refreshed by calling syncNow().
     */
    private void refresh() {
        com.example.campusapp.data.Student me = currentStudent();
        Student profile = me == null ? null : toModel(me);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ownProfile.setValue(profile); // immediate, so a screen never shows the previous student
        } else {
            ownProfile.postValue(profile);
        }

        List<GroupCount> counts = new ArrayList<>();
        for (String group : Validation.GROUPS) {
            int n = 0;
            for (com.example.campusapp.data.Student s : appData.getStudents()) {
                if (group.equals(s.group)) {
                    n++;
                }
            }
            counts.add(new GroupCount(group, n));
        }
        groupCounts.postValue(counts);
    }

    /** The signed-in student, or the demo student when the app is used as a guest. */
    private com.example.campusapp.data.Student currentStudent() {
        Session session = auth.currentSession();
        String id = (session != null && !session.isLecturer())
                ? session.accountId : AppData.CURRENT_STUDENT_ID;
        return appData.findStudentById(id);
    }

    private com.example.campusapp.data.Student findByNumber(String number) {
        return number == null ? null : appData.findStudentByNumber(number.trim());
    }

    /** True when the group already has its 15 students, not counting the student asking. */
    private boolean isFull(String group, String exceptStudentId) {
        int n = 0;
        for (com.example.campusapp.data.Student s : appData.getStudents()) {
            if (group.equals(s.group) && !s.id.equals(exceptStudentId)) {
                n++;
            }
        }
        return n >= GroupCount.CAPACITY;
    }

    private Student toModel(com.example.campusapp.data.Student s) {
        Student m = new Student();
        m.studentId = s.id;
        m.name = s.name;
        m.studentNumber = s.number;
        m.programme = Programs.toCode(s.program);
        m.confirmedGroup = isUnassigned(s.group) ? null : s.group;
        m.pendingGroup = pendingGroupFor(s.id);
        m.syncStatus = hasPendingRequest(s.id) ? Student.PENDING : Student.SYNCED;
        m.version = versionOf(s.id);
        return m;
    }

    private String pendingGroupFor(String studentId) {
        for (PendingChange c : appData.getPendingChanges()) {
            if (studentId.equals(c.studentId) && "group".equals(c.field)
                    && "Pending".equals(c.status)) {
                return c.newValue;
            }
        }
        return null;
    }

    private boolean hasPendingRequest(String studentId) {
        for (PendingChange c : appData.getPendingChanges()) {
            if (studentId.equals(c.studentId) && "Pending".equals(c.status)) {
                return true;
            }
        }
        return false;
    }

    private int versionOf(String studentId) {
        Integer v = versions.get(studentId);
        return v == null ? 1 : v;
    }

    private void bump(String studentId) {
        versions.put(studentId, versionOf(studentId) + 1);
    }

    private static boolean isUnassigned(String group) {
        return group == null || group.isEmpty() || UNASSIGNED_LABEL.equalsIgnoreCase(group);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
