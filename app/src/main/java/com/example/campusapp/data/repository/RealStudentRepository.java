package com.example.campusapp.Data.repository;

import com.example.campusapp.Data.local.dao.StudentDao;
import com.example.campusapp.Data.local.entities.StudentEntity;
import com.example.campusapp.Data.remote.ApiService;
import com.example.campusapp.Data.remote.StudentPageDto;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.AppExecutors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Roster data from the real backend, cached in Room.
 *  - Online: calls the API, caches the rows, returns SUCCESS.
 *  - No connection: returns OFFLINE with matching cached rows.
 *  - 401: SESSION_EXPIRED. 403: NOT_ALLOWED. Other failures: ERROR.
 *  - A cancelled call (superseded search) is dropped without a callback.
 * Room work runs on the disk executor; results return on the main thread.
 */
public class RealStudentRepository implements StudentRosterRepository {

    private final ApiService api;
    private final StudentDao dao;
    private final AppExecutors executors;
    private final Map<Long, Call<StudentPageDto>> inFlight = new ConcurrentHashMap<>();

    public RealStudentRepository(ApiService api, StudentDao dao, AppExecutors executors) {
        this.api = api;
        this.dao = dao;
        this.executors = executors;
    }

    @Override
    public void loadPage(long requestId, String query, int page, int pageSize, Callback callback) {
        final String q = query == null ? "" : query.trim();
        Call<StudentPageDto> call = api.getStudents(q, page, pageSize);
        inFlight.put(requestId, call);

        call.enqueue(new retrofit2.Callback<StudentPageDto>() {
            @Override
            public void onResponse(Call<StudentPageDto> c, Response<StudentPageDto> response) {
                inFlight.remove(requestId);
                if (c.isCanceled()) {
                    return;
                }
                if (response.code() == 401) {
                    deliver(callback, requestId, Result.of(Result.Status.SESSION_EXPIRED, null));
                    return;
                }
                if (response.code() == 403) {
                    deliver(callback, requestId, Result.of(Result.Status.NOT_ALLOWED, null));
                    return;
                }
                final StudentPageDto body = response.body();
                if (!response.isSuccessful() || body == null || body.students == null) {
                    deliver(callback, requestId, Result.of(Result.Status.ERROR, null));
                    return;
                }
                executors.diskIO().execute(() -> dao.upsertAll(toEntities(body)));
                StudentPage pageData = new StudentPage(toModels(body), body.hasMore, false);
                deliver(callback, requestId, Result.of(Result.Status.SUCCESS, pageData));
            }

            @Override
            public void onFailure(Call<StudentPageDto> c, Throwable t) {
                inFlight.remove(requestId);
                if (c.isCanceled()) {
                    return; // superseded by a newer request; nothing to report
                }
                if (!(t instanceof IOException)) {
                    deliver(callback, requestId, Result.of(Result.Status.ERROR, null));
                    return;
                }
                // No connection: answer from Room, labelled as offline.
                executors.diskIO().execute(() -> {
                    int offset = (page - 1) * pageSize;
                    List<StudentEntity> rows = dao.searchCached(like(q), pageSize + 1, offset);
                    boolean hasMore = rows.size() > pageSize;
                    List<StudentEntity> visible = rows.subList(0, Math.min(rows.size(), pageSize));
                    StudentPage cached = new StudentPage(toModelsFromEntities(visible), hasMore, true);
                    deliver(callback, requestId, Result.of(Result.Status.OFFLINE, cached));
                });
            }
        });
    }

    @Override
    public void cancel(long requestId) {
        Call<StudentPageDto> call = inFlight.remove(requestId);
        if (call != null) {
            call.cancel();
        }
    }

    private void deliver(Callback callback, long requestId, Result<StudentPage> result) {
        executors.runOnMain(() -> callback.onResult(requestId, result));
    }

    private static String like(String q) {
        return "%" + q + "%";
    }

    private static List<Student> toModels(StudentPageDto body) {
        List<Student> out = new ArrayList<>();
        for (StudentPageDto.StudentDto d : body.students) {
            Student s = new Student();
            s.studentId = d.student_id;
            s.name = d.full_name;
            s.studentNumber = d.student_number;
            s.programme = d.programme;
            s.confirmedGroup = d.confirmed_group;
            s.syncStatus = Student.SYNCED;
            s.version = d.version;
            out.add(s);
        }
        return out;
    }

    private static List<StudentEntity> toEntities(StudentPageDto body) {
        long now = System.currentTimeMillis();
        List<StudentEntity> out = new ArrayList<>();
        for (StudentPageDto.StudentDto d : body.students) {
            StudentEntity e = new StudentEntity();
            e.studentId = d.student_id;
            e.studentNumber = d.student_number;
            e.fullName = d.full_name;
            e.programme = d.programme;
            e.confirmedGroup = d.confirmed_group;
            e.version = d.version;
            e.cachedAt = now;
            out.add(e);
        }
        return out;
    }

    private static List<Student> toModelsFromEntities(List<StudentEntity> rows) {
        List<Student> out = new ArrayList<>();
        for (StudentEntity e : rows) {
            Student s = new Student();
            s.studentId = e.studentId;
            s.name = e.fullName;
            s.studentNumber = e.studentNumber;
            s.programme = e.programme;
            s.confirmedGroup = e.confirmedGroup;
            s.syncStatus = Student.SYNCED;
            s.version = e.version;
            out.add(s);
        }
        return out;
    }
}
