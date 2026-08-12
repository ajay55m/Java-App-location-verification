package com.app.fourscontracting.data;

public class MoveEmployeeModel {
    public final String id;
    public final String firstName;
    public final String timeInDisplay;
    public final String timeOutDisplay;
    public final boolean onMove;
    public final String action; // MOVE or OUT
    public final String photoUrl;

    public MoveEmployeeModel(String id, String firstName, String timeInDisplay, String timeOutDisplay,
                             boolean onMove, String action, String photoUrl) {
        this.id = id != null ? id : "";
        this.firstName = firstName != null ? firstName : "";
        this.timeInDisplay = timeInDisplay != null ? timeInDisplay : "";
        this.timeOutDisplay = timeOutDisplay != null ? timeOutDisplay : "";
        this.onMove = onMove;
        this.action = action != null ? action : "MOVE";
        this.photoUrl = photoUrl != null ? photoUrl : "";
    }
}
