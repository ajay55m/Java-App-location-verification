package com.app.fourscontracting.data.model.punch;

public class PunchResponse {
    public String status;
    public String message;
    public String action;
    public String next_action;
    public String project_code;
    public String date;
    public String time;
    public String mobile_session_id;

    public int attendance_id;
    public int shift_id;
    public int shift_no;
    public int total_minutes;
    public int location_id;
    public int photo_id;
    public int department_id;
    public Integer distance;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}

