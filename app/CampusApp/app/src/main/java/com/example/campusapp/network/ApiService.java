package com.example.campusapp.network;

import com.example.campusapp.Data.local.PendingOperation;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("sync/operation")
    Call<Void> syncPendingOperation(@Body PendingOperation operation);
}