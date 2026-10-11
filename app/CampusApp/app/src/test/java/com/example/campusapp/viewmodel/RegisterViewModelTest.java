package com.example.campusapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.example.campusapp.R;
import com.example.campusapp.model.Result;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RegisterViewModelTest {

    // Makes LiveData deliver values immediately, with no Android main thread.
    @Rule
    public InstantTaskExecutorRule instantLiveData = new InstantTaskExecutorRule();

    private FakeStudentRepository repository;
    private RegisterViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeStudentRepository();
        viewModel = new RegisterViewModel(repository, new SavedStateHandle());
        viewModel.setName("Chanda Mwila");
        viewModel.setProgramme("CS");
        viewModel.setClaimCode("CLAIM-001");
    }

    /** Rule: nine digits exactly. A bad number never reaches the repository and the input is kept. */
    @Test
    public void shortNumber_isRejected_andInputIsKept() {
        viewModel.setNumber("12345");

        viewModel.submit("password123");

        assertEquals(0, repository.registerCalls);
        assertEquals(Integer.valueOf(R.string.error_number_invalid),
                viewModel.getNumberError().getValue());
        assertEquals("12345", viewModel.getNumber().getValue());
    }

    /** Rule: outer spaces are trimmed and leading zeroes are preserved. */
    @Test
    public void leadingZeroes_arePreserved() {
        viewModel.setNumber(" 012345678 ");

        viewModel.submit("password123");

        assertEquals(1, repository.registerCalls);
        assertEquals("012345678", repository.lastRegistered.studentNumber);
        assertNull(viewModel.getNumberError().getValue());
    }

    /** Rule: a space inside the number is rejected, not removed. */
    @Test
    public void embeddedSpace_isRejected() {
        viewModel.setNumber("1234 5678");

        viewModel.submit("password123");

        assertEquals(0, repository.registerCalls);
    }

    /** Rule: a duplicate number is reported on the number field and the form keeps its values. */
    @Test
    public void duplicateNumber_showsErrorOnNumberField() {
        viewModel.setNumber("202100001");
        repository.nextStatus = Result.Status.DUPLICATE_NUMBER;

        viewModel.submit("password123");

        assertEquals(Integer.valueOf(R.string.msg_duplicate_number),
                viewModel.getNumberError().getValue());
        assertEquals("Chanda Mwila", viewModel.getName().getValue());
        assertEquals(Result.Status.DUPLICATE_NUMBER, viewModel.getOutcome().getValue().peek());
    }
}
