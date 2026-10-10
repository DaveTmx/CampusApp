package com.example.campusapp.data.repository;

import androidx.lifecycle.LiveData;

import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;

import java.util.List;

/**
 * What the ViewModels need from student data. The real class coordinates
 * Room, Retrofit and the sync queue and does that work off the main thread.
 * A ViewModel only ever sees this interface.
 */
public interface StudentRepository {

    /** Group filter value meaning "students with no group". */
    String UNASSIGNED = "UNASSIGNED";

    // ----- Student: registration -----

    /** Saves what has been typed so far in Room. Works offline. */
    void saveRegistrationDraft(Student draft, String claimCode, RepoCallback<Student> callback);

    /**
     * Creates the profile and account (or links to a profile the lecturer already
     * entered), then asks for draft.pendingGroup if one was chosen.
     * GROUP_FULL means the profile exists but is unassigned.
     */
    void register(Student draft, String claimCode, String password, RepoCallback<Student> callback);

    // ----- Student: own profile -----

    /** The signed-in student's profile from Room; updates itself after each sync. */
    LiveData<Student> observeOwnProfile();

    void updateOwnDetails(String name, String programme, RepoCallback<Student> callback);

    /** On GROUP_FULL the previous group is kept. */
    void requestGroupChange(String group, RepoCallback<Student> callback);

    /** Students cannot change their own number; this asks the lecturer to. */
    void requestNumberCorrection(String proposedNumber, RepoCallback<Void> callback);

    // ----- Both roles -----

    LiveData<List<GroupCount>> observeGroupCounts();

    /** The manual Sync action. */
    void syncNow();

    // ----- Lecturer -----

    /**
     * @param query     name or student number, or "" for everyone
     * @param programme CS, IT, DS, or null for any
     * @param group     G01..G04, UNASSIGNED, or null for any
     * @param page      0 for the first page
     */
    void search(String query, String programme, String group, int page,
                RepoCallback<StudentPage> callback);

    void getStudent(String studentId, RepoCallback<Student> callback);

    /** Creates the profile only; a group is requested afterwards with assignGroup. */
    void createStudent(Student student, RepoCallback<Student> callback);

    /** student.version must be the version that was loaded, so conflicts can be detected. */
    void updateStudent(Student student, RepoCallback<Student> callback);

    /** group may be null to unassign. On GROUP_FULL the previous group is kept. */
    void assignGroup(String studentId, String group, RepoCallback<Student> callback);

    void deleteStudent(String studentId, RepoCallback<Void> callback);
}
