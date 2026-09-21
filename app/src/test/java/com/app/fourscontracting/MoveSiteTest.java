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

    @Test
    public void testAttendancePayloadMoveInAndOutImageParameters() {
        // Move IN image payload parameters
        com.app.fourscontracting.data.AttendancePayload inPayload = new com.app.fourscontracting.data.AttendancePayload();
        inPayload.empid = "43";
        inPayload.uid = "85";
        inPayload.type = "IN";
        inPayload.isMovement = true;
        inPayload.imagepath = "move_in_captured_base64_or_path";

        java.util.Map<String, String> inForm = inPayload.toFormParams();
        assertEquals("43", inForm.get("empid"));
        assertEquals("85", inForm.get("uid"));
        assertEquals("move_in_captured_base64_or_path", inForm.get("imagepath"));
        assertEquals("move_in_captured_base64_or_path", inForm.get("in_photo"));
        assertEquals("move_in_captured_base64_or_path", inForm.get("move_in_photo"));
        assertEquals("move_in_captured_base64_or_path", inForm.get("photo_in"));

        // Move OUT image payload parameters
        com.app.fourscontracting.data.AttendancePayload outPayload = new com.app.fourscontracting.data.AttendancePayload();
        outPayload.empid = "43";
        outPayload.uid = "85";
        outPayload.moveId = "105";
        outPayload.type = "OUT";
        outPayload.isMovement = true;
        outPayload.imagepath = "move_out_captured_base64_or_path";

        java.util.Map<String, String> outForm = outPayload.toFormParams();
        assertEquals("43", outForm.get("empid"));
        assertEquals("105", outForm.get("move_id"));
        assertEquals("move_out_captured_base64_or_path", outForm.get("imagepath"));
        assertEquals("move_out_captured_base64_or_path", outForm.get("out_photo"));
        assertEquals("move_out_captured_base64_or_path", outForm.get("move_out_photo"));
        assertEquals("move_out_captured_base64_or_path", outForm.get("photo_out"));
    }

    @Test
    public void testMoveRecordPhotoUrlResolution() {
        // Case 1: Direct photo URL provided in API response
        MoveRecordModel recordWithDirectPhoto = new MoveRecordModel(
                "60", "301", "Ajay", "Site A", "11:16 AM", "", true, false,
                "https://4scontracting.com/uploads/photo1.jpg", ""
        );
        assertEquals("https://4scontracting.com/uploads/photo1.jpg", recordWithDirectPhoto.getInPhotoUrl());

        // Case 2: Fallback endpoint generation when inPhotoUrl is empty but inTime ("11:16 AM") is valid
        MoveRecordModel recordWithTimeInFallback = new MoveRecordModel(
                "60", "301", "Ajay", "Site A", "11:16 AM", "", false, false,
                "", ""
        );
        assertTrue("isHasIn should return true when valid inTime exists", recordWithTimeInFallback.isHasIn());
        assertEquals("Fallback Check-in photo URL", "view_attendance_img.php?move_id=60&type=in&source=hrms", recordWithTimeInFallback.getInPhotoUrl());

        // Case 3: Check-out photo fallback when outTime ("11:16 AM") is valid
        MoveRecordModel recordWithTimeOutFallback = new MoveRecordModel(
                "61", "302", "Ajay", "Site B", "10:55 AM", "11:16 AM", true, false,
                "", ""
        );
        assertTrue("isHasOut should return true when valid outTime exists", recordWithTimeOutFallback.isHasOut());
        assertEquals("Fallback Check-out photo URL", "view_attendance_img.php?move_id=61&type=out&source=hrms", recordWithTimeOutFallback.getOutPhotoUrl());

        // Case 4: Numeric Image ID mapping for IN and OUT
        MoveRecordModel recordWithNumericImageIds = new MoveRecordModel(
                "75", "303", "Ajay", "Site C", "09:00 AM", "05:00 PM", true, true,
                "1234", "5678"
        );
        assertEquals("view_attendance_img.php?image_id=1234&move_id=75&type=in&source=hrms", recordWithNumericImageIds.getInPhotoUrl());
        assertEquals("view_attendance_img.php?image_id=5678&move_id=75&type=out&source=hrms", recordWithNumericImageIds.getOutPhotoUrl());

        // Case 5: Move record move_id=60 fallback image endpoint resolution when photo fields are empty in JSON
        MoveRecordModel recordMove60 = new MoveRecordModel(
                "60", "304", "Ajay", "Site D", "", "", false, false,
                "", ""
        );
        assertEquals("view_attendance_img.php?move_id=60&type=in&source=hrms", recordMove60.getInPhotoUrl());
        assertEquals("view_attendance_img.php?move_id=60&type=out&source=hrms", recordMove60.getOutPhotoUrl());
    }
}
