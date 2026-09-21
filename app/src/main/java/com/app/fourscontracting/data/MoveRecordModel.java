package com.app.fourscontracting.data;

public class MoveRecordModel {
    private final String moveId;
    private final String empId;
    private final String firstName;
    private final String projName;
    private final String inTime;
    private final String outTime;
    private final boolean hasIn;
    private final boolean hasOut;
    private final String inPhotoUrl;
    private final String outPhotoUrl;

    public MoveRecordModel(String moveId, String firstName, String projName, String inTime,
                           String outTime, boolean hasIn, boolean hasOut,
                           String inPhotoUrl, String outPhotoUrl) {
        this(moveId, "", firstName, projName, inTime, outTime, hasIn, hasOut, inPhotoUrl, outPhotoUrl);
    }

    public MoveRecordModel(String moveId, String empId, String firstName, String projName, String inTime,
                           String outTime, boolean hasIn, boolean hasOut,
                           String inPhotoUrl, String outPhotoUrl) {
        this.moveId = moveId != null ? moveId : "";
        this.empId = empId != null ? empId : "";
        this.firstName = firstName != null ? firstName : "";
        this.projName = projName != null ? projName : "";
        this.inTime = inTime != null ? inTime : "";
        this.outTime = outTime != null ? outTime : "";
        this.hasIn = hasIn;
        this.hasOut = hasOut;
        this.inPhotoUrl = inPhotoUrl != null ? inPhotoUrl : "";
        this.outPhotoUrl = outPhotoUrl != null ? outPhotoUrl : "";
    }

    public String getMoveId() {
        return moveId;
    }

    public String getEmpId() {
        return empId != null ? empId.trim() : "";
    }

    public String getFirstName() {
        return firstName;
    }

    public String getProjName() {
        return projName;
    }

    public String getInTime() {
        return inTime;
    }

    public String getOutTime() {
        return outTime;
    }

    public boolean isHasIn() {
        return hasIn || (inTime != null && !inTime.trim().isEmpty() && !"--".equals(inTime.trim()) && !"--:--".equals(inTime.trim()));
    }

    public boolean isHasOut() {
        return hasOut || (outTime != null && !outTime.trim().isEmpty() && !"--".equals(outTime.trim()) && !"--:--".equals(outTime.trim()));
    }

    public String getInPhotoUrl() {
        if (inPhotoUrl != null && !inPhotoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(inPhotoUrl.trim()) && !"0".equals(inPhotoUrl.trim())) {
            String val = inPhotoUrl.trim();
            if (isNumeric(val) && moveId != null && !moveId.trim().isEmpty()) {
                return "view_attendance_img.php?image_id=" + val + "&move_id=" + moveId.trim() + "&type=in&source=hrms";
            }
            return val;
        }
        if (isHasIn() && moveId != null && !moveId.trim().isEmpty() && !"null".equalsIgnoreCase(moveId.trim()) && !"0".equals(moveId.trim())) {
            return "view_attendance_img.php?move_id=" + moveId.trim() + "&type=in&source=hrms";
        }
        return "";
    }

    public String getOutPhotoUrl() {
        if (outPhotoUrl != null && !outPhotoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(outPhotoUrl.trim()) && !"0".equals(outPhotoUrl.trim())) {
            String val = outPhotoUrl.trim();
            if (isNumeric(val) && moveId != null && !moveId.trim().isEmpty()) {
                return "view_attendance_img.php?image_id=" + val + "&move_id=" + moveId.trim() + "&type=out&source=hrms";
            }
            return val;
        }
        if (isHasOut() && moveId != null && !moveId.trim().isEmpty() && !"null".equalsIgnoreCase(moveId.trim()) && !"0".equals(moveId.trim())) {
            return "view_attendance_img.php?move_id=" + moveId.trim() + "&type=out&source=hrms";
        }
        return "";
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        for (char c : str.trim().toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    public boolean isActive() {
        if (outTime == null || outTime.trim().isEmpty() || "--".equals(outTime.trim())) {
            return true;
        }
        String t = outTime.trim();
        return "00:00:00".equals(t) || "00.00.00".equals(t) || "00:00".equals(t) || "null".equalsIgnoreCase(t);
    }
}
