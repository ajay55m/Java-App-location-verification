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
        if (context != null) {
            userLocalDatabase = context.getSharedPreferences(SP_NAME, 0);
        }
    }

    public void storeUserData(User user) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("username", user.username);
        spEditor.apply();
    }

    public void storeUserProjectData(UserProject user) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("projectname", user.projectname);
        spEditor.apply();
    }

    public void storeUserProjectID(String projectId) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("selected_project_id", projectId);
        spEditor.apply();
    }

    public String getLoggedInUserProjectID() {
        String id = userLocalDatabase != null ? userLocalDatabase.getString("selected_project_id", "") : "";
        if ((id == null || id.isEmpty()) && context != null) {
            SessionPrefs session = new SessionPrefs(context);
            id = session.getProjectId();
        }
        return id != null ? id : "";
    }

    public void storeUserLocationData(UserLocation user) {
        if (userLocalDatabase == null) return;
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("locationname", user.locationname);
        spEditor.apply();
    }

    public User getLoggedInUser() {
        String username = userLocalDatabase != null ? userLocalDatabase.getString("username", "") : "";
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
        String projectname = userLocalDatabase != null ? userLocalDatabase.getString("projectname", "") : "";
        if ((projectname == null || projectname.isEmpty()) && context != null) {
            SessionPrefs session = new SessionPrefs(context);
            projectname = session.getProjectName();
        }
        return new UserProject(projectname != null ? projectname : "");
    }

    public UserLocation getLoggedInUserLocation() {
        String locationname = userLocalDatabase != null ? userLocalDatabase.getString("locationname", "") : "";
        return new UserLocation(locationname != null ? locationname : "");
    }

    public void setUserLoggedIn(boolean loggedIn) {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putBoolean("loggedIn", loggedIn);
        spEditor.apply();
    }

    public boolean getUserLoggedIn() {
        if(userLocalDatabase.getBoolean("loggedIn", false) == true) {
            return true;
        } else {
            return false;
        }
    }

    public void updateAssignedProjects(java.util.List<String> newProjects, Context context) {
        String usernameRaw = userLocalDatabase.getString("username", "");
        if (usernameRaw == null || usernameRaw.isEmpty()) {
            return;
        }

        String updatedRaw = usernameRaw;
        if (usernameRaw.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(usernameRaw);
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String p : newProjects) {
                    arr.put(p);
                }
                obj.put("projects", arr);
                updatedRaw = obj.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // fallback for ## format
            String[] parts = usernameRaw.split("##");
            if (parts.length >= 4) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < newProjects.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(newProjects.get(i));
                }
                parts[3] = sb.toString();

                // Reconstruct raw string
                StringBuilder recon = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) recon.append("##");
                    recon.append(parts[i]);
                }
                updatedRaw = recon.toString();
            }
        }

        // Save to userLocalDatabase (userDetails)
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("username", updatedRaw);
        spEditor.apply();

        // Save to default shared preferences
        try {
            SharedPreferences settings = android.preference.PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("username", updatedRaw);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearUserData() {
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.clear();
        spEditor.apply();
    }

    public static String[] parseUserInfo(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return new String[]{"", "0", "0", "", "", ""};
        }

        if (rawData.trim().startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(rawData);
                String uid = obj.optString("uid", "");
                if (uid.isEmpty()) {
                    uid = obj.optString("id", "");
                }
                String role = obj.optString("role", "");
                if (role.isEmpty() || role.equals("0")) {
                    role = obj.optString("flag", "0");
                }
                String displayName = obj.optString("displayName", "");
                if (displayName.isEmpty()) {
                    displayName = obj.optString("name", "");
                }
                if (displayName.isEmpty()) {
                    displayName = obj.optString("username", "");
                }
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
                    projects = obj.optString("projects", "");
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
        String[] parts = rawData.split("##");
        if (parts.length >= 6) {
            return parts;
        }

        // Safe padded fallback
        String[] result = new String[]{"", "0", "0", "", "", ""};
        for (int i = 0; i < parts.length && i < 6; i++) {
            result[i] = parts[i];
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
        if (trimmed.equalsIgnoreCase("error")) {
            return false;
        }
        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                String status = obj.optString("status", "");
                if ("error".equalsIgnoreCase(status)) {
                    return false;
                }
                if ("success".equalsIgnoreCase(status)) {
                    return true;
                }
                String uid = obj.optString("uid", obj.optString("id", ""));
                if (!uid.isEmpty() && !"error".equalsIgnoreCase(uid)) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        if (trimmed.contains("##")) {
            String[] parts = trimmed.split("##");
            if (parts.length >= 3 && !parts[0].equalsIgnoreCase("error")) {
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
                if ("error".equalsIgnoreCase(status)) {
                    return "Invalid credentials";
                }
            } catch (Exception ignored) {}
        }
        return "Invalid Username or Password";
    }

}

