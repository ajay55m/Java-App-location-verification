package com.app.fourscontracting.data.model.project;

import com.google.gson.annotations.SerializedName;

public class ProjectModel {
    public int id;
    public String projname;
    public String project_code;
    public String project_description;

    @SerializedName(value = "location_name", alternate = {"locationname", "loc_name"})
    public String location_name;

    public String client_name;
    public int break_hours;

    public ProjectModel() {}

    public ProjectModel(String idOrCode, String projname) {
        try { this.id = Integer.parseInt(idOrCode); } catch (Exception ignored) {}
        this.project_code = idOrCode;
        this.projname = (projname != null && !projname.isEmpty()) ? projname : idOrCode;
    }

    public ProjectModel(String idOrCode, String projname, String location_name) {
        this(idOrCode, projname);
        this.location_name = location_name;
    }

    public String getProjectCode() {
        if (project_code != null && !project_code.trim().isEmpty()) {
            return project_code.trim();
        }
        return projname != null ? projname.trim() : "";
    }

    public String getLocationName() {
        if (location_name != null && !location_name.trim().isEmpty()) {
            return location_name.trim();
        }
        return "";
    }

    @Override
    public String toString() {
        String code = getProjectCode();
        String loc = getLocationName();
        String desc = project_description != null ? project_description.trim() : "";

        // If location_name is present, include it in the dropdown name
        if (!loc.isEmpty()) {
            if (!code.isEmpty() && !loc.equalsIgnoreCase(code)) {
                if (!desc.isEmpty() && !desc.equalsIgnoreCase(code) && !desc.equalsIgnoreCase(loc)) {
                    return code + " - " + loc + " (" + desc + ")";
                }
                return code + " - " + loc;
            } else if (!desc.isEmpty() && !desc.equalsIgnoreCase(loc)) {
                return loc + " - " + desc;
            }
            return loc;
        }

        if (!desc.isEmpty() && !desc.equalsIgnoreCase(code)) {
            return code + " - " + desc;
        }
        return code != null ? code : "";
    }
}

