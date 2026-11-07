package com.example.campusapp.data.remote;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Adds "Authorization: Bearer <token>" to every request that has a valid saved token. */
public final class AuthInterceptor implements Interceptor {

    private final TokenStore tokens;

    public AuthInterceptor(TokenStore tokens) {
        this.tokens = tokens;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String token = tokens.token();
        // An explicit header set by the caller (logout) is left alone.
        if (token != null && request.header("Authorization") == null) {
            request = request.newBuilder().header("Authorization", "Bearer " + token).build();
        }
        return chain.proceed(request);
    }
}
