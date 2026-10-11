package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.data.repository.StudentRepository;
import com.example.campusapp.R;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.Event;
import com.example.campusapp.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State for the lecturer's roster: search text, two filters, paging and
 * group totals. The search text and filters live in SavedStateHandle so they
 * survive rotation and process death.
 */
public class RosterViewModel extends ViewModel {

    private static final String KEY_QUERY = "query";
    private static final String KEY_PROGRAMME = "programme";
    private static final String KEY_GROUP = "group";

    private final StudentRepository repository;
    private final SavedStateHandle state;
    private final LiveData<List<GroupCount>> groupCounts;

    private final MutableLiveData<List<Student>> students =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> fromCache = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();

    // Every search gets the next number. A response is used only if its number
    // is still the newest, so a slow answer to an old search cannot replace
    // the results of the search the lecturer typed after it.
    private final AtomicInteger latestRequest = new AtomicInteger();

    // The rows loaded so far. Results arrive on a background thread, so every
    // read and write of these two fields happens while holding the lock.
    private final Object lock = new Object();
    private final List<Student> loaded = new ArrayList<>();
    private int nextPage;

    public RosterViewModel(StudentRepository repository, SavedStateHandle state) {
        this.repository = repository;
        this.state = state;
        this.groupCounts = repository.observeGroupCounts();
        load(false);
    }

    // ----- What the screen shows -----

    public LiveData<String> getQuery() {
        return state.getLiveData(KEY_QUERY, "");
    }

    /** CS, IT, DS, or null for any programme. */
    public LiveData<String> getProgrammeFilter() {
        return state.getLiveData(KEY_PROGRAMME);
    }

    /** G01..G04, StudentRepository.UNASSIGNED, or null for any group. */
    public LiveData<String> getGroupFilter() {
        return state.getLiveData(KEY_GROUP);
    }

    /** An empty list while loading is false is the "no students found" state. */
    public LiveData<List<Student>> getStudents() {
        return students;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }

    /** True when offline, so the screen must label the list as limited cached results. */
    public LiveData<Boolean> getFromCache() {
        return fromCache;
    }

    public LiveData<List<GroupCount>> getGroupCounts() {
        return groupCounts;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    // ----- Actions -----

    public void setQuery(String query) {
        String cleaned = Validation.clean(query);
        // After rotation the search box sets its restored text again. Without
        // this check that would throw away the list and reload it.
        String current = state.get(KEY_QUERY);
        if (cleaned.equals(Validation.clean(current))) {
            return;
        }
        state.set(KEY_QUERY, cleaned);
        load(false);
    }

    public void setProgrammeFilter(String programme) {
        if (Objects.equals(programme, state.get(KEY_PROGRAMME))) {
            return;
        }
        state.set(KEY_PROGRAMME, programme);
        load(false);
    }

    public void setGroupFilter(String group) {
        if (Objects.equals(group, state.get(KEY_GROUP))) {
            return;
        }
        state.set(KEY_GROUP, group);
        load(false);
    }

    /** Pull-to-refresh, or coming back from the editor. */
    public void refresh() {
        load(false);
    }

    /** Call when the list is scrolled near its end. */
    public void loadNextPage() {
        if (Boolean.TRUE.equals(loading.getValue()) || !Boolean.TRUE.equals(hasMore.getValue())) {
            return;
        }
        load(true);
    }

    /** Call only after the lecturer has confirmed the dialog; Cancel must not reach here. */
    public void delete(String studentId) {
        repository.deleteStudent(studentId, result -> {
            if (!result.isSaved()) {
                message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
                return;
            }
            List<Student> snapshot;
            synchronized (lock) {
                for (int i = loaded.size() - 1; i >= 0; i--) {
                    if (studentId.equals(loaded.get(i).studentId)) {
                        loaded.remove(i);
                    }
                }
                snapshot = new ArrayList<>(loaded);
            }
            students.postValue(snapshot);
            message.postValue(new Event<>(R.string.msg_deleted));
        });
    }

    public void syncNow() {
        repository.syncNow();
    }

    /**
     * Text for the Android Sharesheet: group labels and counts only, with no
     * names or student numbers. The screen must be observing getGroupCounts()
     * for the totals to be loaded. Returns "" when there is nothing to share.
     */
    public String buildShareSummary() {
        List<GroupCount> counts = groupCounts.getValue();
        if (counts == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (GroupCount c : counts) {
            text.append(c.group).append(": ")
                    .append(c.count).append('/').append(GroupCount.CAPACITY).append('\n');
        }
        return text.toString().trim();
    }

    private void load(boolean append) {
        final int requestId = latestRequest.incrementAndGet();
        final int page;
        synchronized (lock) {
            page = append ? nextPage : 0;
        }
        String query = state.get(KEY_QUERY);
        String programme = state.get(KEY_PROGRAMME);
        String group = state.get(KEY_GROUP);

        loading.setValue(true);
        repository.search(Validation.clean(query), programme, group, page, result -> {
            if (requestId != latestRequest.get()) {
                return; // a newer search has started; ignore this old answer
            }
            loading.postValue(false);
            if (result.data == null) {
                message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
                return;
            }
            List<Student> snapshot;
            synchronized (lock) {
                if (!append) {
                    loaded.clear();
                }
                loaded.addAll(result.data.students);
                nextPage = page + 1;
                snapshot = new ArrayList<>(loaded);
            }
            students.postValue(snapshot);
            hasMore.postValue(result.data.hasMore);
            fromCache.postValue(result.data.fromCache);
        });
    }
}
