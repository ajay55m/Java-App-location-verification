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
            builder.appendQueryParameter("subadmin_id", uid);
            builder.appendQueryParameter("user_id", uid);
            builder.appendQueryParameter("empid", uid);
        }
        if (projid != null && !projid.isEmpty() && !"all".equalsIgnoreCase(projid)) {
            builder.appendQueryParameter("projname", projid);
            builder.appendQueryParameter("project_id", projid);
            builder.appendQueryParameter("projectid", projid);
        }
        if (day != null && !day.isEmpty()) {
            builder.appendQueryParameter("f_day", day);
            builder.appendQueryParameter("day", day);
        }
        if (month != null && !month.isEmpty()) {
            builder.appendQueryParameter("f_month", month);
            builder.appendQueryParameter("month", month);
        }
        if (year != null && !year.isEmpty()) {
            builder.appendQueryParameter("f_year", year);
            builder.appendQueryParameter("year", year);
        }
        if (day != null && !day.isEmpty() && month != null && !month.isEmpty() && year != null && !year.isEmpty()) {
            String fullDate = year + "-" + month + "-" + day;
            builder.appendQueryParameter("date", fullDate);
            builder.appendQueryParameter("attendance_date", fullDate);
            builder.appendQueryParameter("f_date", fullDate);
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

                        // Parse supervisor's self attendance ("own_attendance" JSON object) if present
                        JSONObject ownObj = response.optJSONObject("own_attendance");
                        if (ownObj != null && ownObj.length() > 0) {
                            String attendIdVal = optCleanString(ownObj, "attend_id", "attendance_id");
                            String timeInVal = optCleanString(ownObj, "timein", "time_in", "in_time");
                            String timeOutVal = optCleanString(ownObj, "timeout", "time_out", "out_time");
                            String projNameVal = optCleanString(ownObj, "projname", "project_name", "location_name");
                            String uIdVal = optCleanString(ownObj, "userid", "user_id", "uid");
                            String eIdVal = optCleanString(ownObj, "empid", "emp_id", "employee_id");
                            String idVal = optCleanString(ownObj, "id");
                            
                            if (attendIdVal.isEmpty()) {
                                attendIdVal = idVal;
                            }
                            if (uIdVal.isEmpty()) {
                                uIdVal = !uid.isEmpty() ? uid : "";
                            }
                            if (eIdVal.isEmpty()) {
                                eIdVal = !uIdVal.isEmpty() ? uIdVal : uid;
                            }
                            if (uIdVal.isEmpty()) {
                                uIdVal = eIdVal;
                            }
                            String nameVal = optCleanString(ownObj, "first_name", "emp_name", "name");
                            if (nameVal.isEmpty()) nameVal = userName;

                            if (!attendIdVal.isEmpty() || !timeInVal.isEmpty()) {
                                attendanceRecords.add(new AttendanceRecordModel(
                                        attendIdVal,
                                        uIdVal,
                                        eIdVal,
                                        nameVal,
                                        projNameVal,
                                        timeInVal,
                                        timeOutVal,
                                        ownObj.optDouble("break_hours", 0.0),
                                        ownObj.optBoolean("has_in", !timeInVal.isEmpty() && !"--".equals(timeInVal)),
                                        ownObj.optBoolean("has_out", !timeOutVal.isEmpty() && !"--".equals(timeOutVal)),
                                        ownObj.optString("in_photo_url", ""),
                                        ownObj.optString("out_photo_url", "")
                                ));
                            }
                        }

                        JSONArray recArray = response.optJSONArray("records");
                        if (recArray != null) {
                            for (int i = 0; i < recArray.length(); i++) {
                                JSONObject rObj = recArray.optJSONObject(i);
                                if (rObj != null) {
                                    String attendIdVal = optCleanString(rObj, "attend_id", "attendance_id", "attendance_record_id", "attendanceid", "record_id");
                                    String empIdVal = resolveWorkerId(rObj, uid);
                                    String userIdVal = optCleanString(rObj, "userid", "user_id", "subadmin_id", "manager_uid");
                                    String uidVal = optCleanString(rObj, "uid");
                                    String idVal = optCleanString(rObj, "id");

                                    if (attendIdVal.isEmpty() && !idVal.isEmpty()) {
                                        attendIdVal = idVal;
                                    }

                                    if (userIdVal.isEmpty()) {
                                        userIdVal = !uidVal.isEmpty() ? uidVal : uid;
                                    }

                                    String firstNameVal = optCleanString(rObj, "first_name", "emp_name", "name", "employee_name", "username");
                                    String projNameVal = optCleanString(rObj, "projname", "project_name", "project", "location_name");
                                    String timeInVal = optCleanString(rObj, "timein", "time_in", "in_time");
                                    String timeOutVal = optCleanString(rObj, "timeout", "time_out", "out_time");

                                    boolean hasIn = rObj.has("has_in") ? rObj.optBoolean("has_in", false) : (!timeInVal.isEmpty() && !"--".equals(timeInVal));
                                    boolean hasOut = rObj.has("has_out") ? rObj.optBoolean("has_out", false) : (!timeOutVal.isEmpty() && !"--".equals(timeOutVal));
                                    String inPhoto = optCleanString(rObj, "in_photo_url", "in_photo", "photo_in", "in_image");
                                    String outPhoto = optCleanString(rObj, "out_photo_url", "out_photo", "photo_out", "out_image");

                                    attendanceRecords.add(new AttendanceRecordModel(
                                            attendIdVal,
                                            userIdVal,
                                            empIdVal,
                                            firstNameVal,
                                            projNameVal,
                                            timeInVal,
                                            timeOutVal,
                                            rObj.optDouble("break_hours", 0.0),
                                            hasIn,
                                            hasOut,
                                            inPhoto,
                                            outPhoto
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
                                    String mId = optCleanString(mObj, "move_id", "moveid");
                                    String eId = optCleanString(mObj, "empid", "emp_id", "employee_id", "labor_id", "labour_id", "subcontractor_id", "sub_id", "employee_code", "emp_code", "member_id", "uid", "userid", "user_id", "subadmin_id", "manager_uid");
                                    String uIdVal = optCleanString(mObj, "userid", "user_id", "subadmin_id", "manager_uid");
                                    String uidVal = optCleanString(mObj, "uid");
                                    String idVal = optCleanString(mObj, "id");

                                    if (mId.isEmpty()) {
                                        if (!idVal.isEmpty()) {
                                            mId = idVal;
                                        }
                                    }

                                    if (eId.isEmpty()) {
                                        if (!uidVal.isEmpty()) {
                                            eId = uidVal;
                                        }
                                    }
                                    if (eId.isEmpty()) {
                                        if (!uIdVal.isEmpty()) {
                                            eId = uIdVal;
                                        }
                                    }
                                    if (eId.isEmpty() && !idVal.isEmpty() && !idVal.equalsIgnoreCase(mId)) {
                                        eId = idVal;
                                    }

                                    String fName = optCleanString(mObj, "first_name", "emp_name", "name", "employee_name", "username");
                                    String pName = optCleanString(mObj, "projname", "project_name", "project", "location_name", "department_name", "site_name");
                                    String iTime = optCleanString(mObj, "in_time", "timein", "time_in");
                                    String oTime = optCleanString(mObj, "out_time", "timeout", "time_out");

                                    boolean hasIn = mObj.has("has_in") ? mObj.optBoolean("has_in", false) : (!iTime.isEmpty() && !"--".equals(iTime));
                                    boolean hasOut = mObj.has("has_out") ? mObj.optBoolean("has_out", false) : (!oTime.isEmpty() && !"--".equals(oTime));

                                    String inPhoto = optCleanString(mObj, "in_photo_url", "in_photo", "photo_in", "in_image");
                                    String outPhoto = optCleanString(mObj, "out_photo_url", "out_photo", "photo_out", "out_image");

                                    moveRecords.add(new MoveRecordModel(
                                            mId,
                                            eId,
                                            fName,
                                            pName,
                                            iTime,
                                            oTime,
                                            hasIn,
                                            hasOut,
                                            inPhoto,
                                            outPhoto
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

    private static String optCleanString(JSONObject obj, String... keys) {
        if (obj == null) return "";
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                String val = obj.optString(key, "").trim();
                if (!val.isEmpty() && !"null".equalsIgnoreCase(val) && !"--".equals(val)) {
                    return val;
                }
            }
        }
        return "";
    }

    /** Resolves worker identity from JSON record. */
    public static String resolveWorkerId(JSONObject record, String supervisorUid) {
        if (record == null) return "";

        // 1. Direct explicit worker ID keys
        String workerId = optCleanString(record,
                "empid", "emp_id", "employee_id", "employeeid", "employeeId",
                "worker_id", "workerid", "workerId", "labor_id", "laborid",
                "labour_id", "labourid", "subcontractor_id", "subcontractorId",
                "sub_id", "employee_code", "emp_code", "employee_no",
                "employee_number", "emp_no", "member_id");
        if (!workerId.isEmpty()) {
            return workerId;
        }

        // 2. Nested objects containing employee info
        JSONObject workerObj = record.optJSONObject("employee");
        if (workerObj == null) workerObj = record.optJSONObject("worker");
        if (workerObj == null) workerObj = record.optJSONObject("employee_details");
        if (workerObj == null) workerObj = record.optJSONObject("labour");
        if (workerObj == null) workerObj = record.optJSONObject("labor");
        if (workerObj == null) workerObj = record.optJSONObject("user");
        workerId = optCleanString(workerObj, "id", "empid", "emp_id", "employee_id", "employeeid", "worker_id", "workerid");
        if (!workerId.isEmpty()) {
            return workerId;
        }

        // 3. If "attend_id" / "attendance_id" / "record_id" is present, then "id" is the worker ID!
        String attendIdKey = optCleanString(record, "attend_id", "attendance_id", "attendance_record_id", "attendanceid", "record_id");
        String rawId = optCleanString(record, "id");
        if (!attendIdKey.isEmpty() && !rawId.isEmpty() && !rawId.equalsIgnoreCase(attendIdKey)) {
            return rawId;
        }

        // 4. Check "uid", "userid", "user_id" (ignore if it equals supervisorUid)
        String uidVal = optCleanString(record, "uid");
        if (!uidVal.isEmpty()) {
            if (supervisorUid == null || supervisorUid.trim().isEmpty() || !uidVal.equalsIgnoreCase(supervisorUid.trim())) {
                return uidVal;
            }
        }

        String userIdVal = optCleanString(record, "userid", "user_id");
        if (!userIdVal.isEmpty()) {
            if (supervisorUid == null || supervisorUid.trim().isEmpty() || !userIdVal.equalsIgnoreCase(supervisorUid.trim())) {
                return userIdVal;
            }
        }

        // 5. Fallback rawId
        if (!rawId.isEmpty() && (attendIdKey.isEmpty() || !rawId.equalsIgnoreCase(attendIdKey))) {
            return rawId;
        }

        return "";
    }
}
