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
    @POST("multi-savetimein.php")
    Call<PunchResponse> punchAttendance(
            @Field("uid") int uid,
            @Field("empid") int empid,
            @Field("project_id") String projectCode,   // RST, not 9
            @Field("location_id") int locationId,      // hrms_location_master.id
            @Field("type") String type,                // IN or OUT
            @Field("imagepath") String imageBase64,
            @Field("latitude") double latitude,
            @Field("longitude") double longitude,
            @Field("accuracy") float accuracy,
            @Field("is_mock") int isMock,              // 0 or 1
            @Field("mobile_session_id") String mobileSessionId
    );

    @GET("api_manage_attendance.php")
    Call<ManageAttendanceResponse> getSelfAttendance(
            @Query("uid") int uid,
            @Query("empid") int empid,
            @Query("dept_id") Integer deptId,
            @Query("location_id") Integer locationId,
            @Query("f_day") String day,
            @Query("f_month") String month,
            @Query("f_year") String year,
            @Query("with_photo") int withPhoto // always 0 for list, photos are heavy
    );

    @GET("api_labour_manager.php")
    Call<LabourManagerResponse> getLabourList(
            @Query("dept_id") int deptId,
            @Query("location_id") int locationId,
            @Query("uid") int uid
    );

    @GET("api_get_move_employees.php")
    Call<MoveEmployeesResponse> getMoveEmployees(
            @Query("dept_id") int deptId,
            @Query("location_id") int locationId
    );

    @GET("get_locations.php")
    Call<List<ProjectModel>> getProjects(); // No project_id = project list

    @GET("get_locations.php")
    Call<List<LocationModel>> getLocations(@Query("project_id") String projectCode);

    @GET("api_labour_manager.php")
    Call<ManagerResponse> getTodayStatus(
            @Query("dept_id") int deptId,
            @Query("location_id") int locationId,
            @Query("uid") int uid,
            @Query("empid") int empid
    );
}
