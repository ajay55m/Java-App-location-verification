package com.app.fourscontracting;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences pref;

    public SessionManager(Context c) {
        pref = c != null
                ? c.getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE)
                : null;
    }

    public void saveLogin(LoginResponse r) {
        if (pref == null || r == null) return;
        pref.edit()
            .putString("uid", r.uid != null ? r.uid : "")
            .putString("staffid", r.staffid != null ? r.staffid : "")
            .putString("username", r.username != null ? r.username : "")
            .putString("employee_name", r.employee_name != null ? r.employee_name : "")
            .putString("employee_number", r.employee_number != null ? r.employee_number : "")
            .putInt("dept_id", r.getDeptId())
            .putString("has_project_allocation", r.has_project_allocation != null ? r.has_project_allocation : "1")
            .apply();
    }

    public int getUid() {
        if (pref == null) return 0;
        try {
            return Integer.parseInt(pref.getString("uid", "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getEmpId() {
        if (pref == null) return 0;
        try {
            return Integer.parseInt(pref.getString("staffid", "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getDeptId() {
        if (pref == null) return 0;
        return pref.getInt("dept_id", 0);
    }

    public String getUsername() {
        if (pref == null) return "";
        return pref.getString("username", "");
    }

    public void logout() {
        if (pref != null) {
            pref.edit().clear().apply();
        }
    }
}
