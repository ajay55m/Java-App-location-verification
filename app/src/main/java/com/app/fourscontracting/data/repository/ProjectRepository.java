package com.app.fourscontracting.data.repository;

import com.app.fourscontracting.core.network.ApiClient;
import com.app.fourscontracting.LocationModel;
import com.app.fourscontracting.ProjectModel;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;

public class ProjectRepository {
    public void getProjects(Callback<List<ProjectModel>> callback) {
        ApiClient.getService().getProjects().enqueue(callback);
    }

    public void getLocations(String projectCode, Callback<List<LocationModel>> callback) {
        ApiClient.getService().getLocations(projectCode).enqueue(callback);
    }
}

