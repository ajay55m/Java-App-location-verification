package com.app.fourscontracting.data;

import com.app.fourscontracting.AppMessages;

import org.json.JSONObject;

public class AttendanceResult {
    public final boolean success;
    public final String code;
    public final String message;
    public final boolean queuedOffline;

    public AttendanceResult(boolean success, String code, String message, boolean queuedOffline) {
        this.success = success;
        this.code = code != null ? code : AttendanceCodes.ERROR;
        this.message = message != null ? message : "";
        this.queuedOffline = queuedOffline;
    }

    public static AttendanceResult ok(String message) {
        return new AttendanceResult(true, AttendanceCodes.OK, message, false);
    }

    public static AttendanceResult queued() {
        return new AttendanceResult(true, AttendanceCodes.QUEUED_OFFLINE,
                "No network. Attendance saved offline and will sync automatically.", true);
    }

    public static AttendanceResult fail(String code, String message) {
        return new AttendanceResult(false, code, message, false);
    }

    /**
     * Accepts modern JSON or legacy plain-text bodies from multi-savetimein.php.
     */
    public static AttendanceResult parse(String raw) {
        String trimmed = raw != null ? raw.trim() : "";
        if (trimmed.isEmpty()) {
            return fail(AttendanceCodes.ERROR, "Empty server response");
        }

        if (trimmed.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(trimmed);
                String status = obj.optString("status", "");
                String code = obj.optString("code", "");
                String message = obj.optString("message", obj.optString("msg", ""));

                boolean ok = "success".equalsIgnoreCase(status)
                        || "ok".equalsIgnoreCase(status)
                        || AttendanceCodes.OK.equalsIgnoreCase(code);

                if (code.isEmpty()) {
                    code = ok ? AttendanceCodes.OK : AttendanceCodes.ERROR;
                }
                if (message.isEmpty()) {
                    message = ok ? "Success" : "Error";
                }
                return new AttendanceResult(ok, code, message, false);
            } catch (Exception e) {
                // fall through to plain text
            }
        }

        if (trimmed.equalsIgnoreCase("success")) {
            return ok("Success");
        }
        if (trimmed.equalsIgnoreCase("error")) {
            return fail(AttendanceCodes.ERROR, "Error");
        }
        if (trimmed.toLowerCase().contains("success")) {
            return ok(trimmed);
        }
        return fail(AttendanceCodes.ERROR, trimmed);
    }

    public String userMessage() {
        if (message != null && !message.isEmpty()) {
            return message;
        }
        switch (code) {
            case AttendanceCodes.DUPLICATE:
                return "Attendance already recorded for this action.";
            case AttendanceCodes.OUTSIDE_GEOFENCE:
                return "Outside site boundary. Move closer and retry.";
            case AttendanceCodes.SESSION_EXPIRED:
                return AppMessages.SESSION_EXPIRED;
            case AttendanceCodes.GPS_REQUIRED:
                return AppMessages.GPS_REQUIRED_SAVE;
            case AttendanceCodes.INVALID_PARAMS:
                return "Missing attendance details. Retry from Labour screen.";
            case AttendanceCodes.QUEUED_OFFLINE:
                return "Saved offline. Will sync when online.";
            case AttendanceCodes.NETWORK_ERROR:
                return AppMessages.NO_INTERNET;
            case AttendanceCodes.OK:
                return "Success";
            default:
                return "Unable to save attendance.";
        }
    }
}
