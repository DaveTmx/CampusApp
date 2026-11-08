package com.example.campusapp.data.repository;

import com.example.campusapp.data.remote.ApiService;
import com.example.campusapp.data.remote.StudentDtos;
import com.example.campusapp.model.Result;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lecturer side of student requests: list the pending ones, then approve or reject.
 * Callbacks may arrive on a background thread, so the screen switches to the UI thread itself.
 */
public class StudentRequestRepository {

    private final ApiService api;
    private final Gson gson = new Gson();

    public StudentRequestRepository(ApiService api) {
        this.api = api;
    }

    public void listPending(RepoCallback<List<StudentDtos.RequestDto>> callback) {
        api.listRequests("PENDING").enqueue(new Callback<StudentDtos.RequestsEnvelope>() {
            @Override
            public void onResponse(Call<StudentDtos.RequestsEnvelope> call,
                                   Response<StudentDtos.RequestsEnvelope> response) {
                StudentDtos.RequestsEnvelope body = response.body();
                if (response.isSuccessful() && body != null) {
                    List<StudentDtos.RequestDto> list = body.requests == null ? new ArrayList<>() : body.requests;
                    callback.onResult(Result.of(Result.Status.SUCCESS, list));
                    return;
                }
                callback.onResult(Result.of(statusOf(response, null), null));
            }

            @Override
            public void onFailure(Call<StudentDtos.RequestsEnvelope> call, Throwable t) {
                callback.onResult(Result.of(Result.Status.OFFLINE, null));
            }
        });
    }

    public void decide(int requestId, boolean approve, RepoCallback<Void> callback) {
        Call<Void> call = approve ? api.approveRequest(requestId) : api.rejectRequest(requestId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onResult(Result.of(Result.Status.SUCCESS, null));
                    return;
                }
                callback.onResult(Result.of(statusOf(response, errorCode(response)), null));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onResult(Result.of(Result.Status.OFFLINE, null));
            }
        });
    }

    private static Result.Status statusOf(Response<?> response, String code) {
        if ("GROUP_FULL".equals(code)) return Result.Status.GROUP_FULL;
        if ("DUPLICATE_NUMBER".equals(code)) return Result.Status.DUPLICATE_NUMBER;
        switch (response.code()) {
            case 401: return Result.Status.SESSION_EXPIRED;
            case 403: return Result.Status.NOT_ALLOWED;
            case 404: return Result.Status.NOT_FOUND;
            case 409: return Result.Status.CONFLICT;
            default: return Result.Status.INVALID;
        }
    }

    /** The server's stable error code, such as GROUP_FULL, or null if the body has none. */
    private String errorCode(Response<?> response) {
        try {
            if (response.errorBody() == null) return null;
            StudentDtos.ErrorBody error = gson.fromJson(response.errorBody().string(), StudentDtos.ErrorBody.class);
            return error == null ? null : error.error;
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }
}
