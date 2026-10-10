package com.example.campusapp.viewmodel;

import androidx.datastore.core.Data;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.Data.repository.RepoCallback;
import com.example.campusapp.Data.repository.StudentRepository;
import com.example.campusapp.R;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.Event;
import com.example.campusapp.util.Validation;

import java.util.Objects;

/**
 * State for the lecturer's add/edit student screen.
 *
 * Start the Activity with intent.putExtra(EditorViewModel.EXTRA_STUDENT_ID, id)
 * to edit, or with no extra to add. SavedStateHandle is filled from the intent
 * extras automatically, which is how the ID arrives here.
 */
public class EditorViewModel extends ViewModel {

    public static final String EXTRA_STUDENT_ID = "student_id";

    private static final String KEY_NAME = "name";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_PROGRAMME = "programme";
    private static final String KEY_GROUP = "group";
    private static final String KEY_BASE_VERSION = "base_version";
    private static final String KEY_LOADED = "loaded";

    private final StudentRepository repository;
    private final SavedStateHandle state;

    private final MutableLiveData<Integer> nameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> numberError = new MutableLiveData<>();
    private final MutableLiveData<Integer> programmeError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Student> conflict = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();

    public EditorViewModel(StudentRepository repository, SavedStateHandle state) {
        this.repository = repository;
        this.state = state;
        // Load once. After rotation or process death the lecturer's edits are
        // already in the saved state and must not be replaced by a fresh load.
        if (!isNewStudent() && !Boolean.TRUE.equals(state.get(KEY_LOADED))) {
            load();
        }
    }

    public boolean isNewStudent() {
        return state.get(EXTRA_STUDENT_ID) == null;
    }

    // ----- What the user typed -----

    public LiveData<String> getName() {
        return state.getLiveData(KEY_NAME, "");
    }

    public LiveData<String> getNumber() {
        return state.getLiveData(KEY_NUMBER, "");
    }

    public LiveData<String> getProgramme() {
        return state.getLiveData(KEY_PROGRAMME, "");
    }

    /** "" means Unassigned. */
    public LiveData<String> getGroup() {
        return state.getLiveData(KEY_GROUP, "");
    }

    public void setName(String value) {
        state.set(KEY_NAME, value);
    }

    public void setNumber(String value) {
        state.set(KEY_NUMBER, value);
    }

    public void setProgramme(String value) {
        state.set(KEY_PROGRAMME, value);
    }

    public void setGroup(String value) {
        state.set(KEY_GROUP, value);
    }

    // ----- What the screen shows -----

    public LiveData<Integer> getNameError() {
        return nameError;
    }

    public LiveData<Integer> getNumberError() {
        return numberError;
    }

    public LiveData<Integer> getProgrammeError() {
        return programmeError;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    /**
     * Null normally. After a conflicting edit it holds the record as the server
     * has it now, to show beside the lecturer's own unsaved values.
     */
    public LiveData<Student> getConflict() {
        return conflict;
    }

    /** Becomes true when the screen should close (saved or deleted). */
    public LiveData<Boolean> getFinished() {
        return finished;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    // ----- Actions -----

    public void save() {
        if (Boolean.TRUE.equals(busy.getValue()) || !validate()) {
            return;
        }

        Student proposal = new Student();
        proposal.studentId = state.get(EXTRA_STUDENT_ID);
        proposal.name = text(KEY_NAME);
        proposal.studentNumber = text(KEY_NUMBER);
        proposal.programme = text(KEY_PROGRAMME);
        Integer base = state.get(KEY_BASE_VERSION);
        // The version this edit started from. The server compares it with its
        // own, and that comparison is what detects a conflict.
        proposal.version = base == null ? 0 : base;

        String chosen = text(KEY_GROUP);
        final String wantedGroup = chosen.isEmpty() ? null : chosen;

        busy.setValue(true);
        RepoCallback<Student> afterProfile = result -> onProfileSaved(result, wantedGroup);
        if (proposal.studentId == null) {
            repository.createStudent(proposal, afterProfile);
        } else {
            repository.updateStudent(proposal, afterProfile);
        }
    }

    /** Conflict choice 1: drop my edits and show what the server has. */
    public void useServerVersion() {
        Student server = conflict.getValue();
        if (server != null) {
            fill(server);
            conflict.setValue(null);
        }
    }

    /**
     * Conflict choice 2: I have seen the server's record and still want my
     * values. Rebasing onto the server's version lets the next save() pass.
     * This is a reviewed decision, not a silent overwrite.
     */
    public void keepMyChanges() {
        Student server = conflict.getValue();
        if (server != null) {
            state.set(KEY_BASE_VERSION, server.version);
            conflict.setValue(null);
            save();
        }
    }

    /** Call only after the lecturer has confirmed the dialog. */
    public void delete() {
        String id = state.get(EXTRA_STUDENT_ID);
        if (id == null || Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        repository.deleteStudent(id, result -> {
            busy.postValue(false);
            if (result.isSaved() || result.status == Result.Status.NOT_FOUND) {
                message.postValue(new Event<>(R.string.msg_deleted));
                finished.postValue(true);
            } else {
                message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
            }
        });
    }

    // ----- Internals -----

    private void load() {
        String id = state.get(EXTRA_STUDENT_ID);
        busy.setValue(true);
        repository.getStudent(id, result -> {
            busy.postValue(false);
            if (result.Data == null) {
                message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
                if (result.status == Result.Status.NOT_FOUND) {
                    finished.postValue(true);
                }
                return;
            }
            fill(result.data);
        });
    }

    /** Step 1 of saving is the profile. A group is requested only after the profile exists. */
    private void onProfileSaved(Result<Student> result, String wantedGroup) {
        if (result.status == Result.Status.CONFLICT) {
            // The fields are not touched, so the lecturer's proposal is kept.
            busy.postValue(false);
            conflict.postValue(result.serverCopy);
            message.postValue(new Event<>(R.string.msg_conflict));
            return;
        }
        if (result.status == Result.Status.DUPLICATE_NUMBER) {
            busy.postValue(false);
            numberError.postValue(R.string.msg_duplicate_number);
            return;
        }
        if (!result.isSaved() || result.data == null) {
            busy.postValue(false);
            message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
            if (result.status == Result.Status.NOT_FOUND) {
                finished.postValue(true); // deleted elsewhere; never recreate it
            }
            return;
        }

        final Student saved = result.data;
        post(EXTRA_STUDENT_ID, saved.studentId);
        post(KEY_BASE_VERSION, saved.version);

        if (Objects.equals(wantedGroup, saved.confirmedGroup)) {
            done(result.status);
            return;
        }

        // Step 2: assign or transfer.
        repository.assignGroup(saved.studentId, wantedGroup, groupResult -> {
            if (groupResult.status == Result.Status.GROUP_FULL) {
                // The profile is saved, but the transfer failed: show the
                // group the student still has.
                busy.postValue(false);
                post(KEY_GROUP, saved.confirmedGroup == null ? "" : saved.confirmedGroup);
                message.postValue(new Event<>(R.string.msg_group_full));
            } else if (groupResult.isSaved()) {
                done(groupResult.status);
            } else {
                busy.postValue(false);
                message.postValue(new Event<>(StatusMessages.forStatus(groupResult.status)));
            }
        });
    }

    private void done(Result.Status status) {
        busy.postValue(false);
        message.postValue(new Event<>(StatusMessages.forStatus(status)));
        finished.postValue(true);
    }

    private boolean validate() {
        Integer name = Validation.nameError(text(KEY_NAME));
        Integer number = Validation.numberError(text(KEY_NUMBER));
        Integer programme = Validation.programmeError(text(KEY_PROGRAMME));
        nameError.setValue(name);
        numberError.setValue(number);
        programmeError.setValue(programme);
        if (Validation.groupError(text(KEY_GROUP)) != null) {
            message.setValue(new Event<>(R.string.error_group_invalid));
            return false;
        }
        return name == null && number == null && programme == null;
    }

    private void fill(Student s) {
        post(KEY_NAME, s.name);
        post(KEY_NUMBER, s.studentNumber);
        post(KEY_PROGRAMME, s.programme);
        post(KEY_GROUP, s.confirmedGroup == null ? "" : s.confirmedGroup);
        post(KEY_BASE_VERSION, s.version);
        post(KEY_LOADED, true);
    }

    /**
     * Writes a saved-state value from any thread. state.set() may only be
     * called on the main thread, and repository results arrive on a background
     * one; postValue() on the key's LiveData hands the write to the main thread.
     */
    private <T> void post(String key, T value) {
        state.<T>getLiveData(key).postValue(value);
    }

    private String text(String key) {
        String value = state.get(key);
        return Validation.clean(value);
    }
}
