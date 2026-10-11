package com.example.campusapp.util;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.data.repository.LocalAuthRepository;
import com.example.campusapp.data.repository.LocalStudentRepository;
import com.example.campusapp.viewmodel.ViewModelFactory;

/**
 * One place that builds the repositories and the ViewModelFactory, so every screen shares the
 * same instances. In an Activity:
 *
 *   AuthViewModel vm = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
 *           .get(AuthViewModel.class);
 *
 * To switch to a real backend later, build the Retrofit/Room repositories here instead.
 */
public final class ServiceLocator {

    private static LocalAuthRepository authRepository;
    private static LocalStudentRepository studentRepository;
    private static ViewModelFactory factory;

    private ServiceLocator() {
    }

    public static synchronized LocalAuthRepository authRepository(Context context) {
        if (authRepository == null) {
            authRepository = new LocalAuthRepository(context.getApplicationContext());
        }
        return authRepository;
    }

    public static synchronized LocalStudentRepository studentRepository(Context context) {
        if (studentRepository == null) {
            studentRepository = new LocalStudentRepository(
                    context.getApplicationContext(), authRepository(context));
        }
        return studentRepository;
    }

    public static synchronized ViewModelProvider.Factory viewModelFactory(Context context) {
        if (factory == null) {
            factory = new ViewModelFactory(authRepository(context), studentRepository(context));
        }
        return factory;
    }
}
