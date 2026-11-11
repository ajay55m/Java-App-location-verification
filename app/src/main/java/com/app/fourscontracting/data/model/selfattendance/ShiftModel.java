package com.app.fourscontracting.data.model.selfattendance;

/**
 * Model representing a shift/move interval for supervisor or worker.
 */
public class ShiftModel {
    public int shiftId;
    public int shiftNo;
    public int moveId;
    public String inTime;
    public String outTime;
    public String siteName;
    public boolean isActive;

    public ShiftModel() {}

    public ShiftModel(int shiftNo, String inTime, String outTime, String siteName, boolean isActive) {
        this.shiftNo = shiftNo;
        this.inTime = inTime;
        this.outTime = outTime;
        this.siteName = siteName;
        this.isActive = isActive;
    }
}

