package com.app.fourscontracting.data.model.project;

import com.google.gson.annotations.SerializedName;

public class LocationModel {
    public int id;
    public String projectid;
    public String project_code;
    public String project_description;
    public String location_name;
    public double latitude;
    public double longitude;

    @SerializedName(value = "max_latitude", alternate = {"max_lat"})
    public double max_latitude;

    @SerializedName(value = "max_longtitude", alternate = {"max_longitude", "max_lng"})
    public double max_longtitude;

    public int break_hours;
    public int client_name;

    public LocationModel() {}

    public String getLocationName() {
        return location_name != null ? location_name : "";
    }

    public boolean hasBoundingBox() {
        return Math.abs(max_latitude) > 0.0001 && Math.abs(max_longtitude) > 0.0001;
    }

    public double getMinLatitude() {
        return Math.min(latitude, max_latitude);
    }

    public double getMaxLatitude() {
        return Math.max(latitude, max_latitude);
    }

    public double getMinLongitude() {
        return Math.min(longitude, max_longtitude);
    }

    public double getMaxLongitude() {
        return Math.max(longitude, max_longtitude);
    }

    public double getCenterLatitude() {
        return hasBoundingBox() ? (latitude + max_latitude) / 2.0 : latitude;
    }

    public double getCenterLongitude() {
        return hasBoundingBox() ? (longitude + max_longtitude) / 2.0 : longitude;
    }

    @Override
    public String toString() {
        return getLocationName();
    }
}

