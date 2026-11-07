package com.example.campusapp.util;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;

import com.example.campusapp.BuildConfig;
import com.example.campusapp.data.remote.ApiClient;
import com.example.campusapp.data.remote.ApiService;
import com.example.campusapp.data.remote.AuthInterceptor;
import com.example.campusapp.data.remote.TokenStore;
import com.example.campusapp.data.repository.LocalAuthRepository;
import com.example.campusapp.data.repository.LocalStudentRepository;
import com.example.campusapp.data.repository.RemoteAuthRepository;
import com.example.campusapp.data.repository.RemoteStudentRepository;
import com.example.campusapp.data.sync.OutboxQueue;
import com.example.campusapp.viewmodel.ViewModelFactory;

/**
 * One place that builds the repositories and the ViewModelFactory, so every screen shares the
 * same instances. In an Activity:
 *
 *   AuthViewModel vm = new ViewModelProvider(this, ServiceLocator.viewModelFactory(this))
 *           .get(AuthViewModel.class);
 *
 * Sign-in and the lecturer roster go to the backend. Student registration and password reset
 * still use the local store (authRepository) until they are moved to the backend too.
 */
public final class ServiceLocator {

    private static LocalAuthRepository authRepository;
    private static RemoteAuthRepository remoteAuthRepository;
    private static ApiService apiService;
    private static LocalStudentRepository studentRepository;
    private static RemoteStudentRepository remoteStudentRepository;
    private static ViewModelProvider.Factory factory;

    private ServiceLocator() {
    }

    /** Local accounts: registration and password reset. */
    public static synchronized LocalAuthRepository authRepository(Context context) {
        if (authRepository == null) {
            authRepository = new LocalAuthRepository(context.getApplicationContext());
        }
        return authRepository;
    }

    /** The backend API, with the saved token added to each request. */
    public static synchronized ApiService apiService(Context context) {
        if (apiService == null) {
            Context app = context.getApplicationContext();
            apiService = ApiClient.create(BuildConfig.API_BASE_URL, new AuthInterceptor(new TokenStore(app)));
        }
        return apiService;
    }

    /** Sign-in against the backend (/api/auth). */
    public static synchronized RemoteAuthRepository remoteAuthRepository(Context context) {
        if (remoteAuthRepository == null) {
            Context app = context.getApplicationContext();
            remoteAuthRepository = new RemoteAuthRepository(app, apiService(app), new TokenStore(app));
        }
        return remoteAuthRepository;
    }

    /** Student data on this phone. Used by the student screens and as the fallback for the rest. */
    public static synchronized LocalStudentRepository studentRepository(Context context) {
        if (studentRepository == null) {
            studentRepository = new LocalStudentRepository(
                    context.getApplicationContext(), authRepository(context));
        }
        return studentRepository;
    }

    /** Lecturer roster and editor on the backend; student screens delegate to the local store. */
    public static synchronized RemoteStudentRepository remoteStudentRepository(Context context) {
        if (remoteStudentRepository == null) {
            remoteStudentRepository = new RemoteStudentRepository(
                    apiService(context), studentRepository(context), new OutboxQueue(context));
        }
        return remoteStudentRepository;
    }

    public static synchronized ViewModelProvider.Factory viewModelFactory(Context context) {
        if (factory == null) {
            factory = new ViewModelFactory(remoteAuthRepository(context), remoteStudentRepository(context));
        }
        return factory;
    }
}
