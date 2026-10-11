package com.example.campusapp.ui.lecturer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.Data.repository.StudentRosterRepository;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;
import com.example.campusapp.util.Debouncer;
import com.example.campusapp.viewmodel.FakeStudentRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the lecturer roster: search, debounce, pagination and state.
 * Stale results are blocked two ways:
 *  1. Each request gets a new id; only the newest id may update the screen.
 *  2. The previous request is cancelled when a new one starts.
 * Only the main thread calls this class, so setValue() is safe.
 */
public class LecturerRosterViewModel extends ViewModel {

    public static final int PAGE_SIZE = 20;
    public static final long SEARCH_DEBOUNCE_MS = 300;

    private final StudentRosterRepository repository;
    private final Debouncer debouncer;
    private final MutableLiveData<RosterUiState> state = new MutableLiveData<>();

    private final List<Student> accumulated = new ArrayList<>();
    private RosterUiState.Status status = RosterUiState.Status.LOADING;
    private String query = "";
    private int nextPage = 1;
    private boolean hasMore = false;
    private boolean loadingMore = false;

    private long requestCounter = 0;
    /** The only request whose reply may update the screen. 0 = none. */
    private long activeRequestId = 0;

    public LecturerRosterViewModel(StudentRosterRepository repository, Debouncer debouncer) {
        this.repository = repository;
        this.debouncer = debouncer;
        startSearch("");
    }

    public LiveData<RosterUiState> getState() {
        return state;
    }

    /** Called on every keystroke. Searches only after typing pauses. */
    public void onQueryChanged(String text) {
        final String q = text == null ? "" : text.trim();
        debouncer.submit(() -> {
            if (!q.equals(query)) {
                startSearch(q);
            }
        });
    }

    /** Called when the list is near its end. Ignored while a page is loading. */
    public void onLoadNextPage() {
        if (!hasMore || loadingMore) {
            return;
        }
        loadingMore = true;
        publish(status);
        issue(query, nextPage);
    }

    /** Retries the current query after an error or lost session. */
    public void retry() {
        startSearch(query);
    }

    @Override
    protected void onCleared() {
        debouncer.cancelPending();
        repository.cancel(activeRequestId);
        super.onCleared();
    }

    private void startSearch(String q) {
        query = q;
        accumulated.clear();
        nextPage = 1;
        hasMore = false;
        loadingMore = false;
        publish(RosterUiState.Status.LOADING);
        issue(q, 1);
    }

    private void issue(String q, int page) {
        long previous = activeRequestId;
        long id = ++requestCounter;
        activeRequestId = id;
        if (previous != 0) {
            repository.cancel(previous);
        }
        repository.loadPage(id, q, page, PAGE_SIZE,
                (reqId, result) -> onResult(reqId, result, q, page));
    }

    private void onResult(long requestId, Result<StudentPage> result, String q, int page) {
        // Stale guard: a newer search or page request has started since this one.
        if (requestId != activeRequestId || !q.equals(query)) {
            return;
        }
        loadingMore = false;

        if (result.status == Result.Status.SESSION_EXPIRED) {
            publish(RosterUiState.Status.SESSION_EXPIRED);
            return;
        }
        if (result.status != Result.Status.SUCCESS && result.status != Result.Status.OFFLINE) {
            publish(RosterUiState.Status.ERROR);
            return;
        }

        StudentPage data = result.data;
        if (data != null) {
            accumulated.addAll(data.students);
            hasMore = data.hasMore;
        }
        nextPage = page + 1;

        if (result.status == Result.Status.OFFLINE) {
            publish(RosterUiState.Status.OFFLINE);
        } else if (accumulated.isEmpty()) {
            publish(RosterUiState.Status.EMPTY);
        } else {
            publish(RosterUiState.Status.SUCCESS);
        }
    }

    private void publish(RosterUiState.Status newStatus) {
        status = newStatus;
        state.setValue(new RosterUiState(
                status, new ArrayList<>(accumulated), hasMore, loadingMore, query));
    }

    public void setQuery(String s) {
    }

    public void loadNextPageIfNeeded() {
    }

    public void setFilters(Object o, String s) {
    }

    public LiveData<Object> getStudents() {
        return null;
    }

    public LiveData<LecturerRosterViewModel.UiState> getUiState() {
        return null;
    }

    public LiveData<Object> getFromCache() {
    }

    public static class Factory implements ViewModelProvider.Factory {
        public Factory(FakeStudentRepository fakeStudentRepository) {
        }
    }
}
