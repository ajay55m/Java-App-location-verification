package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.MoveEmployeesResponse;
import com.app.fourscontracting.core.network.ApiClient;
import retrofit2.Callback;

public class MoveRepository {
    public void getMoveEmployees(int deptId, int locationId, Callback<MoveEmployeesResponse> callback) {
        ApiClient.getService().getMoveEmployees(deptId, locationId).enqueue(callback);
    }
}

