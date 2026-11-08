package com.example.campusapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.campusapp.data.local.entities.OperationEntity;
import com.example.campusapp.data.remote.ApiService;
import com.example.campusapp.data.remote.StudentDtos;
import com.example.campusapp.data.remote.StudentPageDto;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Session;
import com.example.campusapp.model.Student;
import com.example.campusapp.data.sync.OutboxQueue;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.AppExecutors;
import com.example.campusapp.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lecturer student data from the backend: the roster, the editor, group placement and deletion.
 *
 * Student screens (own profile, registration, draft) are not on the backend yet, so those calls go to
 * the local repository that is passed in. Remove the delegation when they move.
 *
 * Callbacks may arrive on a background thread, so ViewModels answer with postValue().
 */
public class RemoteStudentRepository implements StudentRepository {

    private static final int PAGE_SIZE = 20;

    private final ApiService api;
    private final StudentRepository local;
    private final OutboxQueue outbox;
    private final Gson gson = new Gson();
    private final MediatorLiveData<List<GroupCount>> groupCounts = new MediatorLiveData<>();
    /** The first page of the last successful roster load. Shown, labelled as cached, when offline. */
    private volatile List<Student> lastRoster = new ArrayList<>();
    /** The signed-in student's own record, from the server. */
    private final MutableLiveData<Student> ownProfile = new MutableLiveData<>();

    /** Call from the main thread: the constructor observes the local counts. */
    public RemoteStudentRepository(ApiService api, StudentRepository local, OutboxQueue outbox) {
        this.api = api;
        this.local = local;
        this.outbox = outbox;
        groupCounts.setValue(new ArrayList<>());
        // Local counts stay on screen until the server answers, so the student registration screen still has numbers.
        groupCounts.addSource(local.observeGroupCounts(), groupCounts::setValue);
        refreshGroupCounts();
    }

    // ----- Lecturer: roster -----

    @Override
    public void search(String query, String programme, String group, int page,
                       RepoCallback<StudentPage> callback) {
        String q = query == null ? "" : query.trim();
        boolean unassigned = UNASSIGNED.equals(group);
        String groupCode = (group == null || unassigned) ? null : group;

        api.getStudents(q, programme, groupCode, unassigned ? Boolean.TRUE : null, page + 1, PAGE_SIZE)
                .enqueue(new Callback<StudentPageDto>() {
                    @Override
                    public void onResponse(Call<StudentPageDto> call, Response<StudentPageDto> response) {
                        StudentPageDto body = response.body();
                        if (!response.isSuccessful() || body == null || body.students == null) {
                            callback.onResult(Result.of(statusOf(response, parseError(response)), null));
                            return;
                        }
                        List<Student> students = new ArrayList<>();
                        for (StudentPageDto.StudentDto d : body.students) {
                            students.add(fromListItem(d));
                        }
                        // Room must not be queried on the main thread. Retrofit calls back on the main thread,
                        // so the queued-status lookup runs in the background and the result returns to the UI.
                        boolean more = body.hasMore;
                        if (page == 0) {
                            lastRoster = new ArrayList<>(students);
                        }
                        AppExecutors.get().diskIO().execute(() -> {
                            applyQueuedStatus(students);
                            outbox.flushIfNeeded();
                            AppExecutors.get().runOnMain(() -> callback.onResult(Result.of(Result.Status.SUCCESS,
                                    new StudentPage(students, more, false))));
                        });
                    }

                    @Override
                    public void onFailure(Call<StudentPageDto> call, Throwable t) {
                        if (t instanceof IOException && page == 0 && !lastRoster.isEmpty()) {
                            answerFromLastRoster(callback);
                            return;
                        }
                        callback.onResult(Result.of(offlineOrError(t), null));
                    }
                });
    }

    /**
     * Offline: answers with the last roster loaded from the server, with queued changes shown
     * as pending. The result is marked as cached so the screen can say it may be out of date.
     */
    private void answerFromLastRoster(RepoCallback<StudentPage> callback) {
        AppExecutors.get().diskIO().execute(() -> {
            List<Student> students = new ArrayList<>();
            for (Student s : lastRoster) {
                students.add(copyOf(s));
            }
            applyQueuedStatus(students);
            AppExecutors.get().runOnMain(() -> callback.onResult(
                    Result.of(Result.Status.OFFLINE, new StudentPage(students, false, true))));
        });
    }

    @Override
    public void getStudent(String studentId, RepoCallback<Student> callback) {
        api.getStudent(studentId).enqueue(new Callback<StudentDtos.RecordEnvelope>() {
            @Override
            public void onResponse(Call<StudentDtos.RecordEnvelope> call,
                                   Response<StudentDtos.RecordEnvelope> response) {
                StudentDtos.RecordEnvelope body = response.body();
                if (!response.isSuccessful() || body == null || body.student == null) {
                    callback.onResult(Result.of(statusOf(response, parseError(response)), null));
                    return;
                }
                callback.onResult(Result.of(Result.Status.SUCCESS, fromRecord(body.student)));
            }

            @Override
            public void onFailure(Call<StudentDtos.RecordEnvelope> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    // ----- Lecturer: editor -----

    @Override
    public void createStudent(Student student, RepoCallback<Student> callback) {
        StudentDtos.CreateRequest body = new StudentDtos.CreateRequest(
                trimmed(student.name), trimmed(student.studentNumber), student.programme);
        api.createStudent(body).enqueue(new Callback<StudentDtos.CreateResponse>() {
            @Override
            public void onResponse(Call<StudentDtos.CreateResponse> call,
                                   Response<StudentDtos.CreateResponse> response) {
                StudentDtos.CreateResponse reply = response.body();
                if (!response.isSuccessful() || reply == null || reply.student == null) {
                    callback.onResult(Result.of(statusOf(response, parseError(response)), null));
                    return;
                }
                callback.onResult(Result.of(Result.Status.SUCCESS, fromRecord(reply.student)));
            }

            @Override
            public void onFailure(Call<StudentDtos.CreateResponse> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    @Override
    public void updateStudent(Student student, RepoCallback<Student> callback) {
        StudentDtos.EditRequest body = new StudentDtos.EditRequest(
                student.version, trimmed(student.name), student.programme, trimmed(student.studentNumber));
        api.editStudent(student.studentId, body).enqueue(new Callback<StudentDtos.EditResponse>() {
            @Override
            public void onResponse(Call<StudentDtos.EditResponse> call,
                                   Response<StudentDtos.EditResponse> response) {
                StudentDtos.EditResponse reply = response.body();
                if (response.isSuccessful() && reply != null) {
                    Student saved = copyOf(student);
                    saved.name = trimmed(student.name);
                    saved.studentNumber = trimmed(student.studentNumber);
                    saved.version = reply.version;
                    callback.onResult(Result.of(Result.Status.SUCCESS, saved));
                    return;
                }
                StudentDtos.ErrorBody error = parseError(response);
                if (error != null && "CONFLICT".equals(error.error) && error.currentRecord != null) {
                    // The proposal is kept; the server's copy is shown so the lecturer can choose.
                    callback.onResult(Result.conflict(student, fromRecord(error.currentRecord)));
                    return;
                }
                callback.onResult(Result.of(statusOf(response, error), null));
            }

            @Override
            public void onFailure(Call<StudentDtos.EditResponse> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    @Override
    public void assignGroup(String studentId, String group, RepoCallback<Student> callback) {
        String opType = group == null ? OperationEntity.UNASSIGN_GROUP : OperationEntity.ASSIGN_GROUP;
        sendOperation(opType, studentId, group, status -> {
            if (status == Result.Status.SUCCESS || status == Result.Status.PENDING) {
                Student placed = new Student();
                placed.studentId = studentId;
                placed.confirmedGroup = group;
                placed.syncStatus = status == Result.Status.PENDING ? Student.PENDING : Student.SYNCED;
                callback.onResult(Result.of(status, placed));
            } else {
                callback.onResult(Result.of(status, null));
            }
        });
    }

    @Override
    public void deleteStudent(String studentId, RepoCallback<Void> callback) {
        sendOperation(OperationEntity.DELETE_STUDENT, studentId, null, status ->
                callback.onResult(Result.of(status, null)));
    }

    @Override
    public void dismissRefused(String studentId, Runnable done) {
        AppExecutors.get().diskIO().execute(() -> {
            outbox.discardFailed(studentId);
            AppExecutors.get().runOnMain(done);
        });
    }

    @Override
    public LiveData<List<GroupCount>> observeGroupCounts() {
        return groupCounts;
    }

    // ----- Student screens: still local -----

    @Override
    public void saveRegistrationDraft(Student draft, String email, RepoCallback<Student> callback) {
        local.saveRegistrationDraft(draft, email, callback);
    }

    @Override
    public void register(Student draft, String email, String password, RepoCallback<Student> callback) {
        local.register(draft, email, password, callback);
    }

    @Override
    public LiveData<Student> observeOwnProfile() {
        refreshOwnProfile();
        return ownProfile;
    }

    @Override
    public void updateOwnDetails(String name, String programme, RepoCallback<Student> callback) {
        Student current = ownProfile.getValue();
        if (current == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        if (!programme.equals(current.programme)) {
            // The server lets a student change their name only. A programme change goes through the lecturer.
            callback.onResult(Result.of(Result.Status.NOT_ALLOWED, current));
            return;
        }
        // Null programme and number are left out of the request, so the server only sees the name change.
        StudentDtos.EditRequest body = new StudentDtos.EditRequest(current.version, trimmed(name), null, null);
        api.editStudent(current.studentId, body).enqueue(new Callback<StudentDtos.EditResponse>() {
            @Override
            public void onResponse(Call<StudentDtos.EditResponse> call,
                                   Response<StudentDtos.EditResponse> response) {
                StudentDtos.EditResponse reply = response.body();
                if (response.isSuccessful() && reply != null) {
                    Student saved = copyOf(current);
                    saved.name = trimmed(name);
                    saved.version = reply.version;
                    ownProfile.postValue(saved);
                    callback.onResult(Result.of(Result.Status.SUCCESS, saved));
                    return;
                }
                callback.onResult(Result.of(statusOf(response, parseError(response)), null));
            }

            @Override
            public void onFailure(Call<StudentDtos.EditResponse> call, Throwable t) {
                callback.onResult(Result.of(offlineOrError(t), null));
            }
        });
    }

    @Override
    public void requestGroupChange(String group, RepoCallback<Student> callback) {
        Student current = ownProfile.getValue();
        sendStudentRequest("GROUP", group, result -> callback.onResult(Result.of(result.status, current)));
    }

    @Override
    public void requestNumberCorrection(String proposedNumber, RepoCallback<Void> callback) {
        sendStudentRequest("NUMBER", proposedNumber, result -> callback.onResult(Result.of(result.status, null)));
    }

    /** Sends a request to the server. It stays PENDING until a lecturer approves or rejects it. */
    private void sendStudentRequest(String type, String value, RepoCallback<Void> callback) {
        Student current = ownProfile.getValue();
        if (current == null) {
            callback.onResult(Result.of(Result.Status.NOT_FOUND, null));
            return;
        }
        api.createStudentRequest(current.studentId, new StudentDtos.RequestBody(type, value))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(Result.of(Result.Status.PENDING, null));
                            return;
                        }
                        callback.onResult(Result.of(statusOf(response, parseError(response)), null));
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        callback.onResult(Result.of(offlineOrError(t), null));
                    }
                });
    }

    @Override
    public void syncNow() {
        local.syncNow();
        refreshGroupCounts();
        refreshOwnProfile();
    }

    // ----- Internals -----

    private void refreshGroupCounts() {
        api.getGroups().enqueue(new Callback<StudentDtos.GroupsResponse>() {
            @Override
            public void onResponse(Call<StudentDtos.GroupsResponse> call,
                                   Response<StudentDtos.GroupsResponse> response) {
                StudentDtos.GroupsResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.data == null) {
                    return; // keep the counts already shown
                }
                List<GroupCount> counts = new ArrayList<>();
                for (StudentDtos.GroupRow row : body.data) {
                    counts.add(new GroupCount(row.group_code, row.occupied));
                }
                groupCounts.postValue(counts);
            }

            @Override
            public void onFailure(Call<StudentDtos.GroupsResponse> call, Throwable t) {
                // Offline or signed out: keep the counts already shown.
            }
        });
    }

    /** Receives the final status of one sent action. */
    private interface Outcome {
        void onDone(Result.Status status);
    }

    /**
     * Sends one lecturer action with a new operationId. If the server answers, its verdict is the
     * result (for example GROUP_FULL). If the phone cannot reach it, or the server is busy, the same
     * operationId is stored in the queue and the result is PENDING.
     */
    private void sendOperation(String opType, String studentId, String groupCode, Outcome done) {
        String operationId = UUID.randomUUID().toString();
        StudentDtos.OperationRequest body = new StudentDtos.OperationRequest(
                operationId, opType, new StudentDtos.Payload(studentId, groupCode));

        api.submitOperation(body).enqueue(new Callback<StudentDtos.OperationResponse>() {
            @Override
            public void onResponse(Call<StudentDtos.OperationResponse> call,
                                   Response<StudentDtos.OperationResponse> response) {
                if (response.isSuccessful()) {
                    refreshGroupCounts();
                    done.onDone(Result.Status.SUCCESS);
                    return;
                }
                if (response.code() >= 500) {
                    queue(operationId, opType, studentId, groupCode);
                    done.onDone(Result.Status.PENDING);
                    return;
                }
                StudentDtos.OperationResponse reply = parseOperationError(response);
                StudentDtos.ErrorBody asError = new StudentDtos.ErrorBody();
                asError.error = reply == null ? null : reply.error;
                done.onDone(statusOf(response, asError));
            }

            @Override
            public void onFailure(Call<StudentDtos.OperationResponse> call, Throwable t) {
                if (t instanceof IOException) {
                    queue(operationId, opType, studentId, groupCode);
                    done.onDone(Result.Status.PENDING);
                } else {
                    done.onDone(Result.Status.ERROR);
                }
            }
        });
    }

    private void queue(String operationId, String opType, String studentId, String groupCode) {
        AppExecutors.get().diskIO().execute(() ->
                outbox.add(OperationEntity.create(operationId, opType, studentId, groupCode)));
    }

    /** Marks rows that still have a queued action, so the roster can show it. */
    private void applyQueuedStatus(List<Student> students) {
        Map<String, String> queued = outbox.statusByStudent();
        for (Student s : students) {
            String status = queued.get(s.studentId);
            if (OperationEntity.FAILED.equals(status)) {
                s.syncStatus = Student.ACTION_REQUIRED;
            } else if (status != null) {
                s.syncStatus = Student.PENDING;
            }
        }
    }

    private StudentDtos.OperationResponse parseOperationError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            return gson.fromJson(response.errorBody().string(), StudentDtos.OperationResponse.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    /** Maps a failed reply to a status the screens already handle. */
    private static Result.Status statusOf(Response<?> response, StudentDtos.ErrorBody error) {
        String code = error == null || error.error == null ? "" : error.error;
        switch (code) {
            case "DUPLICATE_NUMBER":
                return Result.Status.DUPLICATE_NUMBER;
            case "GROUP_FULL":
                return Result.Status.GROUP_FULL;
            case "CONFLICT":
                return Result.Status.CONFLICT;
            case "VALIDATION_FAILED":
            case "NOTHING_TO_UPDATE":
                return Result.Status.INVALID;
            case "STUDENT_NOT_FOUND":
            case "STUDENT_DELETED":
                return Result.Status.NOT_FOUND;
            case "NOT_OWNER":
            case "FIELD_NOT_ALLOWED":
                return Result.Status.NOT_ALLOWED;
            default:
                break;
        }
        switch (response.code()) {
            case 401:
                return Result.Status.SESSION_EXPIRED;
            case 403:
                return Result.Status.NOT_ALLOWED;
            case 404:
            case 410:
                return Result.Status.NOT_FOUND;
            case 429:
                return Result.Status.RATE_LIMITED;
            default:
                return Result.Status.ERROR;
        }
    }

    private StudentDtos.ErrorBody parseError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            return gson.fromJson(response.errorBody().string(), StudentDtos.ErrorBody.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    private static Result.Status offlineOrError(Throwable t) {
        return t instanceof IOException ? Result.Status.OFFLINE : Result.Status.ERROR;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static Student fromListItem(StudentPageDto.StudentDto d) {
        Student s = new Student();
        s.studentId = d.student_id;
        s.name = d.full_name;
        s.studentNumber = d.student_number;
        s.programme = d.programme;
        s.confirmedGroup = d.confirmed_group;
        s.version = d.version;
        s.syncStatus = Student.SYNCED;
        return s;
    }

    /** Loads the signed-in student's own record. For students the account ID is the student ID. */
    private void refreshOwnProfile() {
        Session session = SessionManager.getCurrent();
        if (session == null || session.accountId == null || session.accountId.isEmpty()) {
            return;
        }
        getStudent(session.accountId, result -> {
            if (result.data != null) {
                ownProfile.postValue(result.data);
            }
        });
    }

    private static Student fromRecord(StudentDtos.RecordDto r) {
        Student s = new Student();
        s.studentId = r.studentId;
        s.name = r.studentName;
        s.studentNumber = r.studentNumber;
        s.programme = r.programme;
        s.confirmedGroup = r.groupCode;
        s.version = r.version;
        s.syncStatus = Student.SYNCED;
        return s;
    }

    private static Student copyOf(Student source) {
        Student s = new Student();
        s.studentId = source.studentId;
        s.name = source.name;
        s.studentNumber = source.studentNumber;
        s.programme = source.programme;
        s.confirmedGroup = source.confirmedGroup;
        s.pendingGroup = source.pendingGroup;
        s.version = source.version;
        s.syncStatus = Student.SYNCED;
        return s;
    }
}
