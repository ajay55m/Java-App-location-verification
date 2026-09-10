package com.app.fourscontracting.data;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.app.fourscontracting.MySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * REST API client for native Labour Management (replacing employee_details.php WebView).
 * Uses api_labour_manager.php for stats and employee statuses.
 */
public class LabourManagementApi {

    public interface DepartmentsCallback {
        void onSuccess(List<DepartmentModel> departments);
        void onError(String message);
    }

    public interface EmployeesCallback {
        void onSuccess(List<LabourEmployeeModel> employees, int totalCount, int inCount, int outCount);
        void onError(String message);
    }

    public void fetchDepartments(Context context, DepartmentsCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ApiConfig.API_GET_DEPARTMENTS,
                null,
                response -> {
                    try {
                        if (!"success".equalsIgnoreCase(response.optString("status", ""))) {
                            callback.onError(response.optString("message", "Unable to load departments"));
                            return;
                        }
                        List<DepartmentModel> list = new ArrayList<>();
                        JSONArray arr = response.optJSONArray("departments");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;
                                list.add(new DepartmentModel(o.optString("id"), o.optString("name")));
                            }
                        }
                        callback.onSuccess(list);
                    } catch (Exception e) {
                        callback.onError("Error parsing departments");
                    }
                },
                error -> callback.onError("Network error loading departments")
        );
        MySingleton.getmInstance(context).addToRequestque(request);
    }

    public void fetchEmployees(Context context, String uid, String locationId, String deptId, EmployeesCallback callback) {
        Uri.Builder builder = Uri.parse(ApiConfig.API_LABOUR_MANAGER).buildUpon();
        if (uid != null && !uid.isEmpty()) {
            builder.appendQueryParameter("uid", uid);
        }
        if (locationId != null && !locationId.isEmpty()) {
            builder.appendQueryParameter("location_id", locationId);
        }
        if (deptId != null && !deptId.isEmpty()) {
            builder.appendQueryParameter("dept_id", deptId);
        }
        String url = builder.build().toString();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (!"success".equalsIgnoreCase(response.optString("status", "success"))) {
                            callback.onError(response.optString("message", "Unable to load employees"));
                            return;
                        }

                        List<LabourEmployeeModel> list = new ArrayList<>();
                        JSONArray arr = response.optJSONArray("employees");

                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;
                                list.add(new LabourEmployeeModel(o));
                            }
                        }

                        int computedIn = 0;
                        int computedOut = 0;
                        for (LabourEmployeeModel m : list) {
                            if (m.isClockedIn()) {
                                computedIn++;
                            } else if (m.isClockedOut()) {
                                computedOut++;
                            }
                        }

                        int totalCount = list.size();
                        int inCount = computedIn;
                        int outCount = computedOut;

                        JSONObject stats = response.optJSONObject("stats");
                        if (stats != null) {
                            totalCount = stats.optInt("total_staff", stats.optInt("total", list.size()));
                            if (stats.has("present_here") || stats.has("in_count")) {
                                inCount = stats.optInt("present_here", stats.optInt("in_count", computedIn));
                            }
                            if (stats.has("out_count") || stats.has("time_out_count")) {
                                outCount = stats.optInt("out_count", stats.optInt("time_out_count", computedOut));
                            }
                        }

                        callback.onSuccess(list, totalCount, inCount, outCount);
                    } catch (Exception e) {
                        callback.onError("Error parsing employee details: " + e.getMessage());
                    }
                },
                error -> {
                    // Fallback to legacy endpoint if server is using api_get_move_employees.php
                    fetchEmployeesFallback(context, locationId, deptId, callback);
                }
        );
        MySingleton.getmInstance(context).addToRequestque(request);
    }

    private void fetchEmployeesFallback(Context context, String locationId, String deptId, EmployeesCallback callback) {
        Uri.Builder builder = Uri.parse(ApiConfig.API_GET_MOVE_EMPLOYEES).buildUpon()
                .appendQueryParameter("dept_id", deptId != null ? deptId : "");
        if (locationId != null && !locationId.isEmpty()) {
            builder.appendQueryParameter("location_id", locationId);
        }
        String url = builder.build().toString();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (!"success".equalsIgnoreCase(response.optString("status", ""))) {
                            callback.onError(response.optString("message", "Unable to load employees"));
                            return;
                        }
                        List<LabourEmployeeModel> list = new ArrayList<>();
                        JSONArray arr = response.optJSONArray("employees");
                        int inCount = 0;
                        int outCount = 0;

                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;

                                String timeIn = o.optString("timein_display", o.optString("time_in", ""));
                                String timeOut = o.optString("timeout_display", o.optString("time_out", ""));
                                boolean hasIn = !timeIn.isEmpty() && !"-".equals(timeIn);
                                boolean hasOut = !timeOut.isEmpty() && !"-".equals(timeOut);

                                if (hasIn) inCount++;
                                if (hasOut) outCount++;

                                String empId = o.optString("id", "");
                                String photo = o.optString("photo", o.optString("photo_url", "")).trim();
                                if ((photo.isEmpty() || "null".equalsIgnoreCase(photo)) && !empId.isEmpty()) {
                                    photo = ApiConfig.SUBCONTRACTOR + "/get_photo.php?id=" + empId;
                                } else if (!photo.isEmpty() && !photo.startsWith("http://") && !photo.startsWith("https://")) {
                                    if (photo.startsWith("/")) {
                                        photo = ApiConfig.HOST + photo;
                                    } else {
                                        photo = ApiConfig.SUBCONTRACTOR + "/" + photo;
                                    }
                                }
                                if (photo.startsWith("http://")) {
                                    photo = photo.replace("http://", "https://");
                                }

                                String status = hasIn ? (hasOut ? "IN & OUT COMPLETED" : "TIME IN MARKED") : "NOT MARKED";
                                String statusCode = hasIn ? "IN_HERE" : "";
                                list.add(new LabourEmployeeModel(
                                        o.optString("id"),
                                        o.optString("first_name", o.optString("name", "Employee")),
                                        statusCode,
                                        status,
                                        o.optString("site_name", ""),
                                        photo,
                                        !hasIn,
                                        hasIn && !hasOut,
                                        o.optString("department_name", o.optString("dept", ""))
                                ));
                            }
                        }
                        callback.onSuccess(list, list.size(), inCount, outCount);
                    } catch (Exception e) {
                        callback.onError("Error parsing employee details");
                    }
                },
                error -> callback.onError("Network error loading employee details")
        );
        MySingleton.getmInstance(context).addToRequestque(request);
    }
}

