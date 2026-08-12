package com.app.fourscontracting.data;

public class DepartmentModel {
    public final String id;
    public final String name;

    public DepartmentModel(String id, String name) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
    }

    @Override
    public String toString() {
        return name;
    }
}
