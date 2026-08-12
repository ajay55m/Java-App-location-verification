package com.app.fourscontracting;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProjectApiHelper {

    public interface Callback {
        void onSuccess(List<ProjectModel> projects, String username);
        void onError(String message);
    }

    public void fetchProjects(Context context, String uid, Callback callback) {
        if (context == null) {
            callback.onError("Invalid context");
            return;
        }

        final String url = "https://4scontracting.com/SMCS_APP/fcm_app/getprojects.php?uid=" + uid;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String status = response.optString("status", "");
                        if (!"success".equalsIgnoreCase(status)) {
                            callback.onError(response.optString("message", "Unable to load projects"));
                            return;
                        }

                        JSONArray projectsArray = response.optJSONArray("projects");
                        List<ProjectModel> projects = new ArrayList<>();
                        if (projectsArray != null) {
                            for (int i = 0; i < projectsArray.length(); i++) {
                                JSONObject projectObject = projectsArray.optJSONObject(i);
                                if (projectObject == null) continue;
                                String projectId = projectObject.optString("id", "");
                                String projectName = projectObject.optString("projname", "");
                                if (!projectName.isEmpty()) {
                                    projects.add(new ProjectModel(projectId, projectName));
                                }
                            }
                        }

                        callback.onSuccess(projects, response.optString("username", ""));
                    } catch (Exception e) {
                        callback.onError("Error parsing projects response");
                    }
                },
                error -> callback.onError("Network error while loading projects")
        );

        MySingleton.getmInstance(context).addToRequestque(request);
    }
}
