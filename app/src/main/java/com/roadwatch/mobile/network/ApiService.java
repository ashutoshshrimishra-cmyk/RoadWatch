package com.roadwatch.mobile.network;

import com.roadwatch.mobile.network.dto.AiChatRequest;
import com.roadwatch.mobile.network.dto.AiChatResponse;
import com.roadwatch.mobile.network.dto.AuthorityDto;
import com.roadwatch.mobile.network.dto.BudgetDto;
import com.roadwatch.mobile.network.dto.ComplaintDto;
import com.roadwatch.mobile.network.dto.LoginRequest;
import com.roadwatch.mobile.network.dto.LoginResponse;
import com.roadwatch.mobile.network.dto.PagedComplaintsDto;
import com.roadwatch.mobile.network.dto.PagedNotificationsDto;
import com.roadwatch.mobile.network.dto.RoadAlertCreateRequest;
import com.roadwatch.mobile.network.dto.RoadAlertDto;
import com.roadwatch.mobile.network.dto.RoadDto;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface against the production backend at /api/**.
 * Endpoint contract intentionally kept identical to what the deployed
 * backend (https://github.com/25f3001314-dev/Roadwatch) exposes.
 */
public interface ApiService {

    @POST("api/api/auth/citizen-login")
    Call<LoginResponse> citizenLogin(@Body com.roadwatch.mobile.network.dto.CitizenAuthRequest request);

    @POST("api/api/auth/register")
    Call<LoginResponse> citizenRegister(@Body java.util.Map<String, String> body);

    @POST("api/citizen/fcm-token")
    Call<ResponseBody> registerCitizenFcmToken(@Body java.util.Map<String, String> body);

    @POST("api/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/complaints")
    Call<PagedComplaintsDto> getComplaints(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/complaints/{id}")
    Call<ComplaintDto> getComplaintById(@Path("id") long id);

    @Multipart
    @POST("api/complaints")
    Call<ResponseBody> createComplaint(
            @Part("roadType") RequestBody roadType,
            @Part("location") RequestBody location,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("description") RequestBody description,
            @Part MultipartBody.Part image
    );

    @GET("api/budgets")
    Call<List<BudgetDto>> getBudgets();

    @GET("api/citizen/me/complaints")
    Call<PagedComplaintsDto> getMyComplaints(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/citizen/me/complaints/{id}/timeline")
    Call<List<java.util.Map<String, Object>>> getComplaintTimeline(@Path("id") long id);

    @GET("api/citizen/me/notifications")
    Call<PagedNotificationsDto> getMyNotifications(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/citizen/me/notifications/unread-count")
    Call<java.util.Map<String, Object>> getUnreadNotificationCount();

    @POST("api/citizen/me/notifications/mark-read")
    Call<ResponseBody> markNotificationsRead();

    @POST("api/citizen/me/complaints/{id}/feedback")
    Call<ResponseBody> submitFeedback(@Path("id") long id, @Body java.util.Map<String, Object> body);

    @GET("api/roads")
    Call<List<RoadDto>> getRoads();

    @GET("api/roads/{id}")
    Call<RoadDto> getRoadById(@Path("id") long id);

    @GET("api/authorities")
    Call<List<AuthorityDto>> getAuthorities();

    @POST("api/ai/chat")
    Call<AiChatResponse> aiChat(@Body AiChatRequest request);

    @GET("api/alerts")
    Call<List<RoadAlertDto>> getAlerts(
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radiusKm") Double radiusKm
    );

    @POST("api/alerts")
    Call<RoadAlertDto> createAlert(@Body RoadAlertCreateRequest request);

    @POST("api/alerts/{id}/upvote")
    Call<RoadAlertDto> upvoteAlert(@Path("id") long id);
}
