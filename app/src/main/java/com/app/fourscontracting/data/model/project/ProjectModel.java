package com.app.fourscontracting.data.model.project;

public class ProjectModel {
    public int id;
    public String projname;
    public String project_code;
    public String project_description;
    public String client_name;
    public int break_hours;

    public ProjectModel() {}

    public ProjectModel(String idOrCode, String projname) {
        try { this.id = Integer.parseInt(idOrCode); } catch (Exception ignored) {}
        this.project_code = idOrCode;
        this.projname = (projname != null && !projname.isEmpty()) ? projname : idOrCode;
    }

    public String getProjectCode() {
        if (project_code != null && !project_code.trim().isEmpty()) {
            return project_code.trim();
        }
        return projname != null ? projname.trim() : "";
    }

    @Override
    public String toString() {
        String code = getProjectCode();
        if (project_description != null && !project_description.isEmpty() && !project_description.equalsIgnoreCase(code)) {
            return code + " - " + project_description;
        }
        return code != null ? code : "";
    }
}
