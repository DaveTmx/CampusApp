package com.example.campusapp.Data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the Retrofit service. The auth interceptor adds the session token.
 * baseUrl must end with "/" and should be https:// for the deployed backend.
 */
public final class ApiClient {

    private ApiClient() {}

    public static ApiService create(String baseUrl, Interceptor authInterceptor) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }
}
