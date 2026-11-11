package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.LabourManagerResponse;
import com.app.fourscontracting.core.network.ApiClient;
import retrofit2.Callback;

public class LabourRepository {
    public void getLabourList(int deptId, int locationId, int uid, Callback<LabourManagerResponse> callback) {
        ApiClient.getService().getLabourList(deptId, locationId, uid).enqueue(callback);
    }
}
