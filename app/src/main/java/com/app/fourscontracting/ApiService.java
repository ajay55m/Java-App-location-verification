package com.app.fourscontracting;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("subadminlogin.php")
    Call<LoginResponse> login(@Field("id") String id, @Field("pwd") String pwd);

    @FormUrlEncoded
    @POST("get_locations.php")
    Call<List<ProjectModel>> getProjects(@Field("project_id") String projectId);

    default Call<List<ProjectModel>> getProjects() {
        return getProjects("");
    }

    @FormUrlEncoded
    @POST("get_locations.php")
    Call<List<LocationModel>> getLocations(@Field("project_id") String projectCode);

    @GET("api_labour_manager.php")
    Call<ManagerResponse> getTodayStatus(
            @Query("dept_id") int deptId,
            @Query("location_id") int locationId,
            @Query("uid") int uid,
            @Query("empid") int empid
    );

    @FormUrlEncoded
    @POST("multi-savetimein.php")
    Call<PunchResponse> punchAttendance(
            @Field("uid") int uid,
            @Field("empid") int empid,
            @Field("dept_id") int deptId,
            @Field("project_id") String projectCode,
            @Field("type") String type,
            @Field("latitude") double latitude,
            @Field("longitude") double longitude,
            @Field("is_mock") int isMock,
            @Field("imagepath") String imageBase64,
            @Field("mobile_session_id") String mobileSessionId
    );
}

