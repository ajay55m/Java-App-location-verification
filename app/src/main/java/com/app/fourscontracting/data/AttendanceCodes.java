package com.app.fourscontracting.data;

/**
 * Canonical attendance result codes (JSON "code" field from PHP).
 * Legacy plain "success"/"error" bodies are mapped in AttendanceResult.parse().
 */
public final class AttendanceCodes {
    private AttendanceCodes() {}

    public static final String OK = "OK";
    public static final String QUEUED_OFFLINE = "QUEUED_OFFLINE";
    public static final String ERROR = "ERROR";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String OUTSIDE_GEOFENCE = "OUTSIDE_GEOFENCE";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String INVALID_PARAMS = "INVALID_PARAMS";
    public static final String GPS_REQUIRED = "GPS_REQUIRED";
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
}
