package com.app.fourscontracting.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Attendance punch payload posted to multi-savetimein.php.
 */
public class AttendancePayload {
    public final String clientRequestId;
    public String uid = "";
    public String empid = "";
    public String timein = "";
    public String imagepath = "";
    /** Wire type: IN, OUT, or MOVE */
    public String type = "IN";
    public boolean isMovement = false;
    public String projectId = "";
    public String projname = "";
    public String locationId = "";
    public String breakHours = "";
    public double lat = Double.NaN;
    public double lng = Double.NaN;
    public float accuracy = -1f;
    public long gpsTimeMs = 0L;

    public AttendancePayload() {
        this.clientRequestId = UUID.randomUUID().toString();
    }

    public AttendancePayload(String clientRequestId) {
        this.clientRequestId = clientRequestId != null && !clientRequestId.isEmpty()
                ? clientRequestId
                : UUID.randomUUID().toString();
    }

    public Map<String, String> toFormParams() {
        Map<String, String> params = new HashMap<>();
        params.put("uid", nullToEmpty(uid));
        params.put("empid", nullToEmpty(empid));
        params.put("timein", nullToEmpty(timein));
        params.put("imagepath", nullToEmpty(imagepath));

        // Legacy PHP expects IN/OUT. Movement OUT must stay OUT (not forced to IN).
        String wireType;
        if (type != null && type.equalsIgnoreCase("OUT")) {
            wireType = "OUT";
        } else if (isMovement) {
            wireType = "IN";
        } else if (type != null && type.equalsIgnoreCase("IN")) {
            wireType = "IN";
        } else {
            wireType = "IN";
        }
        params.put("type", wireType);
        params.put("is_movement", isMovement ? "1" : "0");
        if (isMovement) {
            params.put("action_type", "OUT".equals(wireType) ? "MOVE_OUT" : "MOVE");
        } else {
            params.put("action_type", wireType);
        }

        params.put("project_id", nullToEmpty(projectId));
        params.put("departmentid", nullToEmpty(projectId));
        params.put("projname", nullToEmpty(projname));

        String loc = nullToEmpty(locationId);
        params.put("location_id", loc);
        params.put("locationid", loc);
        params.put("loc_id", loc);
        params.put("matched_loc_id", loc);

        params.put("break_hours", nullToEmpty(breakHours));
        params.put("client_request_id", clientRequestId);

        // Server GPS check fields
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
            params.put("lat", String.valueOf(lat));
            params.put("lng", String.valueOf(lng));
            params.put("latitude", String.valueOf(lat));
            params.put("longitude", String.valueOf(lng));
        }
        if (accuracy >= 0f) {
            params.put("accuracy", String.valueOf(accuracy));
            params.put("gps_accuracy", String.valueOf(accuracy));
        }
        if (gpsTimeMs > 0L) {
            params.put("gps_time", String.valueOf(gpsTimeMs));
        }
        return params;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("client_request_id", clientRequestId);
        o.put("uid", nullToEmpty(uid));
        o.put("empid", nullToEmpty(empid));
        o.put("timein", nullToEmpty(timein));
        o.put("imagepath", nullToEmpty(imagepath));
        o.put("type", nullToEmpty(type));
        o.put("is_movement", isMovement);
        o.put("project_id", nullToEmpty(projectId));
        o.put("projname", nullToEmpty(projname));
        o.put("location_id", nullToEmpty(locationId));
        o.put("break_hours", nullToEmpty(breakHours));
        if (!Double.isNaN(lat)) o.put("lat", lat);
        if (!Double.isNaN(lng)) o.put("lng", lng);
        if (accuracy >= 0f) o.put("accuracy", accuracy);
        if (gpsTimeMs > 0L) o.put("gps_time", gpsTimeMs);
        return o;
    }

    public static AttendancePayload fromJson(JSONObject o) {
        AttendancePayload p = new AttendancePayload(o.optString("client_request_id", ""));
        p.uid = o.optString("uid", "");
        p.empid = o.optString("empid", "");
        p.timein = o.optString("timein", "");
        p.imagepath = o.optString("imagepath", "");
        p.type = o.optString("type", "IN");
        p.isMovement = o.optBoolean("is_movement", false);
        p.projectId = o.optString("project_id", "");
        p.projname = o.optString("projname", "");
        p.locationId = o.optString("location_id", "");
        p.breakHours = o.optString("break_hours", "");
        if (o.has("lat")) p.lat = o.optDouble("lat", Double.NaN);
        if (o.has("lng")) p.lng = o.optDouble("lng", Double.NaN);
        if (o.has("accuracy")) p.accuracy = (float) o.optDouble("accuracy", -1);
        if (o.has("gps_time")) p.gpsTimeMs = o.optLong("gps_time", 0L);
        return p;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
