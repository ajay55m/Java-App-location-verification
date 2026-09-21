package com.app.fourscontracting.data;

public class AttendanceRecordModel {
    private final String attendId;
    private final String userId;
    private final String empId;
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
        this(attendId, "", "", firstName, projName, timeIn, timeOut, breakHours, hasIn, hasOut, inPhotoUrl, outPhotoUrl);
    }

    public AttendanceRecordModel(String attendId, String empId, String firstName, String projName, String timeIn,
                                 String timeOut, double breakHours, boolean hasIn, boolean hasOut,
                                 String inPhotoUrl, String outPhotoUrl) {
        this(attendId, "", empId, firstName, projName, timeIn, timeOut, breakHours, hasIn, hasOut, inPhotoUrl, outPhotoUrl);
    }

    public AttendanceRecordModel(String attendId, String userId, String empId, String firstName, String projName, String timeIn,
                                 String timeOut, double breakHours, boolean hasIn, boolean hasOut,
                                 String inPhotoUrl, String outPhotoUrl) {
        this.attendId = attendId != null ? attendId : "";
        this.userId = userId != null ? userId : "";
        this.empId = empId != null ? empId : "";
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

    public String getUserId() {
        if (userId != null && !userId.trim().isEmpty() && !"null".equalsIgnoreCase(userId.trim()) && !"--".equals(userId.trim())) {
            return userId.trim();
        }
        return "";
    }

    public String getEmpId() {
        if (empId != null && !empId.trim().isEmpty() && !"null".equalsIgnoreCase(empId.trim()) && !"--".equals(empId.trim())) {
            return empId.trim();
        }
        return "";
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
        return hasIn || (timeIn != null && !timeIn.trim().isEmpty() && !"--".equals(timeIn.trim()) && !"--:--".equals(timeIn.trim()));
    }

    public boolean isHasOut() {
        return hasOut || (timeOut != null && !timeOut.trim().isEmpty() && !"--".equals(timeOut.trim()) && !"--:--".equals(timeOut.trim()));
    }

    public String getInPhotoUrl() {
        if (inPhotoUrl != null && !inPhotoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(inPhotoUrl.trim()) && !"0".equals(inPhotoUrl.trim())) {
            return inPhotoUrl.trim();
        }
        if (isHasIn() && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
            return "view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=in&source=hrms";
        }
        return "";
    }

    public String getOutPhotoUrl() {
        if (outPhotoUrl != null && !outPhotoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(outPhotoUrl.trim()) && !"0".equals(outPhotoUrl.trim())) {
            return outPhotoUrl.trim();
        }
        if (isHasOut() && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
            return "view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=out&source=hrms";
        }
        return "";
    }

    public boolean isActive() {
        if (timeOut == null || timeOut.trim().isEmpty() || "--".equals(timeOut.trim())) {
            return true;
        }
        String t = timeOut.trim();
        return "00:00:00".equals(t) || "00.00.00".equals(t) || "00:00".equals(t) || "null".equalsIgnoreCase(t);
    }
}
