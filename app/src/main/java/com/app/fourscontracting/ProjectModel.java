package com.app.fourscontracting;

public class ProjectModel {
    public final String id;
    public final String projname;

    public ProjectModel(String id, String projname) {
        this.id = id;
        this.projname = projname;
    }

    @Override
    public String toString() {
        return projname != null ? projname : "";
    }
}
