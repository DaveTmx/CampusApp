package com.example.campusapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.campusapp.Data.repository.StudentRepository;
import com.example.campusapp.R;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.util.Event;
import com.example.campusapp.util.Validation;

/**
 * State for the student registration form.
 *
 * Typed text is kept in SavedStateHandle so it survives rotation and the
 * system killing the app in the background. The password is the exception:
 * it is never saved on the phone, so it is only passed to submit().
 */
public class RegisterViewModel extends ViewModel {

    private static final String KEY_NAME = "name";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_PROGRAMME = "programme";
    private static final String KEY_GROUP = "group";
    private static final String KEY_CLAIM = "claim_code";

    private final StudentRepository repository;
    private final SavedStateHandle state;

    private final MutableLiveData<Integer> nameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> numberError = new MutableLiveData<>();
    private final MutableLiveData<Integer> programmeError = new MutableLiveData<>();
    private final MutableLiveData<Integer> claimError = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Result.Status>> outcome = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();

    public RegisterViewModel(StudentRepository repository, SavedStateHandle state) {
        this.repository = repository;
        this.state = state;
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

    /** "" means no group chosen (Unassigned). */
    public LiveData<String> getGroup() {
        return state.getLiveData(KEY_GROUP, "");
    }

    public LiveData<String> getClaimCode() {
        return state.getLiveData(KEY_CLAIM, "");
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

    public void setClaimCode(String value) {
        state.set(KEY_CLAIM, value);
    }

    // ----- What the screen shows -----

    /** Each error is a string resource ID, or null when the field is fine. */
    public LiveData<Integer> getNameError() {
        return nameError;
    }

    public LiveData<Integer> getNumberError() {
        return numberError;
    }

    public LiveData<Integer> getProgrammeError() {
        return programmeError;
    }

    public LiveData<Integer> getClaimError() {
        return claimError;
    }

    public LiveData<Integer> getPasswordError() {
        return passwordError;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * How submit() ended. SUCCESS, PENDING and GROUP_FULL all mean the profile
     * exists, so the screen can move on; GROUP_FULL means it is unassigned.
     */
    public LiveData<Event<Result.Status>> getOutcome() {
        return outcome;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    // ----- Actions -----

    /** Stores whatever has been typed so far. Nothing is validated yet, so it works half-finished. */
    public void saveDraft() {
        repository.saveRegistrationDraft(buildStudent(), text(KEY_CLAIM), result ->
                message.postValue(new Event<>(result.isSaved()
                        ? R.string.msg_draft_saved
                        : StatusMessages.forStatus(result.status))));
    }

    public void submit(String password) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        if (!validate(password)) {
            return; // the typed input is left exactly as it was
        }

        loading.setValue(true);
        repository.register(buildStudent(), text(KEY_CLAIM), password, result -> {
            loading.postValue(false);
            if (result.status == Result.Status.DUPLICATE_NUMBER) {
                // Shown on the field itself; the form keeps its values.
                numberError.postValue(R.string.msg_duplicate_number);
            } else if (result.status == Result.Status.BAD_CREDENTIALS) {
                claimError.postValue(R.string.error_claim_rejected);
            }
            outcome.postValue(new Event<>(result.status));
            message.postValue(new Event<>(StatusMessages.forStatus(result.status)));
        });
    }

    private boolean validate(String password) {
        Integer name = Validation.nameError(text(KEY_NAME));
        Integer number = Validation.numberError(text(KEY_NUMBER));
        Integer programme = Validation.programmeError(text(KEY_PROGRAMME));
        Integer claim = text(KEY_CLAIM).isEmpty() ? R.string.error_claim_required : null;
        Integer pass = Validation.passwordError(password);

        nameError.setValue(name);
        numberError.setValue(number);
        programmeError.setValue(programme);
        claimError.setValue(claim);
        passwordError.setValue(pass);

        return name == null && number == null && programme == null
                && claim == null && pass == null;
    }

    private Student buildStudent() {
        Student s = new Student();
        s.name = text(KEY_NAME);
        s.studentNumber = text(KEY_NUMBER);
        s.programme = text(KEY_PROGRAMME);
        String group = text(KEY_GROUP);
        // A request only. The place is not reserved until the server confirms it.
        s.pendingGroup = group.isEmpty() ? null : group;
        return s;
    }

    /** The saved text for a key, trimmed, and never null. */
    private String text(String key) {
        String value = state.get(key);
        return Validation.clean(value);
    }
}
