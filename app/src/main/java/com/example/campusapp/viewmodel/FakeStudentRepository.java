package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.campusapp.Data.repository.RepoCallback;
import com.example.campusapp.Data.repository.StudentRepository;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;

import java.util.ArrayList;
import java.util.List;

/**
 * A stand-in for the real repository: no Room, no network. A test sets
 * nextStatus to choose how the "server" answers, then checks what was sent.
 */
public class FakeStudentRepository implements StudentRepository {

    public Result.Status nextStatus = Result.Status.SUCCESS;

    public int registerCalls;
    public Student lastRegistered;

    /** Searches wait here until the test answers them, in any order it likes. */
    public final List<RepoCallback<StudentPage>> waitingSearches = new ArrayList<>();
    public final List<String> searchQueries = new ArrayList<>();

    public final MutableLiveData<Student> ownProfile = new MutableLiveData<>();
    public final MutableLiveData<List<GroupCount>> groupCounts = new MutableLiveData<>();

    @Override
    public void saveRegistrationDraft(Student draft, String claimCode,
                                      RepoCallback<Student> callback) {
        callback.onResult(Result.of(Result.Status.PENDING, draft));
    }

    @Override
    public void register(Student draft, String claimCode, String password,
                         RepoCallback<Student> callback) {
        registerCalls++;
        lastRegistered = draft;
        callback.onResult(Result.of(nextStatus, draft));
    }

    @Override
    public LiveData<Student> observeOwnProfile() {
        return ownProfile;
    }

    @Override
    public void updateOwnDetails(String name, String programme, RepoCallback<Student> callback) {
        callback.onResult(Result.of(nextStatus, null));
    }

    @Override
    public void requestGroupChange(String group, RepoCallback<Student> callback) {
        callback.onResult(Result.of(nextStatus, null));
    }

    @Override
    public void requestNumberCorrection(String proposedNumber, RepoCallback<Void> callback) {
        callback.onResult(Result.of(nextStatus, null));
    }

    @Override
    public LiveData<List<GroupCount>> observeGroupCounts() {
        return groupCounts;
    }

    @Override
    public void syncNow() {
    }

    @Override
    public void search(String query, String programme, String group, int page,
                       RepoCallback<StudentPage> callback) {
        searchQueries.add(query);
        waitingSearches.add(callback);
    }

    @Override
    public void getStudent(String studentId, RepoCallback<Student> callback) {
        callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
    }

    @Override
    public void createStudent(Student student, RepoCallback<Student> callback) {
        callback.onResult(Result.of(nextStatus, student));
    }

    @Override
    public void updateStudent(Student student, RepoCallback<Student> callback) {
        callback.onResult(Result.of(nextStatus, student));
    }

    @Override
    public void assignGroup(String studentId, String group, RepoCallback<Student> callback) {
        callback.onResult(Result.of(nextStatus, null));
    }

    @Override
    public void deleteStudent(String studentId, RepoCallback<Void> callback) {
        callback.onResult(Result.of(nextStatus, null));
    }
}
