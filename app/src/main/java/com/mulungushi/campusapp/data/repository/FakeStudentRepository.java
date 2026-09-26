package com.mulungushi.campusapp.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mulungushi.campusapp.data.model.Student;
import com.mulungushi.campusapp.data.model.StudentPageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stands in for the real Retrofit/Room-backed repository so the
 * Testing/Integration team can build pagination, search, and stale-response
 * handling before the backend exists.
 * Two things make this a faithful stand-in rather than just dummy data:
 *  1. It runs off the main thread and adds artificial latency, so loading
 *     states are real, not instant.
 *  2. Starting a new search actually cancels the previous one in flight
 *     (not just "ignores the old result later") — the same behaviour the
 *     real Retrofit Call.cancel() gives you.
 */
public class FakeStudentRepository implements StudentRepository {

    private static final int SIMULATED_NETWORK_DELAY_MS = 700;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final List<Student> allStudents = generateFakeStudents();

    private volatile Future<?> currentTask;

    @Override
    public LiveData<StudentPageResponse> getStudents(String query, int page, int pageSize) {
        // Cancel whatever search/page request is still running — this is the
        // fix for Activity D's "cancel stale search responses" requirement.
        cancelPendingRequest();

        long myRequestId = requestCounter.incrementAndGet();
        MutableLiveData<StudentPageResponse> result = new MutableLiveData<>();

        currentTask = executor.submit(() -> {
            try {
                Thread.sleep(SIMULATED_NETWORK_DELAY_MS); // pretend this is a network round-trip
            } catch (InterruptedException e) {
                return; // cancelled — post nothing, the caller no longer wants this response
            }

            List<Student> filtered = filter(query);
            int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
            int toIndex = Math.min(fromIndex + pageSize, filtered.size());
            List<Student> pageItems = new ArrayList<>(filtered.subList(fromIndex, toIndex));
            boolean hasMore = toIndex < filtered.size();

            result.postValue(new StudentPageResponse(myRequestId, pageItems, page, hasMore, false, null));
        });

        return result;
    }

    @Override
    public void cancelPendingRequest() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }

    private List<Student> filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            return allStudents;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Student> matches = new ArrayList<>();
        for (Student s : allStudents) {
            if (s.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                    || s.getStudentNumber().contains(needle)) {
                matches.add(s);
            }
        }
        return matches;
    }

    @NonNull
    private static List<Student> generateFakeStudents() {
        String[] names = {
                "Chileshe Banda", "Mwansa Phiri", "Bwalya Tembo", "Chanda Mwila",
                "Natasha Zulu", "Kondwani Mumba", "Lweendo Siame", "Given Chirwa",
                "Mutinta Sakala", "Emmanuel Sinkala", "Ruth Kabwe", "Joseph Lungu",
        };
        String[] programmes = {"CS", "IT", "DS"};
        String[] groups = {"G01", "G02", "G03", "G04", "UNASSIGNED"};

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            String number = String.format(Locale.ROOT, "%09d", 100000000 + i);
            String name = names[i % names.length];
            String programme = programmes[i % programmes.length];
            String group = groups[i % groups.length];
            students.add(new Student("id_" + i, number, name, programme, group));
        }
        return students;
    }
}