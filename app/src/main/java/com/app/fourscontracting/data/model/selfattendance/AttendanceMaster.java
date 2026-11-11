package com.app.fourscontracting.data.model.selfattendance;

/**
 * Attendance master record representing an attendance session/punch for an employee.
 */
public class AttendanceMaster {
    public int attendId;
    public int empId;
    public int uid;
    public String workerName;
    public String siteName;
    public String timeIn;
    public String timeOut;
    public double breakHours;
    public boolean hasIn;
    public boolean hasOut;
    public String inPhotoUrl;
    public String outPhotoUrl;

    public AttendanceMaster() {}

    public AttendanceMaster(int attendId, int empId, String workerName, String siteName, String timeIn, String timeOut) {
        this.attendId = attendId;
        this.empId = empId;
        this.workerName = workerName;
        this.siteName = siteName;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }
}

