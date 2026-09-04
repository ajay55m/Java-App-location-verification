package com.app.fourscontracting;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Single place for app session: verified site + selected project.
 * Backed by existing "AppPrefs" keys (no migration break).
 */
public final class SessionPrefs {

    public static final String PREFS_NAME = "AppPrefs";
    public static final long LOCATION_SESSION_MS = 24L * 60 * 60 * 1000;
    public static final long VERIFY_TOKEN_MS = 5L * 60 * 1000;

    private static final String KEY_LOCATION_ID = "last_verified_location_id";
    private static final String KEY_LOCATION_TIME = "last_verification_timestamp";
    private static final String KEY_TOKEN = "location_verification_token";
    private static final String KEY_TOKEN_TIME = "location_verification_timestamp";
    private static final String KEY_PROJECT_ID = "selected_project_id";
    private static final String KEY_PROJECT_NAME = "selected_project_name";
    private static final String KEY_BREAK_HOURS = "selected_project_break_hours";

    private static final String KEY_PROJECT_CACHE = "cached_assigned_projects_json";
    private static final String KEY_PROJECT_CACHE_TIME = "cached_assigned_projects_time";
    public static final long PROJECT_CACHE_MS = 30L * 60 * 1000; // 30 minutes

    private final SharedPreferences prefs;
    private final Context appContext;

    public SessionPrefs(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public SharedPreferences raw() {
        return prefs;
    }

    public String getLocationId() {
        return prefs.getString(KEY_LOCATION_ID, "");
    }

    public long getLocationVerifiedAt() {
        return prefs.getLong(KEY_LOCATION_TIME, 0L);
    }

    public boolean isLocationSessionValid() {
        String id = getLocationId();
        if (TextUtils.isEmpty(id)) return false;
        long verifiedAt = getLocationVerifiedAt();
        long now = System.currentTimeMillis();
        long age = now - verifiedAt;
        if (age < 0 || age > LOCATION_SESSION_MS) return false;
        return isSameCalendarDay(verifiedAt, now);
    }

    public static boolean isSameCalendarDay(long time1, long time2) {
        if (time1 <= 0 || time2 <= 0) return false;
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
                && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public String getProjectId() {
        return prefs.getString(KEY_PROJECT_ID, "");
    }

    public String getProjectName() {
        return prefs.getString(KEY_PROJECT_NAME, "");
    }

    public String getBreakHours() {
        String v = prefs.getString(KEY_BREAK_HOURS, "");
        if (v == null || "EMPTY_IN_PREFS".equals(v) || "null".equalsIgnoreCase(v)) {
            return "";
        }
        return v;
    }

    public String getVerificationToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public boolean isVerificationTokenValid(String intentToken) {
        if (intentToken == null || intentToken.isEmpty()) return false;
        String saved = getVerificationToken();
        if (!intentToken.equals(saved)) return false;
        long age = System.currentTimeMillis() - prefs.getLong(KEY_TOKEN_TIME, 0L);
        return age >= 0 && age <= VERIFY_TOKEN_MS;
    }

    /** After successful GPS match — locks site for 24h and issues 5‑min punch token. */
    public void saveVerifiedLocation(String locationId, String locationName, String secureToken) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_TOKEN, secureToken != null ? secureToken : "")
                .putLong(KEY_TOKEN_TIME, now)
                .putString(KEY_LOCATION_ID, locationId != null ? locationId : "")
                .putLong(KEY_LOCATION_TIME, now)
                .apply();

        if (locationName != null && !locationName.isEmpty()) {
            new UserLocalStore(appContext).storeUserLocationData(new UserLocation(locationName));
        }
    }

    public void saveProject(String projectId, String projectName, String breakHours) {
        prefs.edit()
                .putString(KEY_PROJECT_ID, projectId != null ? projectId : "")
                .putString(KEY_PROJECT_NAME, projectName != null ? projectName : "")
                .putString(KEY_BREAK_HOURS, breakHours != null ? breakHours : "")
                .apply();
        appContext.getSharedPreferences("userDetails", Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROJECT_ID, projectId != null ? projectId : "")
                .apply();
    }

    public void clearVerificationToken() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_TOKEN_TIME)
                .apply();
    }

    public void saveProjectListCache(java.util.List<Project> projects) {
        if (projects == null) return;
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Project p : projects) {
                if (p == null) continue;
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("id", p.id != null ? p.id : "");
                o.put("name", p.name != null ? p.name : "");
                o.put("break_hours", p.breakHours != null ? p.breakHours : "");
                arr.put(o);
            }
            prefs.edit()
                    .putString(KEY_PROJECT_CACHE, arr.toString())
                    .putLong(KEY_PROJECT_CACHE_TIME, System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public java.util.List<Project> getCachedProjectList() {
        return getCachedProjectList(true);
    }

    public java.util.List<Project> getCachedProjectList(boolean allowStale) {
        java.util.List<Project> out = new java.util.ArrayList<>();
        if (!allowStale) {
            long age = System.currentTimeMillis() - prefs.getLong(KEY_PROJECT_CACHE_TIME, 0L);
            if (age < 0 || age > PROJECT_CACHE_MS) return out;
        }
        String raw = prefs.getString(KEY_PROJECT_CACHE, "");
        if (TextUtils.isEmpty(raw)) return out;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String name = o.optString("name", "");
                if (name.isEmpty()) continue;
                out.add(new Project(o.optString("id", ""), name, o.optString("break_hours", "")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
