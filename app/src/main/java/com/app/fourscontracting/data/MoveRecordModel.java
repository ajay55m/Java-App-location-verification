package com.app.fourscontracting.data;

public class MoveRecordModel {
    private final String moveId;
    private final String firstName;
    private final String projName;
    private final String inTime;
    private final String outTime;
    private final boolean hasIn;
    private final boolean hasOut;
    private final String inPhotoUrl;
    private final String outPhotoUrl;

    public MoveRecordModel(String moveId, String firstName, String projName, String inTime,
                           String outTime, boolean hasIn, boolean hasOut,
                           String inPhotoUrl, String outPhotoUrl) {
        this.moveId = moveId != null ? moveId : "";
        this.firstName = firstName != null ? firstName : "";
        this.projName = projName != null ? projName : "";
        this.inTime = inTime != null ? inTime : "";
        this.outTime = outTime != null ? outTime : "";
        this.hasIn = hasIn;
        this.hasOut = hasOut;
        this.inPhotoUrl = inPhotoUrl != null ? inPhotoUrl : "";
        this.outPhotoUrl = outPhotoUrl != null ? outPhotoUrl : "";
    }

    public String getMoveId() {
        return moveId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getProjName() {
        return projName;
    }

    public String getInTime() {
        return inTime;
    }

    public String getOutTime() {
        return outTime;
    }

    public boolean isHasIn() {
        return hasIn;
    }

    public boolean isHasOut() {
        return hasOut;
    }

    public String getInPhotoUrl() {
        if ((inPhotoUrl == null || inPhotoUrl.trim().isEmpty()) && hasIn && !moveId.isEmpty()) {
            return "view_move_img.php?move_id=" + moveId + "&type=in";
        }
        return inPhotoUrl;
    }

    public String getOutPhotoUrl() {
        if ((outPhotoUrl == null || outPhotoUrl.trim().isEmpty()) && hasOut && !moveId.isEmpty()) {
            return "view_move_img.php?move_id=" + moveId + "&type=out";
        }
        return outPhotoUrl;
    }

    public boolean isActive() {
        return outTime.isEmpty() || "--".equals(outTime);
    }
}
