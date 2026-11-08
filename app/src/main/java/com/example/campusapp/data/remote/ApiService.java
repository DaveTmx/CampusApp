package com.example.campusapp.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Retrofit endpoints for the backend. */
public interface ApiService {

    /**
     * Lecturer roster. page is 1-based. Pass programme, group or unassigned as needed; null is left out.
     * Send group or unassigned, not both.
     */
    @GET("api/students")
    Call<StudentPageDto> getStudents(
            @Query("q") String query,
            @Query("programme") String programme,
            @Query("group") String group,
            @Query("unassigned") Boolean unassigned,
            @Query("page") int page,
            @Query("pageSize") int pageSize);

    @GET("api/students/{id}")
    Call<StudentDtos.RecordEnvelope> getStudent(@Path("id") String studentId);

    @POST("api/students")
    Call<StudentDtos.CreateResponse> createStudent(@Body StudentDtos.CreateRequest body);

    @PUT("api/students/{id}")
    Call<StudentDtos.EditResponse> editStudent(@Path("id") String studentId, @Body StudentDtos.EditRequest body);

    @GET("api/requests")
    Call<StudentDtos.RequestsEnvelope> listRequests(@Query("status") String status);

    @POST("api/requests/{id}/approve")
    Call<Void> approveRequest(@Path("id") int requestId);

    @POST("api/requests/{id}/reject")
    Call<Void> rejectRequest(@Path("id") int requestId);

    @POST("api/students/{id}/requests")
    Call<Void> createStudentRequest(@Path("id") String studentId, @Body StudentDtos.RequestBody body);

    @POST("api/students/{id}/assign")
    Call<StudentDtos.AssignResponse> assignGroup(@Path("id") String studentId, @Body StudentDtos.AssignRequest body);

    /** Removes the student from their group. Idempotent. */
    @POST("api/students/{id}/unassign")
    Call<StudentDtos.AssignResponse> unassignGroup(@Path("id") String studentId);

    @DELETE("api/students/{id}")
    Call<StudentDtos.DeleteResponse> deleteStudent(@Path("id") String studentId);

    /** Sends one lecturer action. Safe to repeat with the same operationId. */
    @POST("api/operations")
    Call<StudentDtos.OperationResponse> submitOperation(@Body StudentDtos.OperationRequest body);

    /** Group capacity and occupancy for G01 to G14. */
    @GET("api/groups")
    Call<StudentDtos.GroupsResponse> getGroups();

    /** Students, by email or student number. Lecturer accounts get 403 STAFF_SIGN_IN_REQUIRED here. */
    @POST("api/auth/login")
    Call<AuthDto.LoginResponse> login(@Body AuthDto.StudentLoginRequest body);

    /** Creates an inactive student account. The reply is 201; the student then opens the link in their email. */
    @POST("api/auth/register")
    Call<Void> register(@Body AuthDto.RegisterRequest body);

    /** Always answers 200, so the reply does not show whether the email has an account. */
    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(@Body AuthDto.EmailRequest body);

    /** Lecturers, step 1: checks the password and emails a code. */
    @POST("api/auth/staff/login")
    Call<AuthDto.ChallengeResponse> staffLogin(@Body AuthDto.LoginRequest body);

    /** Lecturers, step 2: exchanges the emailed code for a session. */
    @POST("api/auth/staff/verify-code")
    Call<AuthDto.LoginResponse> verifyStaffCode(@Body AuthDto.VerifyCodeRequest body);

    /** Revokes every token for the account. Passed explicitly, because the local token is cleared first. */
    @POST("api/auth/logout")
    Call<Void> logout(@Header("Authorization") String bearer);
}
