package com.app.fourscontracting.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data model for a worker displayed in the native Labour Management screen.
 * Parsed directly from api_labour_manager.php JSON response.
 */
public class LabourEmployeeModel {
    private final String id;
    private final String name;
    private final String statusCode;
    private final String displayText;
    private final String siteName;
    private final String photoUrl;
    private final boolean canIn;
    private final boolean canOut;
    private final String department;
    private final boolean hasIn;
    private final boolean hasOut;
    private final String timeIn;
    private final String timeOut;

    public LabourEmployeeModel(JSONObject json) throws JSONException {
        this.id = json.optString("id", "");
        this.name = json.optString("name", json.optString("first_name", "Employee"));
        this.statusCode = json.optString("status_code", "");
        this.displayText = json.optString("display_text", "");
        this.siteName = json.optString("site_name", "");
        String rawPhoto = json.optString("photo", json.optString("photo_url", "")).trim();
        if ((rawPhoto.isEmpty() || "null".equalsIgnoreCase(rawPhoto)) && !this.id.isEmpty()) {
            rawPhoto = ApiConfig.SUBCONTRACTOR + "/get_photo.php?id=" + this.id;
        } else if (!rawPhoto.isEmpty() && !rawPhoto.startsWith("http://") && !rawPhoto.startsWith("https://")) {
            if (rawPhoto.startsWith("/")) {
                rawPhoto = ApiConfig.HOST + rawPhoto;
            } else {
                rawPhoto = ApiConfig.SUBCONTRACTOR + "/" + rawPhoto;
            }
        }
        if (rawPhoto.startsWith("http://")) {
            rawPhoto = rawPhoto.replace("http://", "https://");
        }
        this.photoUrl = rawPhoto;
        this.canIn = json.optBoolean("can_in", false);
        this.canOut = json.optBoolean("can_out", false);
        this.department = json.optString("department_name", json.optString("dept", ""));
        this.hasIn = json.optBoolean("has_in", json.optBoolean("is_in", false));
        this.hasOut = json.optBoolean("has_out", json.optBoolean("is_out", false));
        this.timeIn = json.optString("time_in", json.optString("timeIn", "")).trim();
        this.timeOut = json.optString("time_out", json.optString("timeOut", "")).trim();
    }

    public LabourEmployeeModel(String id, String name, String statusCode, String displayText,
                               String siteName, String photoUrl, boolean canIn, boolean canOut,
                               String department) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.statusCode = statusCode != null ? statusCode : "";
        this.displayText = displayText != null ? displayText : "";
        this.siteName = siteName != null ? siteName : "";
        String rawPhoto = photoUrl != null ? photoUrl.trim() : "";
        if ((rawPhoto.isEmpty() || "null".equalsIgnoreCase(rawPhoto)) && !this.id.isEmpty()) {
            rawPhoto = ApiConfig.SUBCONTRACTOR + "/get_photo.php?id=" + this.id;
        } else if (!rawPhoto.isEmpty() && !rawPhoto.startsWith("http://") && !rawPhoto.startsWith("https://")) {
            if (rawPhoto.startsWith("/")) {
                rawPhoto = ApiConfig.HOST + rawPhoto;
            } else {
                rawPhoto = ApiConfig.SUBCONTRACTOR + "/" + rawPhoto;
            }
        }
        if (rawPhoto.startsWith("http://")) {
            rawPhoto = rawPhoto.replace("http://", "https://");
        }
        this.photoUrl = rawPhoto;
        this.canIn = canIn;
        this.canOut = canOut;
        this.department = department != null ? department : "";
        this.hasIn = false;
        this.hasOut = false;
        this.timeIn = "";
        this.timeOut = "";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatusCode() { return statusCode; }
    public String getDisplayText() { return displayText; }
    public String getSiteName() { return siteName; }
    public String getPhotoUrl() { return photoUrl; }
    public boolean isCanIn() { return canIn; }
    public boolean isCanOut() { return canOut; }
    public String getDepartment() { return department; }
    public boolean isHasIn() { return hasIn; }
    public boolean isHasOut() { return hasOut; }
    public String getTimeIn() { return timeIn; }
    public String getTimeOut() { return timeOut; }

    public boolean isClockedIn() {
        if (canOut) return true;
        String code = statusCode != null ? statusCode.trim().toUpperCase() : "";
        String text = displayText != null ? displayText.trim().toUpperCase() : "";

        if ("IN".equals(code) || "IN_HERE".equals(code) || "TIME_IN".equals(code) || "PRESENT".equals(code)) {
            return true;
        }
        if (text.contains("TIME IN") || text.contains("PUNCHED IN") || text.contains("CHECKED IN") || text.contains("PRESENT")) {
            return true;
        }
        if (hasIn || (!timeIn.isEmpty() && !"--".equals(timeIn) && !"--:--".equals(timeIn))) {
            return true;
        }
        return false;
    }

    public boolean isClockedOut() {
        String code = statusCode != null ? statusCode.trim().toUpperCase() : "";
        String text = displayText != null ? displayText.trim().toUpperCase() : "";

        if ("OUT".equals(code) || "OUT_HERE".equals(code) || "TIME_OUT".equals(code) || "PUNCH_OUT".equals(code) || "COMPLETED".equals(code) || "SHIFT_FINISHED".equals(code) || "FINISHED".equals(code)) {
            return true;
        }
        if (text.contains("TIME OUT") || text.contains("PUNCHED OUT") || text.contains("CHECKED OUT") || text.contains("COMPLETED") || text.contains("SHIFT FINISHED") || text.contains("FINISHED")) {
            return true;
        }
        if (hasOut || (!timeOut.isEmpty() && !"--".equals(timeOut) && !"--:--".equals(timeOut))) {
            return true;
        }
        return false;
    }
}
