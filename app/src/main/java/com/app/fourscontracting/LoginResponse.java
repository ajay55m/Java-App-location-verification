package com.app.fourscontracting;

import org.json.JSONObject;

public class LoginResponse {
    public String status, message, uid, flag, username, email, staffid, userrole, has_project_allocation;
    public String department_id, dept_id, employee_department_id;
    public String employee_id, employee_number, employee_name, company_id, work_location_id;
    public String start_lat_lng, end_lat_lng;

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

    public int getDeptId() {
        String val = department_id != null ? department_id : (dept_id != null ? dept_id : employee_department_id);
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
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

                res.department_id = obj.optString("department_id", obj.optString("dept_id", obj.optString("employee_department_id", "")));
                res.dept_id = res.department_id;
                res.employee_department_id = res.department_id;

                res.employee_id = obj.optString("employee_id", res.staffid);
                res.employee_number = obj.optString("employee_number", "");
                res.employee_name = obj.optString("employee_name", res.username);
                res.company_id = obj.optString("company_id", "");
                res.work_location_id = obj.optString("work_location_id", "");

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
