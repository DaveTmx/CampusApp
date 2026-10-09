package com.example.campusapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.example.campusapp.model.GroupCount;
import com.example.campusapp.model.Result;
import com.example.campusapp.model.Student;
import com.example.campusapp.model.StudentPage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RosterViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantLiveData = new InstantTaskExecutorRule();

    private FakeStudentRepository repository;
    private RosterViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeStudentRepository();
        viewModel = new RosterViewModel(repository, new SavedStateHandle());
    }

    private static Result<StudentPage> pageWith(String name) {
        Student s = new Student();
        s.studentId = name;
        s.name = name;
        return Result.of(Result.Status.SUCCESS,
                new StudentPage(Collections.singletonList(s), false, false));
    }

    /** Rule: the answer to an old search must not replace the newest search's results. */
    @Test
    public void oldSearchResponse_isIgnored() {
        viewModel.setQuery("ban");   // waitingSearches: 0 = first load, 1 = "ban"
        viewModel.setQuery("banda"); // 2 = "banda"

        repository.waitingSearches.get(2).onResult(pageWith("Banda"));
        repository.waitingSearches.get(1).onResult(pageWith("Bangwe")); // arrives late

        assertEquals(1, viewModel.getStudents().getValue().size());
        assertEquals("Banda", viewModel.getStudents().getValue().get(0).name);
        assertFalse(viewModel.getLoading().getValue());
    }

    /** Rule: the shared summary holds group labels and counts only, never personal data. */
    @Test
    public void shareSummary_hasOnlyLabelsAndCounts() {
        repository.groupCounts.setValue(Arrays.asList(
                new GroupCount("G01", 14), new GroupCount("G02", 15)));

        assertEquals("G01: 14/15\nG02: 15/15", viewModel.buildShareSummary());
    }
}
