package com.app.fourscontracting.data.model.project;

public class LocationModel {
    public int id;
    public String projectid;
    public String project_code;
    public String project_description;
    public String location_name;
    public double latitude;
    public double longitude;
    public int break_hours;
    public int client_name;

    public LocationModel() {}

    public String getLocationName() {
        return location_name != null ? location_name : "";
    }

    @Override
    public String toString() {
        return getLocationName();
    }
}

