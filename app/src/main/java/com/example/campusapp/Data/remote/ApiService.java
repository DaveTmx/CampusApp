package com.example.campusapp.Data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Retrofit endpoints used by the roster. */
public interface ApiService {

    @GET("api/students")
    Call<StudentPageDto> getStudents(
            @Query("q") String query,
            @Query("page") int page,
            @Query("pageSize") int pageSize);
}
