package com.app.fourscontracting;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public class UserLocalStore {
    public static final String SP_NAME = "userDetails";
    SharedPreferences userLocalDatabase;
    private Context context;

    public UserLocalStore(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        if (this.context != null) {
            userLocalDatabase = this.context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
    }

    private SharedPreferences getPrefs() {
        if (userLocalDatabase != null) return userLocalDatabase;
        if (context != null) {
            userLocalDatabase = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        return userLocalDatabase;
    }

    public void storeUserData(User user) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null || user == null) return;
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("username", user.username != null ? user.username : "");
        if (user.uid != null && !user.uid.isEmpty()) {
            spEditor.putString("uid", user.uid);
            spEditor.putString("user_id", user.uid);
        }
        if (user.staffId != null && !user.staffId.isEmpty()) {
            spEditor.putString("staffid", user.staffId);
            spEditor.putString("staff_id", user.staffId);
        }
        if (user.userRole != null && !user.userRole.isEmpty()) {
            spEditor.putString("userrole", user.userRole);
            spEditor.putString("role", user.userRole);
        }
        if (user.flag != null && !user.flag.isEmpty()) {
            spEditor.putString("flag", user.flag);
        }
        if (user.displayName != null && !user.displayName.isEmpty()) {
            spEditor.putString("display_name", user.displayName);
        }
        if (user.email != null) {
            spEditor.putString("email", user.email);
        }
        if (user.startLatLng != null) {
            spEditor.putString("start_lat_lng", user.startLatLng);
        }
        if (user.endLatLng != null) {
            spEditor.putString("end_lat_lng", user.endLatLng);
        }
        if (user.hasProjectAllocation != null) {
            spEditor.putString("has_project_allocation", user.hasProjectAllocation);
        }
        spEditor.putInt("dept_id", user.deptId);
        spEditor.apply();

        if (context != null) {
            try {
                context.getSharedPreferences("app", Context.MODE_PRIVATE).edit()
                    .putString("staffid", user.staffId != null ? user.staffId : "")
                    .putString("username", user.displayName != null && !user.displayName.isEmpty() ? user.displayName : user.username)
                    .putString("has_project_allocation", user.hasProjectAllocation != null ? user.hasProjectAllocation : "0")
                    .putInt("dept_id", user.deptId)
                    .apply();
            } catch (Exception ignored) {}
        }
    }

    public int getDeptId() {
        SharedPreferences prefs = getPrefs();
        int d = prefs != null ? prefs.getInt("dept_id", 0) : 0;
        if (d == 0 && context != null) {
            try {
                d = context.getSharedPreferences("app", Context.MODE_PRIVATE).getInt("dept_id", 0);
            } catch (Exception ignored) {}
        }
        return d;
    }

    public String getHasProjectAllocation() {
        SharedPreferences prefs = getPrefs();
        String alloc = prefs != null ? prefs.getString("has_project_allocation", "") : "";
        if (alloc.isEmpty() && context != null) {
            try {
                alloc = context.getSharedPreferences("app", Context.MODE_PRIVATE).getString("has_project_allocation", "0");
            } catch (Exception ignored) {}
        }
        return (alloc != null && !alloc.isEmpty()) ? alloc : "0";
    }

    public boolean hasProjectAllocation() {
        return "1".equals(getHasProjectAllocation());
    }

    public String getUid() {
        SharedPreferences prefs = getPrefs();
        String uid = prefs != null ? prefs.getString("uid", "") : "";
        if (uid.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.uid != null && !u.uid.isEmpty()) {
                uid = u.uid;
            } else {
                String raw = prefs != null ? prefs.getString("username", "") : "";
                String[] parts = parseUserInfo(raw);
                uid = parts.length > 0 ? parts[0] : "";
            }
        }
        return uid;
    }

    public String getStaffId() {
        SharedPreferences prefs = getPrefs();
        String s = prefs != null ? prefs.getString("staffid", "") : "";
        if (s.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.staffId != null) s = u.staffId;
        }
        return s;
    }

    public String getUserRole() {
        SharedPreferences prefs = getPrefs();
        String r = prefs != null ? prefs.getString("userrole", "") : "";
        if (r.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.userRole != null) r = u.userRole;
        }
        return r;
    }

    public String getFlag() {
        SharedPreferences prefs = getPrefs();
        String f = prefs != null ? prefs.getString("flag", "") : "";
        if (f.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.flag != null) f = u.flag;
        }
        return f;
    }

    public String getStartLatLng() {
        SharedPreferences prefs = getPrefs();
        String s = prefs != null ? prefs.getString("start_lat_lng", "") : "";
        if (s.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.startLatLng != null) s = u.startLatLng;
        }
        return s;
    }

    public String getEndLatLng() {
        SharedPreferences prefs = getPrefs();
        String s = prefs != null ? prefs.getString("end_lat_lng", "") : "";
        if (s.isEmpty()) {
            User u = getLoggedInUser();
            if (u != null && u.endLatLng != null) s = u.endLatLng;
        }
        return s;
    }

    public String getEmail() {
        SharedPreferences prefs = getPrefs();
        return prefs != null ? prefs.getString("email", "") : "";
    }

    public void storeUserProjectData(UserProject user) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null || user == null) return;
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("projectname", user.projectname != null ? user.projectname : "");
        spEditor.apply();
    }

    public void storeUserProjectID(String projectId) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("selected_project_id", projectId != null ? projectId : "");
        spEditor.apply();
    }

    public String getLoggedInUserProjectID() {
        SharedPreferences prefs = getPrefs();
        String id = prefs != null ? prefs.getString("selected_project_id", "") : "";
        if ((id == null || id.isEmpty()) && context != null) {
            try {
                SessionPrefs session = new SessionPrefs(context);
                id = session.getProjectId();
            } catch (Exception ignored) {}
        }
        return id != null ? id : "";
    }

    public void storeUserLocationData(UserLocation user) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null || user == null) return;
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString("locationname", user.locationname != null ? user.locationname : "");
        spEditor.apply();
    }

    public User getLoggedInUser() {
        SharedPreferences prefs = getPrefs();
        String username = prefs != null ? prefs.getString("username", "") : "";
        if ((username == null || username.isEmpty()) && context != null) {
            try {
                SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
                username = settings.getString("username", "");
            } catch (Exception ignored) {}
        }
        if ((username == null || username.isEmpty()) && context != null) {
            try {
                SharedPreferences appPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                username = appPrefs.getString("username", "");
            } catch (Exception ignored) {}
        }
        return new User(username != null ? username : "");
    }

    public UserProject getLoggedInUserProject() {
        SharedPreferences prefs = getPrefs();
        String projectname = prefs != null ? prefs.getString("projectname", "") : "";
        if ((projectname == null || projectname.isEmpty()) && context != null) {
            try {
                SessionPrefs session = new SessionPrefs(context);
                projectname = session.getProjectName();
            } catch (Exception ignored) {}
        }
        return new UserProject(projectname != null ? projectname : "");
    }

    public UserLocation getLoggedInUserLocation() {
        SharedPreferences prefs = getPrefs();
        String locationname = prefs != null ? prefs.getString("locationname", "") : "";
        return new UserLocation(locationname != null ? locationname : "");
    }

    public void setUserLoggedIn(boolean loggedIn) {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;
        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putBoolean("loggedIn", loggedIn);
        spEditor.apply();
    }

    public boolean getUserLoggedIn() {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return false;
        return prefs.getBoolean("loggedIn", false);
    }

    public void updateAssignedProjects(java.util.List<String> newProjects, Context context) {
        if (newProjects == null) return;
        SharedPreferences prefs = getPrefs();
        String usernameRaw = prefs != null ? prefs.getString("username", "") : "";
        if (usernameRaw == null || usernameRaw.isEmpty()) {
            return;
        }

        String updatedRaw = usernameRaw;
        if (usernameRaw.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(usernameRaw);
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String p : newProjects) {
                    if (p != null) arr.put(p);
                }
                obj.put("projects", arr);
                updatedRaw = obj.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // fallback for ## format
            String[] parts = usernameRaw.split("##");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < newProjects.size(); i++) {
                if (i > 0) sb.append(",");
                if (newProjects.get(i) != null) sb.append(newProjects.get(i));
            }
            if (parts.length >= 4) {
                parts[3] = sb.toString();
                StringBuilder recon = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) recon.append("##");
                    recon.append(parts[i]);
                }
                updatedRaw = recon.toString();
            } else {
                StringBuilder recon = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) recon.append("##");
                    recon.append(parts[i]);
                }
                while (recon.toString().split("##", -1).length < 3) {
                    recon.append("##");
                }
                recon.append("##").append(sb.toString());
                updatedRaw = recon.toString();
            }
        }

        if (prefs != null) {
            SharedPreferences.Editor spEditor = prefs.edit();
            spEditor.putString("username", updatedRaw);
            spEditor.apply();
        }

        Context ctx = context != null ? context.getApplicationContext() : this.context;
        if (ctx != null) {
            try {
                SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(ctx);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", updatedRaw);
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearUserData() {
        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            SharedPreferences.Editor spEditor = prefs.edit();
            spEditor.clear();
            spEditor.apply();
        }
    }

    public static String[] parseUserInfo(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            return new String[]{"", "0", "0", "", "", ""};
        }

        String trimmed = rawData.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                String uid = obj.optString("uid", "");
                if (uid.isEmpty()) uid = obj.optString("id", "");
                if (uid.isEmpty()) uid = obj.optString("user_id", "");
                if (uid.isEmpty()) uid = obj.optString("subadmin_id", "");
                if (uid.isEmpty()) uid = obj.optString("empid", "");
                if (uid.isEmpty()) uid = obj.optString("staffid", "");

                String role = obj.optString("flag", "");
                if (role.isEmpty() || role.equals("0")) role = obj.optString("userrole", "");
                if (role.isEmpty()) role = obj.optString("role", "");
                if (role.isEmpty()) role = obj.optString("user_type", "1");

                String displayName = obj.optString("username", "");
                if (displayName.isEmpty()) displayName = obj.optString("displayName", "");
                if (displayName.isEmpty()) displayName = obj.optString("name", "");
                if (displayName.isEmpty()) displayName = obj.optString("full_name", "");
                if (displayName.isEmpty()) displayName = obj.optString("subadmin_name", "");

                String projects = "";
                org.json.JSONArray projArray = obj.optJSONArray("projects");
                if (projArray != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < projArray.length(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(projArray.optString(i));
                    }
                    projects = sb.toString();
                } else {
                    projects = obj.optString("projects", obj.optString("projname", obj.optString("assigned_projects", "")));
                    if (projects.startsWith("[") && projects.endsWith("]")) {
                        try {
                            org.json.JSONArray arr = new org.json.JSONArray(projects);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < arr.length(); i++) {
                                if (i > 0) sb.append(",");
                                sb.append(arr.optString(i));
                            }
                            projects = sb.toString();
                        } catch (Exception ignored) {}
                    }
                }

                return new String[]{uid, role, displayName, projects, "", ""};
            } catch (Exception e) {
                // fall through to fallback
            }
        }

        // Standard ## split fallback
        String[] parts = trimmed.split("##", -1);
        String[] result = new String[]{"", "0", "0", "", "", ""};
        for (int i = 0; i < parts.length && i < 6; i++) {
            result[i] = parts[i] != null ? parts[i] : "";
        }
        return result;
    }

    private static String urlEncode(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    public static String buildEmployeeDetailsUrl(String uid, String projId, String projName, String locId) {
        String baseUrl = com.app.fourscontracting.data.ApiConfig.EMPLOYEE_DETAILS;
        String encUid = urlEncode(uid);
        String rawProjName = projName != null && !projName.isEmpty() ? projName : (projId != null ? projId : "");
        String rawProjId = projId != null && !projId.isEmpty() ? projId : rawProjName;
        String encProjName = urlEncode(rawProjName);
        String encProjId = urlEncode(rawProjId);

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?uid=").append(encUid);
        sb.append("&id=").append(encUid);
        sb.append("&projname=").append(encProjName);
        sb.append("&project_id=").append(encProjId);
        sb.append("&projid=").append(encProjId);
        if (locId != null && !locId.isEmpty()) {
            String encLoc = urlEncode(locId);
            sb.append("&location_id=").append(encLoc);
            sb.append("&loc_id=").append(encLoc);
        }
        return sb.toString();
    }

    public static boolean isLoginResponseSuccess(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        String trimmed = response.trim();
        if (trimmed.equalsIgnoreCase("error") || trimmed.equalsIgnoreCase("false") || trimmed.equalsIgnoreCase("failed")) {
            return false;
        }
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                String status = obj.optString("status", "");
                if ("error".equalsIgnoreCase(status) || "false".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
                    return false;
                }
                if ("success".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status) || obj.optBoolean("success", false)) {
                    return true;
                }
                String uid = obj.optString("uid", obj.optString("id", obj.optString("user_id", obj.optString("subadmin_id", ""))));
                if (!uid.isEmpty() && !"error".equalsIgnoreCase(uid) && !"0".equalsIgnoreCase(uid)) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        if (trimmed.contains("##")) {
            String[] parts = trimmed.split("##", -1);
            if (parts.length >= 1 && !parts[0].equalsIgnoreCase("error") && !parts[0].equalsIgnoreCase("false") && !parts[0].trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static String getLoginErrorMessage(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "Invalid Username or Password";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                String msg = obj.optString("message", "");
                if (!msg.isEmpty()) {
                    return msg;
                }
                String status = obj.optString("status", "");
                if ("error".equalsIgnoreCase(status) || "false".equalsIgnoreCase(status)) {
                    return "Invalid credentials";
                }
            } catch (Exception ignored) {}
        }
        return "Invalid Username or Password";
    }

}


