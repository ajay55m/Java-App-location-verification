package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.ManageAttendanceResponse;
import com.app.fourscontracting.core.network.ApiClient;
import retrofit2.Callback;

public class SelfAttendanceRepository {
    public void getSelfAttendance(int uid, int empId, Integer deptId, Integer locationId,
                                  String day, String month, String year, int withPhoto,
                                  Callback<ManageAttendanceResponse> callback) {
        ApiClient.getService().getSelfAttendance(uid, empId, deptId, locationId, day, month, year, withPhoto).enqueue(callback);
    }
}
