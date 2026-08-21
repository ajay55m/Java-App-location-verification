package com.app.fourscontracting.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageAttendanceApi {

    private static final String TAG = "ManageAttendanceApi";

    public interface FeedCallback {
        void onSuccess(String userName,
                       List<AllocatedProjectModel> allocatedProjects,
                       List<AttendanceRecordModel> attendanceRecords,
                       List<MoveRecordModel> moveRecords);
        void onError(String message);
    }

    public interface BreakUpdateCallback {
        void onSuccess(String attendId, double updatedBreakHours);
        void onError(String message);
    }

    public void fetchFeed(Context context, String uid, String projid,
                          String day, String month, String year,
                          FeedCallback callback) {
        Uri.Builder builder = Uri.parse(ApiConfig.API_MANAGE_ATTENDANCE).buildUpon();
        builder.appendQueryParameter("action", "get_feed");
        if (uid != null && !uid.isEmpty()) {
            builder.appendQueryParameter("uid", uid);
        }
        if (projid != null && !projid.isEmpty()) {
            builder.appendQueryParameter("projname", projid);
        }
        if (day != null && !day.isEmpty()) {
            builder.appendQueryParameter("f_day", day);
        }
        if (month != null && !month.isEmpty()) {
            builder.appendQueryParameter("f_month", month);
        }
        if (year != null && !year.isEmpty()) {
            builder.appendQueryParameter("f_year", year);
        }

        String url = builder.build().toString();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (callback == null) return;
                    try {
                        if (response == null || !"success".equalsIgnoreCase(response.optString("status", ""))) {
                            callback.onError(response != null ? response.optString("message", "Failed to fetch attendance data") : "Empty response from server");
                            return;
                        }

                        String userName = response.optString("user_name", "User");

                        // Parse allocated projects
                        List<AllocatedProjectModel> projects = new ArrayList<>();
                        JSONArray projArray = response.optJSONArray("allocated_projects");
                        if (projArray == null) {
                            projArray = response.optJSONArray("projects");
                        }
                        if (projArray != null) {
                            for (int i = 0; i < projArray.length(); i++) {
                                JSONObject pObj = projArray.optJSONObject(i);
                                if (pObj != null) {
                                    projects.add(new AllocatedProjectModel(
                                            pObj.optString("id", ""),
                                            pObj.optString("projname", "")
                                    ));
                                }
                            }
                        }

                        // Parse attendance records
                        List<AttendanceRecordModel> attendanceRecords = new ArrayList<>();
                        JSONArray recArray = response.optJSONArray("records");
                        if (recArray != null) {
                            for (int i = 0; i < recArray.length(); i++) {
                                JSONObject rObj = recArray.optJSONObject(i);
                                if (rObj != null) {
                                    attendanceRecords.add(new AttendanceRecordModel(
                                            rObj.optString("attend_id", ""),
                                            rObj.optString("first_name", ""),
                                            rObj.optString("projname", ""),
                                            rObj.optString("timein", ""),
                                            rObj.optString("timeout", ""),
                                            rObj.optDouble("break_hours", 0.0),
                                            rObj.optBoolean("has_in", false),
                                            rObj.optBoolean("has_out", false),
                                            rObj.optString("in_photo_url", ""),
                                            rObj.optString("out_photo_url", "")
                                    ));
                                }
                            }
                        }

                        // Parse move records
                        List<MoveRecordModel> moveRecords = new ArrayList<>();
                        JSONArray moveArray = response.optJSONArray("move_records");
                        if (moveArray != null) {
                            for (int i = 0; i < moveArray.length(); i++) {
                                JSONObject mObj = moveArray.optJSONObject(i);
                                if (mObj != null) {
                                    moveRecords.add(new MoveRecordModel(
                                            mObj.optString("move_id", ""),
                                            mObj.optString("first_name", ""),
                                            mObj.optString("projname", ""),
                                            mObj.optString("in_time", ""),
                                            mObj.optString("out_time", ""),
                                            mObj.optBoolean("has_in", false),
                                            mObj.optBoolean("has_out", false),
                                            mObj.optString("in_photo_url", ""),
                                            mObj.optString("out_photo_url", "")
                                    ));
                                }
                            }
                        }

                        callback.onSuccess(userName, projects, attendanceRecords, moveRecords);

                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error: " + e.getMessage(), e);
                        callback.onError("Data parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    if (callback != null) {
                        Log.e(TAG, "Volley error: " + error.toString());
                        callback.onError("Network connection error: Check your internet.");
                    }
                }
        );

        com.app.fourscontracting.MySingleton.getmInstance(context).addToRequestque(request);
    }

    public void updateBreakStatus(Context context, String attendId, boolean tookBreak, BreakUpdateCallback callback) {
        Uri.Builder builder = Uri.parse(ApiConfig.API_MANAGE_ATTENDANCE).buildUpon();
        builder.appendQueryParameter("action", "update_break");
        String url = builder.build().toString();

        com.android.volley.toolbox.StringRequest request = new com.android.volley.toolbox.StringRequest(
                Request.Method.POST,
                url,
                responseStr -> {
                    try {
                        JSONObject obj = new JSONObject(responseStr);
                        if ("success".equalsIgnoreCase(obj.optString("status", ""))) {
                            double breakHours = obj.optDouble("break_hours", tookBreak ? 1.0 : 0.0);
                            callback.onSuccess(attendId, breakHours);
                        } else {
                            callback.onError(obj.optString("message", "Error updating break status"));
                        }
                    } catch (Exception e) {
                        callback.onError("Response error while updating break status");
                    }
                },
                error -> callback.onError("Network error updating break status")
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", attendId);
                params.put("took_break", tookBreak ? "1" : "0");
                return params;
            }
        };

        com.app.fourscontracting.MySingleton.getmInstance(context).addToRequestque(request);
    }
}
