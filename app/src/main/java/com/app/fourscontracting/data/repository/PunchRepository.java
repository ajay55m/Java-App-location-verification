package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.PunchResponse;
import com.app.fourscontracting.core.network.ApiClient;
import retrofit2.Callback;

public class PunchRepository {
    public void punchAttendance(int uid, int empid, String projectCode, int locationId, String type,
                                String imageBase64, double latitude, double longitude, float accuracy,
                                int isMock, String mobileSessionId, Callback<PunchResponse> callback) {
        ApiClient.getService().punchAttendance(uid, empid, projectCode, locationId, type,
                imageBase64, latitude, longitude, accuracy, isMock, mobileSessionId).enqueue(callback);
    }
}
