package com.app.fourscontracting;

import java.util.List;

public class ManagerResponse {
    public String status, date;
    public int count;
    public List<AttendanceData> data;
    public List<LabourEmployeeItem> employees;

    public static class AttendanceData {
        public int attendance_id, employee_id, total_minutes;
        public List<ShiftData> shifts;
    }

    public static class ShiftData {
        public int shift_no, shift_id;
        public String in_at, out_at, next_action, project_code;
    }

    public static class LabourEmployeeItem {
        public String id;
        public String name;
        public String status_code;
        public String display_text;
        public boolean can_in;
        public boolean can_out;
        public String site_name;
        public String photo;
    }
}

