package com.app.fourscontracting;

public class PunchResponse {
    public String status; // success / error
    public String action; // IN / OUT
    public String message;
    public String mobile_session_id;
    public int attendance_id, shift_id, shift_no, photo_id, total_minutes, department_id, distance;
}
