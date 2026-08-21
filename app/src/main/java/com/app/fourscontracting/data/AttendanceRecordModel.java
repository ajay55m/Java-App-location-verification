package com.app.fourscontracting.data;

public class AttendanceRecordModel {
    private final String attendId;
    private final String firstName;
    private final String projName;
    private final String timeIn;
    private final String timeOut;
    private double breakHours;
    private final boolean hasIn;
    private final boolean hasOut;
    private final String inPhotoUrl;
    private final String outPhotoUrl;

    public AttendanceRecordModel(String attendId, String firstName, String projName, String timeIn,
                                 String timeOut, double breakHours, boolean hasIn, boolean hasOut,
                                 String inPhotoUrl, String outPhotoUrl) {
        this.attendId = attendId != null ? attendId : "";
        this.firstName = firstName != null ? firstName : "";
        this.projName = projName != null ? projName : "";
        this.timeIn = timeIn != null ? timeIn : "";
        this.timeOut = timeOut != null ? timeOut : "";
        this.breakHours = breakHours;
        this.hasIn = hasIn;
        this.hasOut = hasOut;
        this.inPhotoUrl = inPhotoUrl != null ? inPhotoUrl : "";
        this.outPhotoUrl = outPhotoUrl != null ? outPhotoUrl : "";
    }

    public String getAttendId() {
        return attendId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getProjName() {
        return projName;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public double getBreakHours() {
        return breakHours;
    }

    public void setBreakHours(double breakHours) {
        this.breakHours = breakHours;
    }

    public boolean isHasIn() {
        return hasIn;
    }

    public boolean isHasOut() {
        return hasOut;
    }

    public String getInPhotoUrl() {
        if ((inPhotoUrl == null || inPhotoUrl.trim().isEmpty()) && hasIn && !attendId.isEmpty()) {
            return "view_attendance_img.php?attendance_id=" + attendId + "&type=in";
        }
        return inPhotoUrl;
    }

    public String getOutPhotoUrl() {
        if ((outPhotoUrl == null || outPhotoUrl.trim().isEmpty()) && hasOut && !attendId.isEmpty()) {
            return "view_attendance_img.php?attendance_id=" + attendId + "&type=out";
        }
        return outPhotoUrl;
    }

    public boolean isActive() {
        return timeOut.isEmpty() || "--".equals(timeOut);
    }
}
