package com.app.fourscontracting;

/**
 * Clear English field messages (GPS, session, time lock).
 */
public final class AppMessages {
    private AppMessages() {}

    public static final String GPS_DISABLED =
            "GPS is turned off. Enable Location in Settings, then try again.";
    public static final String GPS_WEAK =
            "Weak GPS signal. Step outdoors or near a window and wait a few seconds.";
    public static final String GPS_STALE =
            "GPS is outdated. Wait for a fresh location lock, then retry.";
    public static final String GPS_MOCK =
            "Fake GPS detected. Turn off mock location apps, then retry.";
    public static final String GPS_TIMEOUT =
            "Could not get GPS. Move outdoors and retry.";
    public static final String GPS_REQUIRED_SAVE =
            "GPS is required to save attendance. Move outdoors and retry.";
    public static final String GPS_SERVICES_UNAVAILABLE =
            "Location services are unavailable on this phone.";

    public static final String LOCATION_NOT_VERIFIED =
            "Location not verified. Verify your site first, then try again.";
    public static final String SESSION_EXPIRED =
            "Verification expired. Verify location again, then submit attendance.";
    public static final String SUPERVISOR_LOCATION_REQUIRED =
            "Supervisor location verification is required. Verify your site first.";
    public static final String PROJECT_SESSION_MISSING =
            "Project is missing. Select a project, then verify location again.";
    public static final String NO_SITE_LOCATIONS =
            "No GPS sites are set for this project. Contact admin.";

    public static final String AUTO_TIME_REQUIRED =
            "Turn on automatic date & time (network time) in phone Settings, then retry.";
    public static final String USER_SESSION_MISSING =
            "Login session is missing. Please sign in again.";
    public static final String NO_INTERNET =
            "No internet connection. Check mobile data or Wi‑Fi.";

    public static String gpsWeakWithAccuracy(float meters) {
        return "Weak GPS (" + Math.round(meters) + " m). Step outdoors and retry.";
    }
}
