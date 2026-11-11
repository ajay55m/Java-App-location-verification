package com.app.fourscontracting.data.model.selfattendance;

import com.app.fourscontracting.ManagerResponse;
import com.app.fourscontracting.data.model.project.ProjectModel;

import java.util.List;

public class ManageAttendanceResponse {
    public String status;
    public String date;
    public int uid;
    public int empid;
    public int dept_id;
    public int location_id;
    public String next_action;

    public OwnAttendance own_attendance;
    public List<OwnMoveRecord> own_move_records;
    public List<WorkerRecord> records;
    public List<WorkerMoveRecord> move_records;
    public List<ProjectModel> projects;
    public int count;
    public List<ManagerResponse.AttendanceData> data;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public static class OwnAttendance {
        public int attend_id;
        public int empid;
        public String timein;
        public String timeout;
        public int total_minutes;
        public int break_hours;
        public String attendance_code;
        public int is_from_app;
        public String approval_status;
        public String projname;
        public String location_name;
        public boolean has_in;
        public boolean has_out;
        public String in_time;
        public String out_time;
        public String in_photo_url;
        public String out_photo_url;
    }

    public static class OwnMoveRecord {
        public int move_id;
        public int shift_id;
        public int shift_no;
        public String in_time;
        public String out_time;
        public String in_at;
        public String out_at;
        public String projname;
        public String location_name;
        public boolean has_in;
        public boolean has_out;
        public String next_action;
    }

    public static class WorkerRecord {
        public int attend_id;
        public int empid;
        public String employee_number;
        public String first_name;
        public String attendance_date;
        public String in_time;
        public String out_time;
        public String in_at;
        public String out_at;
        public int break_hours;
        public int is_from_app;
        public String projname;
        public int projectid;
        public boolean has_in;
        public boolean has_out;
        public String status_code;
        public String in_photo_url;
        public String out_photo_url;
    }

    public static class WorkerMoveRecord {
        public int move_id;
        public int shift_id;
        public int shift_no;
        public int employee_id;
        public String employee_number;
        public String first_name;
        public String in_time;
        public String out_time;
        public String projname;
        public String location_name;
        public boolean has_in;
        public boolean has_out;
        public String next_action;
        public String in_photo_url;
        public String out_photo_url;
    }
}

