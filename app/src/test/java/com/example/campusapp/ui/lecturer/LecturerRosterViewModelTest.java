package com.example.campusapp.ui.lecturer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.campusapp.Data.repository.StudentRosterRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.Debouncer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for LecturerRosterViewModel. No device and no network: the
 * repository and the debounce timer are handwritten doubles, so each test
 * decides exactly when a reply arrives or time passes.
 */
public class LecturerRosterViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutor = new InstantTaskExecutorRule();

    private ManualRepository repository;
    private ManualScheduler scheduler;
    private LecturerRosterViewModel viewModel;

    @Before
    public void setUp() {
        repository = new ManualRepository();
        scheduler = new ManualScheduler();
        viewModel = new LecturerRosterViewModel(repository, new Debouncer(scheduler, 300));
    }

    // 1. A fresh ViewModel shows LOADING, then SUCCESS when the first page arrives.
    @Test
    public void initialLoadShowsLoadingThenSuccess() {
        assertEquals(RosterUiState.Status.LOADING, state().status);
        assertEquals(1, repository.requests.size());

        repository.complete(0, page(student("1", "Chileshe Banda")));

        assertEquals(RosterUiState.Status.SUCCESS, state().status);
        assertEquals(1, state().students.size());
    }

    // 2. A search with no matches shows EMPTY, not an empty SUCCESS list.
    @Test
    public void emptyResultShowsEmpty() {
        repository.complete(0, page());

        assertEquals(RosterUiState.Status.EMPTY, state().status);
        assertTrue(state().students.isEmpty());
    }

    // 3. Cached results served while offline are labelled OFFLINE.
    @Test
    public void offlineResultShowsOfflineWithCachedStudents() {
        repository.complete(0, offlinePage(student("1", "Mwansa Phiri")));

        assertEquals(RosterUiState.Status.OFFLINE, state().status);
        assertEquals(1, state().students.size());
    }

    // 4. An expired session is reported so the UI can show the sign-in prompt.
    @Test
    public void sessionExpiredIsReported() {
        repository.fail(0, Result.Status.SESSION_EXPIRED);

        assertEquals(RosterUiState.Status.SESSION_EXPIRED, state().status);
    }

    // 5. A slow reply for an old search must not overwrite the newer search.
    @Test
    public void staleResponseIsIgnored() {
        viewModel.onQueryChanged("bwalya");
        scheduler.runAll();                       // starts request for "bwalya"
        viewModel.onQueryChanged("natasha");
        scheduler.runAll();                       // starts request for "natasha"

        // The newer search answers first.
        repository.complete(2, page(student("2", "Natasha Zulu")));
        // The older search answers late. Its reply must be dropped.
        repository.complete(1, page(student("3", "Bwalya Tembo")));

        RosterUiState current = state();
        assertEquals("natasha", current.query);
        assertEquals(1, current.students.size());
        assertEquals("Natasha Zulu", current.students.get(0).name);
    }

    // 6. Starting a new search cancels the one still in flight.
    @Test
    public void newSearchCancelsPreviousRequest() {
        long firstId = repository.requests.get(0).requestId;

        viewModel.onQueryChanged("zulu");
        scheduler.runAll();

        assertTrue(repository.cancelled.contains(firstId));
    }

    // 7. Typing quickly produces one search for the final text, not one per key.
    @Test
    public void debounceCollapsesKeystrokesIntoOneSearch() {
        repository.complete(0, page());
        int before = repository.requests.size();

        viewModel.onQueryChanged("c");
        viewModel.onQueryChanged("ch");
        viewModel.onQueryChanged("chi");
        assertEquals(before, repository.requests.size());   // nothing sent yet

        scheduler.runAll();

        assertEquals(before + 1, repository.requests.size());
        assertEquals("chi", repository.requests.get(before).query);
    }

    // 8. Next-page requests append results, and a second request is ignored while one is loading.
    @Test
    public void nextPageAppendsAndIgnoresRepeatedRequests() {
        repository.complete(0, pageWithMore(student("1", "A One"), student("2", "A Two")));
        assertTrue(state().hasMore);

        viewModel.onLoadNextPage();
        viewModel.onLoadNextPage();              // ignored: page 2 is already loading
        assertTrue(state().loadingMore);
        assertEquals(2, repository.requests.size());

        repository.complete(1, page(student("3", "A Three")));

        RosterUiState current = state();
        assertEquals(3, current.students.size());
        assertFalse(current.loadingMore);
        assertFalse(current.hasMore);
    }

    // ---- helpers ---------------------------------------------------------

    private static Result<StudentPage> pageOf(boolean hasMore, String... names) {
        List<Student> students = new ArrayList<>();
        for (String name : names) {
            Student s = new Student();
            s.studentId = name;
            s.name = name;
            students.add(s);
        }
        return Result.of(Result.Status.SUCCESS, new StudentPage(students, hasMore, false));
    }
    private RosterUiState state() {
        return viewModel.getState().getValue();
    }

    private static Student student(String id, String name) {
        Student s = new Student();
        s.studentId = id;
        s.name = name;
        s.studentNumber = "100000" + id;
        s.programme = "CS";
        s.syncStatus = Student.SYNCED;
        return s;
    }

    private static Result<StudentPage> page(Student... students) {
        return Result.of(Result.Status.SUCCESS,
                new StudentPage(new ArrayList<>(Arrays.asList(students)), false, false));
    }

    private static Result<StudentPage> pageWithMore(Student... students) {
        return Result.of(Result.Status.SUCCESS,
                new StudentPage(new ArrayList<>(Arrays.asList(students)), true, false));
    }

    private static Result<StudentPage> offlinePage(Student... students) {
        return Result.of(Result.Status.OFFLINE,
                new StudentPage(new ArrayList<>(Arrays.asList(students)), false, true));
    }

    /** Records requests and lets the test choose when (and whether) each one answers. */
    static class ManualRepository implements StudentRosterRepository {

        static class Request {
            final long requestId;
            final String query;
            final int page;
            final Callback callback;

            Request(long requestId, String query, int page, Callback callback) {
                this.requestId = requestId;
                this.query = query;
                this.page = page;
                this.callback = callback;
            }
        }

        final List<Request> requests = new ArrayList<>();
        final List<Long> cancelled = new ArrayList<>();

        @Override
        public void loadPage(long requestId, String query, int page, int pageSize, Callback callback) {
            requests.add(new Request(requestId, query, page, callback));
        }

        @Override
        public void cancel(long requestId) {
            cancelled.add(requestId);
        }

        /** Delivers a result for the request at the given index in the list. */
        void complete(int index, Result<StudentPage> result) {
            Request r = requests.get(index);
            r.callback.onResult(r.requestId, result);
        }

        void fail(int index, Result.Status status) {
            Request r = requests.get(index);
            r.callback.onResult(r.requestId, Result.of(status, null));
        }
    }

    /** Holds posted tasks until the test runs them. */
    static class ManualScheduler implements Debouncer.Scheduler {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void postDelayed(Runnable task, long delayMs) {
            tasks.add(task);
        }

        @Override
        public void cancel(Runnable task) {
            tasks.remove(task);
        }

        void runAll() {
            List<Runnable> copy = new ArrayList<>(tasks);
            tasks.clear();
            for (Runnable r : copy) {
                r.run();
            }
        }
    }
}
