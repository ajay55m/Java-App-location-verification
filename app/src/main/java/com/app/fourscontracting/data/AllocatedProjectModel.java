package com.app.fourscontracting.data;

public class AllocatedProjectModel {
    private final String id;
    private final String projname;

    public AllocatedProjectModel(String id, String projname) {
        this.id = id != null ? id : "";
        this.projname = projname != null ? projname : "";
    }

    public String getId() {
        return id;
    }

    public String getProjname() {
        return projname;
    }

    @Override
    public String toString() {
        return projname;
    }
}
