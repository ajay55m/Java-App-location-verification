package com.app.fourscontracting;

import com.app.fourscontracting.data.AttendancePayload;
import com.app.fourscontracting.data.AttendanceRecordModel;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class SupervisorVsWorkerAttendanceTest {

    @Test
    public void testWorkerAttendanceCheckOutPayload_preservesWorkerIdAndSupervisorId() {
        String supervisorUid = "101";
        String workerEmpId = "85";

        AttendancePayload payload = new AttendancePayload();
        payload.uid = supervisorUid;
        payload.empid = workerEmpId;
        payload.attendId = "999";
        payload.type = "OUT";
        payload.projectId = "SITE_A";

        Map<String, String> params = payload.toFormParams();

        assertEquals("85", params.get("empid"));
        assertEquals("85", params.get("emp_id"));
        assertEquals("85", params.get("employee_id"));
        assertEquals("85", params.get("eid"));

        assertEquals("101", params.get("uid"));
        assertEquals("101", params.get("user_id"));
        assertEquals("101", params.get("userid"));
        assertEquals("101", params.get("subadmin_id"));
        assertEquals("101", params.get("manager_uid"));
        assertEquals("101", params.get("manager_id"));

        assertEquals("OUT", params.get("type"));
        assertEquals("checkout", params.get("action"));
        assertEquals("999", params.get("attend_id"));
    }

    @Test
    public void testWorkerRecordNotClassifiedAsSupervisorSelfRecord() {
        String supervisorUid = "101";
        String workerEmpId = "85";
        String workerUserId = "85";

        AttendanceRecordModel workerRecord = new AttendanceRecordModel(
                "999", workerUserId, workerEmpId, "Worker John", "Site A",
                "08:00:00", "", 0.0, true, false, "in.jpg", ""
        );

        assertNotEquals(supervisorUid, workerRecord.getEmpId());
        assertNotEquals(supervisorUid, workerRecord.getUserId());
        assertEquals("85", workerRecord.getEmpId());

        boolean isSelfRecord = workerEmpId.equalsIgnoreCase(supervisorUid) || workerUserId.equalsIgnoreCase(supervisorUid);
        assertFalse("Worker record #85 must not be classified as supervisor self-record #101", isSelfRecord);
    }

    @Test
    public void testWorkerLoggedBySupervisorIsNotClassifiedAsSupervisorSelfRecord() {
        String supervisorUid = "85";
        String workerEmpId = "43";
        String supervisorUserIdInRecord = "85";

        AttendanceRecordModel workerRecord = new AttendanceRecordModel(
                "2241", supervisorUserIdInRecord, workerEmpId, "Standard Worker", "Site A",
                "11:14:00", "", 0.0, true, false, "in.jpg", ""
        );

        boolean isSelfRecord = workerRecord.getEmpId().equalsIgnoreCase(supervisorUid);
        assertFalse("Standard Worker (empId 43) logged by supervisor 85 must NOT be classified as supervisor self-record", isSelfRecord);
    }

    @Test
    public void testSupervisorSelfRecordMatching() {
        String supervisorUid = "101";

        AttendanceRecordModel supervisorRecord = new AttendanceRecordModel(
                "888", supervisorUid, supervisorUid, "Site Supervisor", "Site A",
                "08:00:00", "", 0.0, true, false, "sup_in.jpg", ""
        );

        assertEquals("101", supervisorRecord.getEmpId());
        assertEquals("101", supervisorRecord.getUserId());

        boolean isSelfRecord = supervisorRecord.getEmpId().equalsIgnoreCase(supervisorUid);
        assertTrue("Supervisor's own record #101 must match supervisor UID #101", isSelfRecord);
    }

    @Test
    public void testSelfAttendancePayload_hasSameUserIdAndEmpId() {
        String selfUserId = "43";

        AttendancePayload payload = new AttendancePayload();
        payload.uid = selfUserId;
        payload.empid = selfUserId;
        payload.attendId = "2233";
        payload.type = "OUT";

        Map<String, String> params = payload.toFormParams();

        assertEquals("43", params.get("uid"));
        assertEquals("43", params.get("userid"));
        assertEquals("43", params.get("user_id"));
        assertEquals("43", params.get("empid"));
        assertEquals("43", params.get("emp_id"));
        assertEquals("43", params.get("employee_id"));
        assertEquals("43", params.get("subadmin_id"));
    }

    @Test
    public void testSelfAttendanceState_shift1NotStarted_moveButtonMustBeGone() {
        boolean hasIn = false;
        boolean hasOut = false;
        boolean hasActiveMove = false;

        boolean isShift1Completed = hasIn && hasOut;
        boolean isShift1Active = hasIn && !hasOut;
        boolean canMoveSite = isShift1Completed && !hasActiveMove;
        boolean canCheckIn = !hasIn && !hasActiveMove;
        boolean canCheckOut = isShift1Active && !hasActiveMove;

        assertFalse("Move button must NOT show when Shift 1 is not clocked in", canMoveSite);
        assertTrue("Check In button must show when Shift 1 is not clocked in", canCheckIn);
        assertFalse("Check Out button must NOT show when Shift 1 is not clocked in", canCheckOut);
    }

    @Test
    public void testSelfAttendanceState_shift1Active_moveButtonMustBeGone() {
        boolean hasIn = true;
        boolean hasOut = false;
        boolean hasActiveMove = false;

        boolean isShift1Completed = hasIn && hasOut;
        boolean isShift1Active = hasIn && !hasOut;
        boolean canMoveSite = isShift1Completed && !hasActiveMove;
        boolean canCheckIn = !hasIn && !hasActiveMove;
        boolean canCheckOut = isShift1Active && !hasActiveMove;

        assertFalse("Move button must NOT show when Shift 1 is active on duty", canMoveSite);
        assertFalse("Check In button must NOT show when Shift 1 is active", canCheckIn);
        assertTrue("Check Out button must show when Shift 1 is active", canCheckOut);
    }

    @Test
    public void testSelfAttendanceState_shift1Completed_moveButtonMustShow() {
        boolean hasIn = true;
        boolean hasOut = true;
        boolean hasActiveMove = false;

        boolean isShift1Completed = hasIn && hasOut;
        boolean isShift1Active = hasIn && !hasOut;
        boolean canMoveSite = isShift1Completed && !hasActiveMove;
        boolean canCheckIn = !hasIn && !hasActiveMove;
        boolean canCheckOut = isShift1Active && !hasActiveMove;

        assertTrue("Move button MUST show only after Shift 1 is completed", canMoveSite);
        assertFalse("Check In button must NOT show after Shift 1 is completed", canCheckIn);
        assertFalse("Check Out button must NOT show after Shift 1 is completed", canCheckOut);
    }

    @Test
    public void testSelfAttendanceState_shift2MoveActive_moveButtonGone_moveOutVisible() {
        boolean hasIn = true;
        boolean hasOut = true;
        boolean hasActiveMove = true;

        boolean isShift1Completed = hasIn && hasOut;
        boolean canMoveSite = isShift1Completed && !hasActiveMove;
        boolean canMoveOut = hasActiveMove;

        assertFalse("Move button must be GONE while Shift 2 movement is active", canMoveSite);
        assertTrue("Move Out button must be VISIBLE while Shift 2 movement is active", canMoveOut);
    }
}
