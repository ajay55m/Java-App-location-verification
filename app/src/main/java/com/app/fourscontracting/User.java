package com.app.fourscontracting;

import org.json.JSONObject;

public class User {
    public String username; // Raw JSON response or username for backward compatibility
    public String uid = "";
    public String staffId = "";
    public String userRole = "";
    public String flag = "1";
    public String email = "";
    public String startLatLng = "0.0,0.0";
    public String endLatLng = "0.0,0.0";
    public String displayName = "";
    public String hasProjectAllocation = "0";

    public User(String username) {
        this.username = username != null ? username : "";
        parseJson(this.username);
    }

    private void parseJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                this.uid = obj.optString("uid", obj.optString("id", obj.optString("user_id", "")));
                this.staffId = obj.optString("staffid", obj.optString("staff_id", ""));
                this.userRole = obj.optString("userrole", obj.optString("role", ""));
                this.flag = obj.optString("flag", "1");
                this.displayName = obj.optString("username", obj.optString("displayName", obj.optString("name", "")));
                this.email = obj.optString("email", "");
                this.startLatLng = obj.optString("start_lat_lng", "0.0,0.0");
                this.endLatLng = obj.optString("end_lat_lng", "0.0,0.0");
                if (obj.has("has_project_allocation")) {
                    this.hasProjectAllocation = obj.optString("has_project_allocation", "0");
                } else {
                    org.json.JSONArray projArr = obj.optJSONArray("projects");
                    String projStr = obj.optString("projects", obj.optString("projname", ""));
                    if ((projArr != null && projArr.length() > 0) || (!projStr.isEmpty() && !projStr.equals("[]"))) {
                        this.hasProjectAllocation = "1";
                    } else {
                        this.hasProjectAllocation = "0";
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
