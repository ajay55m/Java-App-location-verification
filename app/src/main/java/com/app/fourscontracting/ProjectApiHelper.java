package com.app.fourscontracting;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        final String url = "https://4scontracting.com/SMCS_APP/fcm_app/get_locations.php";
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);
                        List<ProjectModel> projects = new ArrayList<>();
                        java.util.Set<String> seenCodes = new java.util.HashSet<>();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.optJSONObject(i);
                            if (obj == null) continue;
                            String code = obj.optString("project_code", obj.optString("projname", ""));
                            if (code.isEmpty()) code = String.valueOf(obj.optInt("id", 0));
                            if (code.isEmpty() || "0".equals(code)) continue;
                            if (seenCodes.contains(code.toLowerCase())) continue;
                            seenCodes.add(code.toLowerCase());

                            String desc = obj.optString("project_description", "");
                            String displayName = (!desc.isEmpty() && !desc.equalsIgnoreCase(code))
                                    ? (code + " - " + desc)
                                    : code;

                            projects.add(new ProjectModel(code, displayName));
                        }

                        callback.onSuccess(projects, "");
                    } catch (Exception e) {
                        callback.onError("Error parsing locations response");
                    }
                },
                error -> callback.onError("Network error while loading projects")
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", com.app.fourscontracting.core.utils.Constants.USER_AGENT);
                headers.put("Accept", "application/json, text/plain, */*");
                headers.put("Cookie", "humans_21909=1");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("project_id", "");
                return params;
            }
        };

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                12000, 1, com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        MySingleton.getmInstance(context).addToRequestque(request);
    }
}
