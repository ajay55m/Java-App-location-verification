package com.app.fourscontracting.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.fourscontracting.LoginResponse;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.UserLocalStore;

public class SessionManager {
    private final SharedPreferences pref;
    private final Context context;

    public SessionManager(Context c) {
        this.context = c != null ? c.getApplicationContext() : null;
        this.pref = context != null
                ? context.getSharedPreferences("app", Context.MODE_PRIVATE)
                : null;
    }

    public void save(String key, String value) {
        if (pref == null) return;
        pref.edit().putString(key, value != null ? value : "").apply();
    }

    public void save(String key, int value) {
        if (pref == null) return;
        pref.edit().putInt(key, value).apply();
    }

    public void saveLogin(LoginResponse r) {
        if (pref == null || r == null) return;
        String effEmpId = r.empid != null && !r.empid.isEmpty()
                ? r.empid
                : (r.staffid != null && !r.staffid.isEmpty() ? r.staffid : r.employee_id);
        if (effEmpId == null) effEmpId = "";

        String locId = (r.work_location_id != null && !r.work_location_id.isEmpty())
                ? r.work_location_id
                : "";

        pref.edit()
            .putString("uid", r.uid != null ? r.uid : "")
            .putString("empid", effEmpId)
            .putString("staffid", effEmpId)
            .putString("username", r.username != null ? r.username : "")
            .putString("employee_name", r.employee_name != null ? r.employee_name : "")
            .putString("employee_number", r.employee_number != null ? r.employee_number : "")
            .putString("location_id", locId)
            .putInt("dept_id", r.getDeptId())
            .putString("dept_id_str", String.valueOf(r.getDeptId()))
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
        if (pref == null) return 31;
        String emp = pref.getString("empid", "");
        if (emp.isEmpty() || "0".equals(emp)) {
            emp = pref.getString("staffid", "");
        }
        if (emp.isEmpty() || "0".equals(emp)) {
            emp = pref.getString("employee_id", "0");
        }
        try {
            int val = Integer.parseInt(emp);
            return val > 0 ? val : 31;
        } catch (Exception e) {
            return 31;
        }
    }

    public void setEmpId(int empId) {
        if (pref == null || empId <= 0) return;
        pref.edit()
            .putString("empid", String.valueOf(empId))
            .putString("staffid", String.valueOf(empId))
            .apply();
    }

    public String getEmpName() {
        if (pref == null) return "";
        return pref.getString("employee_name", "");
    }

    public int getLocationId() {
        if (pref == null) return 12;
        int loc = 0;
        try {
            loc = Integer.parseInt(pref.getString("location_id", "0"));
        } catch (Exception ignored) {}
        if (loc <= 0) {
            loc = pref.getInt("location_id", 0);
        }
        if (loc <= 0 && context != null) {
            SessionPrefs sp = new SessionPrefs(context);
            try {
                loc = Integer.parseInt(sp.getLocationId());
            } catch (Exception ignored) {}
        }
        if (loc <= 0) {
            loc = 12;
        }
        return loc;
    }

    public int getDeptId() {
        if (pref == null) return 5;
        int d = pref.getInt("dept_id", 0);
        if (d <= 0) {
            String s = pref.getString("dept_id_str", "");
            try { d = Integer.parseInt(s); } catch (Exception ignored) {}
        }
        if (d <= 0) {
            String s = pref.getString("department_id", "");
            try { d = Integer.parseInt(s); } catch (Exception ignored) {}
        }
        if (d <= 0 && context != null) {
            d = new UserLocalStore(context).getDeptId();
        }
        if (d <= 0) {
            d = 5;
        }
        return d;
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

