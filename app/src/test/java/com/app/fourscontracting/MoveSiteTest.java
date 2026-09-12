package com.app.fourscontracting;

import com.app.fourscontracting.data.MoveEmployeeModel;
import com.app.fourscontracting.data.MoveRecordModel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Move Site pending queue filtering, active move detection, and Move Record model attributes.
 */
public class MoveSiteTest {

    @Test
    public void testMoveSiteQueueFiltering_filtersActiveMovingWorkers() {
        // Worker 1: Ready to move
        MoveEmployeeModel worker1 = new MoveEmployeeModel(
                "201", "John Ready", "08:00 AM", "", false, "MOVE", "photo1.jpg"
        );

        // Worker 2: Currently active in site movement (onMove == true)
        MoveEmployeeModel worker2 = new MoveEmployeeModel(
                "202", "Alex Moving", "08:15 AM", "", true, "OUT", "photo2.jpg"
        );

        List<MoveEmployeeModel> allWorkers = new ArrayList<>();
        allWorkers.add(worker1);
        allWorkers.add(worker2);

        List<MoveEmployeeModel> moveSiteQueue = new ArrayList<>();
        int readyCount = 0;
        int movingCount = 0;

        for (MoveEmployeeModel emp : allWorkers) {
            if (emp.onMove) {
                movingCount++;
            } else {
                readyCount++;
                moveSiteQueue.add(emp);
            }
        }

        assertEquals("Total ready workers count", 1, readyCount);
        assertEquals("Total actively moving workers count", 1, movingCount);
        assertEquals("Move site queue must only contain workers ready for site move", 1, moveSiteQueue.size());
        assertEquals("John Ready", moveSiteQueue.get(0).firstName);
        assertFalse("Worker in queue must not be on move", moveSiteQueue.get(0).onMove);
    }

    @Test
    public void testMoveRecordModel_empIdAndMoveOutActionAvailability() {
        // Record 1: Active site movement (outTime is empty / "--")
        MoveRecordModel activeRecord = new MoveRecordModel(
                "MR101",
                "301",
                "Michael Scott",
                "Project Alpha",
                "09:15 AM",
                "",
                true,
                false,
                "in_photo.jpg",
                ""
        );

        // Record 2: Completed site movement
        MoveRecordModel completedRecord = new MoveRecordModel(
                "MR102",
                "302",
                "Jim Halpert",
                "Project Alpha",
                "08:00 AM",
                "04:30 PM",
                true,
                true,
                "in_photo.jpg",
                "out_photo.jpg"
        );

        assertEquals("Worker empId in active move record", "301", activeRecord.getEmpId());
        assertEquals("Worker name", "Michael Scott", activeRecord.getFirstName());
        assertEquals("Project name", "Project Alpha", activeRecord.getProjName());
        assertTrue("Active move record must be identified as active", activeRecord.isActive());

        assertEquals("Worker empId in completed move record", "302", completedRecord.getEmpId());
        assertNotNull("Completed record out time", completedRecord.getOutTime());
        assertFalse("Completed record out time is not active", completedRecord.isActive());
    }

    @Test
    public void testMoveSiteDepartmentSelectionPreservation() {
        List<com.app.fourscontracting.data.DepartmentModel> deptList = new ArrayList<>();
        deptList.add(new com.app.fourscontracting.data.DepartmentModel("", "-- Select Department --"));
        deptList.add(new com.app.fourscontracting.data.DepartmentModel("5", "4s Contracting Employees"));
        deptList.add(new com.app.fourscontracting.data.DepartmentModel("9", "Civil Subcontractors"));

        // Case: User previously selected Dept ID "9"
        String activeDeptId = "9";
        int targetPosition = -1;

        if (!activeDeptId.isEmpty()) {
            for (int i = 0; i < deptList.size(); i++) {
                if (activeDeptId.equalsIgnoreCase(deptList.get(i).id)) {
                    targetPosition = i;
                    break;
                }
            }
        }

        assertEquals("Should preserve selected department position (Dept 9 at index 2)", 2, targetPosition);
    }

    @Test
    public void testAttendancePayloadMoveIdAndFormFields() {
        com.app.fourscontracting.data.AttendancePayload payload = new com.app.fourscontracting.data.AttendancePayload();
        payload.empid = "85";
        payload.uid = "34";
        payload.moveId = "MR99";
        payload.attendId = "ATT512";
        payload.type = "OUT";
        payload.isMovement = true;

        java.util.Map<String, String> form = payload.toFormParams();
        assertEquals("85", form.get("empid"));
        assertEquals("85", form.get("emp_id"));
        assertEquals("85", form.get("employee_id"));
        assertEquals("34", form.get("user_id"));
        assertEquals("34", form.get("uid"));
        assertEquals("34", form.get("subadmin_id"));
        assertEquals("34", form.get("manager_uid"));
        assertEquals("MR99", form.get("move_id"));
        assertEquals("MR99", form.get("moveid"));
        assertEquals("ATT512", form.get("attend_id"));
        assertEquals("ATT512", form.get("attendance_id"));
        assertEquals("ATT512", form.get("id"));
        assertEquals("OUT", form.get("type"));
        assertEquals("checkout", form.get("action"));
        assertEquals("1", form.get("is_movement"));
        assertEquals("MOVE_OUT", form.get("action_type"));
    }
}
