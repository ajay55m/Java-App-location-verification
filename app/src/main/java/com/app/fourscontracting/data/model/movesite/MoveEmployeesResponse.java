package com.app.fourscontracting.data.model.movesite;

import java.util.List;

public class MoveEmployeesResponse {
    public String status;
    public String date;
    public String location_id;
    public String location_name;
    public int ready_count;
    public int moving_count;
    public List<MoveEmployeeItem> employees;

    public static class MoveEmployeeItem {
        public String id;
        public String first_name;
        public String time_in;
        public String time_out;
        public boolean on_move;
        public String action;
        public String photo_url;
        public String move_id;
        public String attend_id;
    }
}
