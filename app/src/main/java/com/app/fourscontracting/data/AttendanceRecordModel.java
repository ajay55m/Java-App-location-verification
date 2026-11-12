package com.app.fourscontracting.data;

import java.util.Calendar;

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
    private int shiftNo = 1;
    private String date = "";

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
        this(attendId, userId, empId, firstName, projName, timeIn, timeOut, breakHours, hasIn, hasOut, inPhotoUrl, outPhotoUrl, 1);
    }

    public AttendanceRecordModel(String attendId, String userId, String empId, String firstName, String projName, String timeIn,
                                 String timeOut, double breakHours, boolean hasIn, boolean hasOut,
                                 String inPhotoUrl, String outPhotoUrl, int shiftNo) {
        this(attendId, userId, empId, firstName, projName, timeIn, timeOut, breakHours, hasIn, hasOut, inPhotoUrl, outPhotoUrl, shiftNo, "");
    }

    public AttendanceRecordModel(String attendId, String userId, String empId, String firstName, String projName, String timeIn,
                                 String timeOut, double breakHours, boolean hasIn, boolean hasOut,
                                 String inPhotoUrl, String outPhotoUrl, int shiftNo, String date) {
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
        this.shiftNo = shiftNo > 0 ? shiftNo : 1;
        this.date = date != null ? date.trim() : "";
    }

    public int getShiftNo() {
        return shiftNo > 0 ? shiftNo : 1;
    }

    public void setShiftNo(int shiftNo) {
        this.shiftNo = shiftNo > 0 ? shiftNo : 1;
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
            String val = inPhotoUrl.trim();
            if (isNumeric(val) && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
                return ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=in";
            }
            if (val.contains("subcontractor/view_attendance_img.php")) {
                val = val.replace("subcontractor/view_attendance_img.php", "fcm_app/view_attendance_img.php");
            }
            return val;
        }
        if (isHasIn() && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
            return ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=in";
        }
        if (isHasIn() && empId != null && !empId.trim().isEmpty() && !"null".equalsIgnoreCase(empId.trim()) && !"0".equals(empId.trim())) {
            return ApiConfig.SUBCONTRACTOR + "/get_photo.php?id=" + empId.trim();
        }
        return "";
    }

    public String getOutPhotoUrl() {
        if (outPhotoUrl != null && !outPhotoUrl.trim().isEmpty() && !"null".equalsIgnoreCase(outPhotoUrl.trim()) && !"0".equals(outPhotoUrl.trim())) {
            String val = outPhotoUrl.trim();
            if (isNumeric(val) && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
                return ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=out";
            }
            if (val.contains("subcontractor/view_attendance_img.php")) {
                val = val.replace("subcontractor/view_attendance_img.php", "fcm_app/view_attendance_img.php");
            }
            return val;
        }
        if (isHasOut() && attendId != null && !attendId.trim().isEmpty() && !"null".equalsIgnoreCase(attendId.trim()) && !"0".equals(attendId.trim())) {
            return ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=" + attendId.trim() + "&type=out";
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
        if (timeOut == null || timeOut.trim().isEmpty() || "--".equals(timeOut.trim())) {
            return true;
        }
        String t = timeOut.trim();
        return "00:00:00".equals(t) || "00.00.00".equals(t) || "00:00".equals(t) || "null".equalsIgnoreCase(t);
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public void setDate(String date) {
        this.date = date != null ? date.trim() : "";
    }

    public boolean isPastDate() {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        return isDateBeforeToday(date);
    }

    public boolean canCheckOut() {
        if (!isActive()) return false;
        if (isPastDate()) return false;
        return true;
    }

    public static boolean isDateBeforeToday(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        String clean = dateStr.trim();
        int y = 0, m = 0, d = 0;
        try {
            if (clean.contains("-")) {
                String[] parts = clean.split("-");
                if (parts.length >= 3) {
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        y = Integer.parseInt(parts[0]);
                        m = Integer.parseInt(parts[1]);
                        d = Integer.parseInt(parts[2].split(" ")[0].split("T")[0]);
                    } else if (parts[2].length() >= 4) { // dd-MM-yyyy
                        d = Integer.parseInt(parts[0]);
                        m = Integer.parseInt(parts[1]);
                        y = Integer.parseInt(parts[2].split(" ")[0].split("T")[0]);
                    }
                }
            } else if (clean.contains("/")) {
                String[] parts = clean.split("/");
                if (parts.length >= 3) {
                    if (parts[0].length() == 4) { // yyyy/MM/dd
                        y = Integer.parseInt(parts[0]);
                        m = Integer.parseInt(parts[1]);
                        d = Integer.parseInt(parts[2].split(" ")[0]);
                    } else if (parts[2].length() >= 4) { // dd/MM/yyyy
                        d = Integer.parseInt(parts[0]);
                        m = Integer.parseInt(parts[1]);
                        y = Integer.parseInt(parts[2].split(" ")[0]);
                    }
                }
            }
        } catch (Exception ignored) {
            return false;
        }

        if (y <= 0 || m <= 0 || d <= 0) return false;

        Calendar today = Calendar.getInstance();
        int curY = today.get(Calendar.YEAR);
        int curM = today.get(Calendar.MONTH) + 1;
        int curD = today.get(Calendar.DAY_OF_MONTH);

        if (y < curY) return true;
        if (y == curY && m < curM) return true;
        if (y == curY && m == curM && d < curD) return true;

        return false;
    }

    public static boolean isDateBeforeToday(String day, String month, String year) {
        if (day == null || month == null || year == null) return false;
        try {
            int d = Integer.parseInt(day.trim());
            int m = Integer.parseInt(month.trim());
            int y = Integer.parseInt(year.trim());
            Calendar today = Calendar.getInstance();
            int curY = today.get(Calendar.YEAR);
            int curM = today.get(Calendar.MONTH) + 1;
            int curD = today.get(Calendar.DAY_OF_MONTH);

            if (y < curY) return true;
            if (y == curY && m < curM) return true;
            if (y == curY && m == curM && d < curD) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
