package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.data.repository.StudentRepository;
import com.example.campusapp.R;
import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.Event;
import com.example.campusapp.util.Validation;

import java.util.List;

/**
 * State for the student's own profile screen.
 *
 * The profile comes straight from Room through the repository, so it is still
 * there after a restart and refreshes itself when a sync finishes. The screen
 * shows Student.confirmedGroup and Student.pendingGroup as two separate things.
 */
public class ProfileViewModel extends ViewModel {

    private final StudentRepository repository;
    private final LiveData<Student> profile;
    private final LiveData<List<GroupCount>> groupCounts;

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> nameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> programmeError = new MutableLiveData<>();
    private final MutableLiveData<Integer> numberError = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();

    public ProfileViewModel(StudentRepository repository) {
        this.repository = repository;
        this.profile = repository.observeOwnProfile();
        this.groupCounts = repository.observeGroupCounts();
    }

    public LiveData<Student> getProfile() {
        return profile;
    }

    /** Occupancy of every group, for example G01 14/15. */
    public LiveData<List<GroupCount>> getGroupCounts() {
        return groupCounts;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public LiveData<Integer> getNameError() {
        return nameError;
    }

    public LiveData<Integer> getProgrammeError() {
        return programmeError;
    }

    public LiveData<Integer> getNumberError() {
        return numberError;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    /** A student may change only their own name and programme. */
    public void updateDetails(String name, String programme) {
        Integer nameProblem = Validation.nameError(name);
        Integer programmeProblem = Validation.programmeError(programme);
        nameError.setValue(nameProblem);
        programmeError.setValue(programmeProblem);
        if (nameProblem != null || programmeProblem != null) {
            return;
        }

        busy.setValue(true);
        repository.updateOwnDetails(Validation.clean(name), programme, this::report);
    }

    /**
     * Asks for a different group. If it is full the repository answers
     * GROUP_FULL and the confirmed group stays as it was.
     */
    public void requestGroupChange(String group) {
        if (group == null || group.isEmpty() || Validation.groupError(group) != null) {
            message.setValue(new Event<>(R.string.error_group_invalid));
            return;
        }
        busy.setValue(true);
        repository.requestGroupChange(group, this::report);
    }

    /** Sends the lecturer a request; the student number itself is not changed here. */
    public void requestNumberCorrection(String proposedNumber) {
        Integer problem = Validation.numberError(proposedNumber);
        numberError.setValue(problem);
        if (problem != null) {
            return;
        }

        busy.setValue(true);
        repository.requestNumberCorrection(Validation.clean(proposedNumber), result -> {
            busy.postValue(false);
            message.postValue(new Event<>(result.isSaved()
                    ? R.string.msg_correction_requested
                    : StatusMessages.forStatus(result.status)));
        });
    }

    public void syncNow() {
        repository.syncNow();
    }

    private void report(Result<Student> result) {
        busy.postValue(false);
        message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
    }
}
