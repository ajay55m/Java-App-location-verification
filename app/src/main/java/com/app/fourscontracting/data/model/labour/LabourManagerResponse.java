package com.app.fourscontracting.data.model.labour;

import java.util.List;

public class LabourManagerResponse {
    public Stats stats;
    public List<LabourEmployee> employees;

    public static class Stats {
        public int total_staff;
        public int present_here;
        public int absent;
    }

    public static class LabourEmployee {
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

