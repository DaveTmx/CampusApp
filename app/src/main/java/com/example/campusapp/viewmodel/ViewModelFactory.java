package com.example.campusapp.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.example.campusapp.data.repository.AuthRepository;
import com.example.campusapp.data.repository.StudentRepository;

/**
 * Builds the ViewModels. Android can only create a ViewModel by itself when
 * the constructor has no parameters; ours take a repository, so this class
 * tells Android how to construct each one.
 *
 * In an Activity:
 *   ViewModelFactory factory = new ViewModelFactory(authRepository, studentRepository);
 *   RegisterViewModel vm = new ViewModelProvider(this, factory).get(RegisterViewModel.class);
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository authRepository;
    private final StudentRepository studentRepository;

    public ViewModelFactory(AuthRepository authRepository, StudentRepository studentRepository) {
        this.authRepository = authRepository;
        this.studentRepository = studentRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                          @NonNull CreationExtras extras) {
        ViewModel viewModel;
        if (modelClass == AuthViewModel.class) {
            viewModel = new AuthViewModel(authRepository);
        } else if (modelClass == ProfileViewModel.class) {
            viewModel = new ProfileViewModel(studentRepository);
        } else if (modelClass == RegisterViewModel.class) {
            viewModel = new RegisterViewModel(studentRepository,
                    SavedStateHandleSupport.createSavedStateHandle(extras));
        } else if (modelClass == RosterViewModel.class) {
            viewModel = new RosterViewModel(studentRepository,
                    SavedStateHandleSupport.createSavedStateHandle(extras));
        } else if (modelClass == EditorViewModel.class) {
            viewModel = new EditorViewModel(studentRepository,
                    SavedStateHandleSupport.createSavedStateHandle(extras));
        } else {
            throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
        }
        return modelClass.cast(viewModel);
    }
}
