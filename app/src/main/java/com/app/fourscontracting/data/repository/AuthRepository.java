package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.core.network.ApiClient;
import com.app.fourscontracting.data.model.auth.LoginResponse;
import retrofit2.Call;
import retrofit2.Callback;

public class AuthRepository {
    public void login(String id, String pwd, Callback<LoginResponse> callback) {
        ApiClient.getService().login(id, pwd).enqueue(new Callback<com.app.fourscontracting.LoginResponse>() {
            @Override
            public void onResponse(Call<com.app.fourscontracting.LoginResponse> call, retrofit2.Response<com.app.fourscontracting.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.app.fourscontracting.LoginResponse r = response.body();
                    LoginResponse mapped = LoginResponse.fromJson(r.status);
                    mapped.status = r.status;
                    mapped.message = r.message;
                    mapped.uid = r.uid;
                    mapped.empid = r.empid;
                    mapped.staffid = r.staffid;
                    mapped.username = r.username;
                    mapped.employee_name = r.employee_name;
                    mapped.employee_number = r.employee_number;
                    mapped.dept_id = r.dept_id;
                    mapped.department_id = r.department_id;
                    mapped.work_location_id = r.work_location_id;
                    mapped.has_project_allocation = r.has_project_allocation;
                    callback.onResponse(null, retrofit2.Response.success(mapped));
                } else {
                    callback.onResponse(null, retrofit2.Response.error(response.code(), response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<com.app.fourscontracting.LoginResponse> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }
}

