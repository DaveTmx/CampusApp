package com.example.campusapp.Data.repository;

import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Stand-in for RealStudentRepository while the backend is not ready.
 * Adds 700 ms of delay and really cancels superseded requests, so loading
 * and stale-reply behaviour can be tested on the device.
 */
public class FakeStudentRosterRepository implements StudentRosterRepository {

    private static final long DELAY_MS = 700;

    private final AppExecutors executors;
    private final ExecutorService worker = Executors.newCachedThreadPool();
    private final Map<Long, Future<?>> inFlight = new ConcurrentHashMap<>();
    private final List<Student> all = new ArrayList<>();

    public FakeStudentRosterRepository(AppExecutors executors) {
        this.executors = executors;
        String[] names = {"Chileshe Banda", "Mwansa Phiri", "Bwalya Tembo", "Chanda Mwila",
                "Natasha Zulu", "Kondwani Mumba", "Lweendo Siame", "Given Chirwa"};
        String[] programmes = {"CS", "IT", "DS"};
        String[] groups = {"G01", "G02", "G03", "G04", null};
        for (int i = 0; i < 48; i++) {
            Student s = new Student();
            s.studentId = "id_" + i;
            s.studentNumber = String.format(Locale.ROOT, "%09d", 100000000 + i);
            s.name = names[i % names.length];
            s.programme = programmes[i % programmes.length];
            s.confirmedGroup = groups[i % groups.length];
            s.syncStatus = Student.SYNCED;
            all.add(s);
        }
    }

    @Override
    public void loadPage(long requestId, String query, int page, int pageSize, Callback callback) {
        final String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Future<?> task = worker.submit(() -> {
            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                return; // cancelled: send nothing
            }
            StudentPage data = filterPage(q, page, pageSize);
            inFlight.remove(requestId);
            executors.runOnMain(() ->
                    callback.onResult(requestId, Result.of(Result.Status.SUCCESS, data)));
        });
        inFlight.put(requestId, task);
    }

    @Override
    public void cancel(long requestId) {
        Future<?> task = inFlight.remove(requestId);
        if (task != null) {
            task.cancel(true);
        }
    }

    private StudentPage filterPage(String q, int page, int pageSize) {
        List<Student> matches = new ArrayList<>();
        for (Student s : all) {
            if (q.isEmpty() || s.name.toLowerCase(Locale.ROOT).contains(q)
                    || s.studentNumber.contains(q)) {
                matches.add(s);
            }
        }
        int from = Math.min((page - 1) * pageSize, matches.size());
        int to = Math.min(from + pageSize, matches.size());
        return new StudentPage(new ArrayList<>(matches.subList(from, to)), to < matches.size(), false);
    }
}
