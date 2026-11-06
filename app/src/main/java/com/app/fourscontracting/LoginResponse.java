package com.app.fourscontracting;

import org.json.JSONObject;

public class LoginResponse {
    public String status;
    public String uid;
    public String flag;
    public String username;
    public String email;
    public String staffid;
    public String userrole;
    public String has_project_allocation; // ADD
    public String message;
    public String start_lat_lng;
    public String end_lat_lng;

    public LoginResponse() {
        this.status = "";
        this.uid = "";
        this.flag = "1";
        this.username = "";
        this.email = "";
        this.staffid = "";
        this.userrole = "";
        this.has_project_allocation = "0";
        this.message = "";
        this.start_lat_lng = "0.0,0.0";
        this.end_lat_lng = "0.0,0.0";
    }

    public static LoginResponse fromJson(String raw) {
        LoginResponse res = new LoginResponse();
        if (raw == null || raw.trim().isEmpty()) return res;
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                res.status = obj.optString("status", "");
                res.uid = obj.optString("uid", obj.optString("id", obj.optString("user_id", "")));
                res.flag = obj.optString("flag", "1");
                res.username = obj.optString("username", obj.optString("displayName", obj.optString("name", "")));
                res.email = obj.optString("email", "");
                res.staffid = obj.optString("staffid", obj.optString("staff_id", ""));
                res.userrole = obj.optString("userrole", obj.optString("role", ""));

                if (obj.has("has_project_allocation")) {
                    res.has_project_allocation = obj.optString("has_project_allocation", "0");
                } else {
                    org.json.JSONArray projArr = obj.optJSONArray("projects");
                    String projStr = obj.optString("projects", obj.optString("projname", ""));
                    if ((projArr != null && projArr.length() > 0) || (!projStr.isEmpty() && !projStr.equals("[]"))) {
                        res.has_project_allocation = "1";
                    } else {
                        res.has_project_allocation = "0";
                    }
                }

                res.message = obj.optString("message", "");
                res.start_lat_lng = obj.optString("start_lat_lng", "0.0,0.0");
                res.end_lat_lng = obj.optString("end_lat_lng", "0.0,0.0");
            } catch (Exception e) {
                res.message = e.getMessage();
            }
        }
        return res;
    }
}

