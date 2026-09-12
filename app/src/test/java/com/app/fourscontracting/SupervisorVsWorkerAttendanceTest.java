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
    public void testSupervisorSelfRecordMatching() {
        String supervisorUid = "101";

        AttendanceRecordModel supervisorRecord = new AttendanceRecordModel(
                "888", supervisorUid, supervisorUid, "Supervisor Ajay", "Site A",
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
}
