package com.app.fourscontracting;

import androidx.annotation.NonNull;

public class Project {
    public String id;
    public String name;
    public String breakHours;

    public Project(String id, String name, String breakHours) {
        this.id = id;
        this.name = name;
        this.breakHours = breakHours;
    }

    // This is what shows in the AutoCompleteTextView/Spinner dropdown
    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
