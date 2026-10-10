package com.example.campusapp.Data.repository;

import com.example.campusapp.Data.model.Student;
import com.example.campusapp.model.Result;

import java.util.List;

/**
 * How a repository hands a result back. It may be called on a background
 * thread, which is why the ViewModels answer with postValue().
 */
public interface RepoCallback<T> {
    void onResult(Result<T> result);
    void onSuccess(List<Student> students);
    void onOfflineFallback(List<Student> students);
    void onSessionExpired();
}
