package com.example.campusapp.ui.lecturer;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.Data.repository.StudentRosterRepository;
import com.example.campusapp.util.Debouncer;

/**
 * Creates LecturerRosterViewModel with its repository and a main-thread debouncer.
 * Usage: new ViewModelProvider(this, new LecturerRosterViewModelFactory(repo))
 *            .get(LecturerRosterViewModel.class);
 */
public class LecturerRosterViewModelFactory implements ViewModelProvider.Factory {

    private final StudentRosterRepository repository;

    public LecturerRosterViewModelFactory(StudentRosterRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LecturerRosterViewModel.class)) {
            Debouncer debouncer = new Debouncer(Debouncer.mainThread(),
                    LecturerRosterViewModel.SEARCH_DEBOUNCE_MS);
            return (T) new LecturerRosterViewModel(repository, debouncer);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
