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
}

