package com.mulungushi.campusapp.ui.lecturer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.mulungushi.campusapp.data.model.Student;
import com.mulungushi.campusapp.data.model.StudentPageResponse;
import com.mulungushi.campusapp.data.repository.FakeStudentRepository;
import com.mulungushi.campusapp.data.repository.StudentRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the Lecturer Roster screen (search + filter chips + pagination).
 * Swap FakeStudentRepository for RealStudentRepository once the backend is
 * ready — nothing else in this class, or in the Fragment/Activity observing
 * it, needs to change, because both implement the same StudentRepository
 * contract.
 */
public class LecturerRosterViewModel extends ViewModel {

    private static final int PAGE_SIZE = 20;

    // TODO(integration): replace with RealStudentRepository(apiService) when the
    // backend endpoint is live. Everything below stays the same.
    private final StudentRepository repository = new FakeStudentRepository();

    private final MediatorLiveData<List<Student>> visibleStudents = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> isLoading = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> isOffline = new MediatorLiveData<>();

    private final List<Student> accumulated = new ArrayList<>();
    private String currentQuery = "";
    private int currentPage = 1;
    private boolean hasMorePages = true;

    // The id of the most recent request WE made. Even though the repository
    // also cancels the previous in-flight task, we keep this as a second,
    // independent guard — defence in depth against any stale response.
    private long latestRequestId = 0;

    public LecturerRosterViewModel() {
        isLoading.setValue(false);
        isOffline.setValue(false);
        loadFirstPage("");
    }

    public LiveData<List<Student>> getVisibleStudents() { return visibleStudents; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsOffline() { return isOffline; }

    /** Called on every keystroke in the search box. */
    public void onSearchTextChanged(String query) {
        loadFirstPage(query);
    }

    /** Called when the lecturer scrolls near the bottom of the roster. */
    public void onLoadNextPageRequested() {
        if (!hasMorePages || Boolean.TRUE.equals(isLoading.getValue())) {
            return; // already loading, or nothing more to fetch
        }
        fetchPage(currentQuery, currentPage + 1, /* isNewSearch= */ false);
    }

    private void loadFirstPage(String query) {
        currentQuery = query;
        accumulated.clear();
        visibleStudents.setValue(new ArrayList<>());
        fetchPage(query, 1, /* isNewSearch= */ true);
    }

    private void fetchPage(String query, int page, boolean isNewSearch) {
        isLoading.setValue(true);

        LiveData<StudentPageResponse> source = repository.getStudents(query, page, PAGE_SIZE);

        visibleStudents.addSource(source, response -> {
            // Guard: the repository's requestId only ever increases. If we've
            // already accepted a response with an equal or higher id, this
            // one is stale (it was in flight before a newer search/page
            // request superseded it) — drop it instead of updating the UI.
            if (response.getRequestId() <= latestRequestId) {
                visibleStudents.removeSource(source);
                return;
            }
            latestRequestId = response.getRequestId();

            isLoading.setValue(false);
            currentPage = response.getPage();
            hasMorePages = response.hasMore();

            if (isNewSearch) {
                accumulated.clear();
            }
            accumulated.addAll(response.getStudents());
            visibleStudents.setValue(new ArrayList<>(accumulated));
            visibleStudents.removeSource(source);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancelPendingRequest();
    }
}