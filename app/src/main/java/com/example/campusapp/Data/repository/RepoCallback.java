package com.example.campusapp.Data.repository;

import com.example.campusapp.model.Result;

/**
 * How a repository hands a result back. It may be called on a background
 * thread, which is why the ViewModels answer with postValue().
 */
public interface RepoCallback<T> {
    void onResult(Result<T> result);
}
