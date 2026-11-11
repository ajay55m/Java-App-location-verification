package com.app.fourscontracting.data;

/**
 * Single place for backend base URLs (debug/release can diverge later).
 */
public final class ApiConfig {
    private ApiConfig() {}

    public static final String HOST = "https://4scontracting.com";
    public static final String SMCS_ROOT = HOST + "/SMCS_APP";
    public static final String FCM_APP = SMCS_ROOT + "/fcm_app";
    public static final String SUBCONTRACTOR = SMCS_ROOT + "/subcontractor";

    public static final String MULTI_SAVE_TIME_IN = FCM_APP + "/multi-savetimein.php";
    public static final String GET_PROJECTS = FCM_APP + "/getprojects.php";
    public static final String VERSION_CHECK = FCM_APP + "/version_check.php";
    public static final String SUBADMIN_LOGIN = FCM_APP + "/subadminlogin.php";
    public static final String EMPLOYEE_DETAILS = SUBCONTRACTOR + "/employee_details.php";
    /** JSON APIs for native Labour Management & Move Site */
    public static final String API_GET_DEPARTMENTS = FCM_APP + "/api_get_departments.php";
    public static final String API_GET_MOVE_EMPLOYEES = FCM_APP + "/api_get_move_employees.php";
    public static final String API_LABOUR_MANAGER = FCM_APP + "/api_labour_manager.php";
    public static final String API_MANAGE_ATTENDANCE = FCM_APP + "/api_manage_attendance.php";
    public static final String VIEW_ATTENDANCE_IMG = FCM_APP + "/view_attendance_img.php";

    /** Same key as Postman header x-api-key (Move Site JSON APIs). */
    public static final String X_API_KEY = "4S_Secure_Access_Token_2024_#$";
    public static final String HEADER_X_API_KEY = "x-api-key";

    /**
     * Resolves authoritative attendance photo URL including shift_no to avoid LruCache collisions.
     */
    public static String attendancePhotoUrl(String photoUrl, String attendanceId, int shiftNo, String type) {
        int sNo = shiftNo > 0 ? shiftNo : 1;
        String cleanType = (type != null && !type.trim().isEmpty()) ? type.trim().toLowerCase(java.util.Locale.US) : "in";

        if (photoUrl != null && !photoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(photoUrl.trim()) && !"0".equals(photoUrl.trim())) {
            String url = photoUrl.trim();
            if (url.contains("view_attendance_img.php")) {
                if (!url.contains("shift_no=") && !url.contains("move_id=")) {
                    url = url + (url.contains("?") ? "&" : "?") + "shift_no=" + sNo;
                }
                return ImageLoaderHelper.sanitizeUrl(url);
            }
            return ImageLoaderHelper.sanitizeUrl(url);
        }

        if (attendanceId != null && !attendanceId.trim().isEmpty() && !"0".equals(attendanceId.trim()) && !"null".equalsIgnoreCase(attendanceId.trim()) && !"--".equals(attendanceId.trim())) {
            String generated = FCM_APP + "/view_attendance_img.php?attendance_id=" + attendanceId.trim() + "&shift_no=" + sNo + "&type=" + cleanType;
            return ImageLoaderHelper.sanitizeUrl(generated);
        }
        return "";
    }

    public static String attendancePhotoUrl(String photoUrl, String attendanceId, String shiftNoStr, String type) {
        int shiftNo = 1;
        if (shiftNoStr != null && !shiftNoStr.trim().isEmpty()) {
            try {
                shiftNo = Integer.parseInt(shiftNoStr.trim());
            } catch (Exception ignored) {}
        }
        return attendancePhotoUrl(photoUrl, attendanceId, shiftNo, type);
    }

    public static String movePhotoUrl(String photoUrl, String moveId, String type) {
        String cleanType = (type != null && !type.trim().isEmpty()) ? type.trim().toLowerCase(java.util.Locale.US) : "in";
        if (photoUrl != null && !photoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(photoUrl.trim()) && !"0".equals(photoUrl.trim())) {
            return ImageLoaderHelper.sanitizeUrl(photoUrl.trim());
        }
        if (moveId != null && !moveId.trim().isEmpty() && !"0".equals(moveId.trim()) && !"null".equalsIgnoreCase(moveId.trim())) {
            String generated = FCM_APP + "/view_attendance_img.php?source=hrms&move_id=" + moveId.trim() + "&type=" + cleanType;
            return ImageLoaderHelper.sanitizeUrl(generated);
        }
        return "";
    }
}
