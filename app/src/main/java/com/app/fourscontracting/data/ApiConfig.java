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
    public static final String API_GET_DEPARTMENTS = SUBCONTRACTOR + "/api_get_departments.php";
    public static final String API_GET_MOVE_EMPLOYEES = SUBCONTRACTOR + "/api_get_move_employees.php";
    public static final String API_LABOUR_MANAGER = SUBCONTRACTOR + "/api_labour_manager.php";
    public static final String API_MANAGE_ATTENDANCE = SUBCONTRACTOR + "/api_manage_attendance.php";

    /** Same key as Postman header x-api-key (Move Site JSON APIs). */
    public static final String X_API_KEY = "4S_Secure_Access_Token_2024_#$";
    public static final String HEADER_X_API_KEY = "x-api-key";
}
