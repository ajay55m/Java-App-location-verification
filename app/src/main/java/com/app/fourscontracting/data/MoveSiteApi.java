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
 * Native Move Site API client — does not use employee_details.php / WebView.
 * Cookie bypass is handled by CookieHurlStack:
 *   1) normal request (no Cookie)
 *   2) if 409/403 → retry with Cookie: humans_21909=1
 */
public class MoveSiteApi {

    public interface DepartmentsCallback {
        void onSuccess(List<DepartmentModel> departments);
        void onError(String message);
    }

    public interface EmployeesCallback {
        void onSuccess(List<MoveEmployeeModel> employees, String locationName, int readyCount, int movingCount);
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

    public void fetchMoveEmployees(Context context, String deptId, String locationId, EmployeesCallback callback) {
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
                        List<MoveEmployeeModel> list = new ArrayList<>();
                        JSONArray arr = response.optJSONArray("employees");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.optJSONObject(i);
                                if (o == null) continue;
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
                                String moveIdVal = o.optString("move_id", o.optString("moveid", o.optString("id", "")));
                                String attendIdVal = o.optString("attend_id", o.optString("attendance_id", ""));
                                list.add(new MoveEmployeeModel(
                                        o.optString("id"),
                                        o.optString("first_name"),
                                        o.optString("timein_display"),
                                        o.optString("timeout_display"),
                                        o.optBoolean("on_move", false),
                                        o.optString("action", "MOVE"),
                                        photo,
                                        moveIdVal,
                                        attendIdVal
                                ));
                            }
                        }
                        callback.onSuccess(
                                list,
                                response.optString("location_name", ""),
                                response.optInt("ready_count", 0),
                                response.optInt("moving_count", 0)
                        );
                    } catch (Exception e) {
                        callback.onError("Error parsing employees");
                    }
                },
                error -> callback.onError("Network error loading move employees")
        );
        MySingleton.getmInstance(context).addToRequestque(request);
    }
}
