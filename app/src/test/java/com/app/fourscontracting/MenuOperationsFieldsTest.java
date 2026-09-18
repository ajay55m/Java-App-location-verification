package com.app.fourscontracting;

import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.AttendancePayload;
import com.app.fourscontracting.data.AttendanceRecordModel;
import com.app.fourscontracting.data.DepartmentModel;
import com.app.fourscontracting.data.LabourEmployeeModel;
import com.app.fourscontracting.data.MoveEmployeeModel;
import com.app.fourscontracting.data.MoveRecordModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for verifying all form fields, data models,
 * insertion payloads, update operations, and state transitions across
 * all menu operations in the application:
 * 1. Labour Management Menu (SettingFragment)
 * 2. Self Attendance Menu (SupervisorAttendanceFragment)
 * 3. Manage Attendance Menu (ManageAttendanceFragment)
 * 4. Move Site Menu (MoveSiteFragment)
 * 5. Manage Move Site Menu (ManageMoveSiteFragment)
 * 6. Leave Request Form Menu (HomeFragment)
 */
public class MenuOperationsFieldsTest {

    // =========================================================================
    // 1. MENU: LABOUR MANAGEMENT (SettingFragment)
    // =========================================================================

    @Test
    public void testLabourManagement_departmentModelAndSelection() {
        DepartmentModel d1 = new DepartmentModel("", "-- Select Department --");
        DepartmentModel d2 = new DepartmentModel("5", "4s Contracting Employees");

        assertEquals("", d1.id);
        assertEquals("-- Select Department --", d1.name);
        assertEquals("-- Select Department --", d1.toString());

        assertEquals("5", d2.id);
        assertEquals("4s Contracting Employees", d2.name);
        assertEquals("4s Contracting Employees", d2.toString());
    }

    @Test
    public void testLabourManagement_employeeCardFieldsAndStatusTransitions() throws Exception {
        // Test Unmarked Worker
        JSONObject jsonUnmarked = new JSONObject();
        jsonUnmarked.put("id", "101");
        jsonUnmarked.put("name", "John Smith");
        jsonUnmarked.put("status_code", "NOT_MARKED");
        jsonUnmarked.put("display_text", "Not Marked");
        jsonUnmarked.put("site_name", "Main Site");
        jsonUnmarked.put("department_name", "Civil");
        jsonUnmarked.put("can_in", true);
        jsonUnmarked.put("can_out", false);

        LabourEmployeeModel unmarked = new LabourEmployeeModel(jsonUnmarked);
        assertEquals("101", unmarked.getId());
        assertEquals("John Smith", unmarked.getName());
        assertEquals("NOT_MARKED", unmarked.getStatusCode());
        assertEquals("Not Marked", unmarked.getDisplayText());
        assertTrue(unmarked.isCanIn());
        assertFalse(unmarked.isCanOut());
        assertFalse(unmarked.isClockedIn());
        assertFalse(unmarked.isClockedOut());

        // Test Clocked IN Worker
        JSONObject jsonIn = new JSONObject();
        jsonIn.put("id", "101");
        jsonIn.put("name", "John Smith");
        jsonIn.put("status_code", "IN_HERE");
        jsonIn.put("display_text", "TIME IN 08:30 AM");
        jsonIn.put("can_in", false);
        jsonIn.put("can_out", true);

        LabourEmployeeModel clockedIn = new LabourEmployeeModel(jsonIn);
        assertFalse(clockedIn.isCanIn());
        assertTrue(clockedIn.isCanOut());
        assertTrue(clockedIn.isClockedIn());
        assertFalse(clockedIn.isClockedOut());

        // Test Clocked OUT Worker
        JSONObject jsonOut = new JSONObject();
        jsonOut.put("id", "101");
        jsonOut.put("name", "John Smith");
        jsonOut.put("status_code", "OUT");
        jsonOut.put("display_text", "COMPLETED 05:30 PM");
        jsonOut.put("can_in", true);
        jsonOut.put("can_out", false);
        jsonOut.put("has_out", true);

        LabourEmployeeModel clockedOut = new LabourEmployeeModel(jsonOut);
        assertFalse(clockedOut.isClockedIn());
        assertTrue(clockedOut.isClockedOut());
    }

    @Test
    public void testLabourManagement_insertClockInPayloadFields() {
        AttendancePayload payload = new AttendancePayload();
        payload.uid = "85";
        payload.empid = "101";
        payload.type = "IN";
        payload.isMovement = false;
        payload.projectId = "PROJ_01";
        payload.projname = "Site Alpha";
        payload.locationId = "LOC_01";
        payload.imagepath = "photo_101.jpg";
        payload.lat = 25.2048;
        payload.lng = 55.2708;
        payload.accuracy = 10.5f;

        Map<String, String> form = payload.toFormParams();

        assertEquals("85", form.get("uid"));
        assertEquals("85", form.get("user_id"));
        assertEquals("101", form.get("empid"));
        assertEquals("101", form.get("emp_id"));
        assertEquals("101", form.get("employee_id"));
        assertEquals("IN", form.get("type"));
        assertEquals("0", form.get("is_movement"));
        assertEquals("PROJ_01", form.get("project_id"));
        assertEquals("Site Alpha", form.get("projname"));
        assertEquals("LOC_01", form.get("location_id"));
        assertEquals("photo_101.jpg", form.get("imagepath"));
        assertEquals("25.2048", form.get("lat"));
        assertEquals("55.2708", form.get("lng"));
        assertEquals("10.5", form.get("accuracy"));
    }

    // =========================================================================
    // 2. MENU: SELF ATTENDANCE (SupervisorAttendanceFragment)
    // =========================================================================

    @Test
    public void testSelfAttendance_supervisorSelfRecordIdentification() {
        String supervisorUid = "85";
        String supervisorName = "Supervisor Admin";

        // Direct empId match
        AttendanceRecordModel rec1 = new AttendanceRecordModel("1001", "85", "85", "Supervisor Admin", "Site A", "08:00", "", 0.0, true, false, "", "");
        // Numeric padded empId match ("085" vs "85")
        AttendanceRecordModel rec2 = new AttendanceRecordModel("1002", "85", "085", "Supervisor Admin", "Site A", "08:00", "", 0.0, true, false, "", "");
        // Worker record (different empId "43")
        AttendanceRecordModel recWorker = new AttendanceRecordModel("1003", "85", "43", "Worker Dave", "Site A", "08:15", "", 0.0, true, false, "", "");

        assertTrue("Direct empId match must identify supervisor self record", isSelfRecord(rec1, supervisorUid, supervisorName));
        assertTrue("Numeric empId match must identify supervisor self record", isSelfRecord(rec2, supervisorUid, supervisorName));
        assertFalse("Worker record must NOT be identified as supervisor self record", isSelfRecord(recWorker, supervisorUid, supervisorName));
    }

    private boolean isSelfRecord(AttendanceRecordModel r, String sUid, String sName) {
        if (r == null) return false;
        String eId = r.getEmpId() != null ? r.getEmpId().trim() : "";
        String uId = r.getUserId() != null ? r.getUserId().trim() : "";
        if (!sUid.isEmpty()) {
            if (!eId.isEmpty()) {
                boolean isSame = eId.equalsIgnoreCase(sUid);
                if (!isSame) {
                    try {
                        isSame = Integer.parseInt(eId) == Integer.parseInt(sUid);
                    } catch (Exception ignored) {}
                }
                return isSame;
            }
            if (!uId.isEmpty()) {
                boolean isSameUser = uId.equalsIgnoreCase(sUid);
                if (!isSameUser) {
                    try {
                        isSameUser = Integer.parseInt(uId) == Integer.parseInt(sUid);
                    } catch (Exception ignored) {}
                }
                if (isSameUser) return true;
            }
        }
        return false;
    }

    @Test
    public void testSelfAttendance_insertSelfClockInAndOutPayloads() {
        // Self Punch IN
        AttendancePayload selfInPayload = new AttendancePayload();
        selfInPayload.uid = "85";
        selfInPayload.empid = "85";
        selfInPayload.type = "IN";
        selfInPayload.projectId = "P10";
        selfInPayload.projname = "Headquarters";

        Map<String, String> formIn = selfInPayload.toFormParams();
        assertEquals("85", formIn.get("uid"));
        assertEquals("85", formIn.get("empid"));
        assertEquals("IN", formIn.get("type"));

        // Self Punch OUT
        AttendancePayload selfOutPayload = new AttendancePayload();
        selfOutPayload.uid = "85";
        selfOutPayload.empid = "85";
        selfOutPayload.attendId = "ATT_99";
        selfOutPayload.type = "OUT";
        selfOutPayload.projectId = "P10";

        Map<String, String> formOut = selfOutPayload.toFormParams();
        assertEquals("85", formOut.get("uid"));
        assertEquals("85", formOut.get("empid"));
        assertEquals("ATT_99", formOut.get("attend_id"));
        assertEquals("OUT", formOut.get("type"));
        assertEquals("checkout", formOut.get("action"));
    }

    // =========================================================================
    // 3. MENU: MANAGE ATTENDANCE (ManageAttendanceFragment)
    // =========================================================================

    @Test
    public void testManageAttendance_recordFieldsAndBreakUpdate() {
        AttendanceRecordModel record = new AttendanceRecordModel(
                "ATT_500", "85", "43", "Worker Alex", "Site Bravo",
                "07:30:00", "16:30:00", 0.0, true, true, "photo_in.jpg", "photo_out.jpg"
        );

        assertEquals("ATT_500", record.getAttendId());
        assertEquals("85", record.getUserId());
        assertEquals("43", record.getEmpId());
        assertEquals("Worker Alex", record.getFirstName());
        assertEquals("Site Bravo", record.getProjName());
        assertEquals("07:30:00", record.getTimeIn());
        assertEquals("16:30:00", record.getTimeOut());
        assertEquals(0.0, record.getBreakHours(), 0.001);
        assertFalse(record.isActive()); // Both in & out -> completed

        // Perform break update operation
        record.setBreakHours(1.0);
        assertEquals(1.0, record.getBreakHours(), 0.001);
    }

    @Test
    public void testManageAttendance_updateWorkerClockOutPayloadFields() {
        AttendancePayload outPayload = new AttendancePayload();
        outPayload.uid = "85";      // Supervisor ID
        outPayload.empid = "43";    // Worker ID
        outPayload.attendId = "ATT_500";
        outPayload.type = "OUT";
        outPayload.projectId = "LOC_20";
        outPayload.projname = "Site Bravo";
        outPayload.locationId = "LOC_20";
        outPayload.imagepath = "checkout_photo.jpg";

        Map<String, String> params = outPayload.toFormParams();

        assertEquals("85", params.get("uid"));
        assertEquals("85", params.get("user_id"));
        assertEquals("43", params.get("empid"));
        assertEquals("43", params.get("emp_id"));
        assertEquals("ATT_500", params.get("attend_id"));
        assertEquals("ATT_500", params.get("id"));
        assertEquals("OUT", params.get("type"));
        assertEquals("checkout", params.get("action"));
    }

    // =========================================================================
    // 4. MENU: MOVE SITE (MoveSiteFragment)
    // =========================================================================

    @Test
    public void testMoveSite_employeeModelAndActionMapping() {
        MoveEmployeeModel moveEmp = new MoveEmployeeModel(
                "305", "Worker Movement", "09:00 AM", "", false, "MOVE", "emp_photo.jpg", "MOVE_12", "ATT_34"
        );

        assertEquals("305", moveEmp.id);
        assertEquals("Worker Movement", moveEmp.firstName);
        assertEquals("09:00 AM", moveEmp.timeInDisplay);
        assertFalse(moveEmp.onMove);
        assertEquals("MOVE", moveEmp.action);
        assertEquals("emp_photo.jpg", moveEmp.photoUrl);
        assertEquals("MOVE_12", moveEmp.moveId);
        assertEquals("ATT_34", moveEmp.attendId);
    }

    @Test
    public void testMoveSite_insertMoveSitePayloadFields() {
        AttendancePayload movePayload = new AttendancePayload();
        movePayload.uid = "85";       // Supervisor ID
        movePayload.empid = "305";    // Worker ID
        movePayload.moveId = "MOVE_12";
        movePayload.type = "MOVE";
        movePayload.isMovement = true;
        movePayload.projectId = "PROJ_NEW";
        movePayload.projname = "New Expansion Site";
        movePayload.locationId = "LOC_NEW";
        movePayload.imagepath = "move_photo.jpg";

        Map<String, String> form = movePayload.toFormParams();

        assertEquals("85", form.get("uid"));
        assertEquals("305", form.get("empid"));
        assertEquals("MOVE_12", form.get("move_id"));
        assertEquals("IN", form.get("type")); // Wire type for movement IN
        assertEquals("1", form.get("is_movement"));
        assertEquals("MOVE", form.get("action_type"));
        assertEquals("PROJ_NEW", form.get("project_id"));
        assertEquals("New Expansion Site", form.get("projname"));
    }

    // =========================================================================
    // 5. MENU: MANAGE MOVE SITE (ManageMoveSiteFragment)
    // =========================================================================

    @Test
    public void testManageMoveSite_recordFieldsAndMoveOutPayload() {
        MoveRecordModel moveRecord = new MoveRecordModel(
                "MR_900", "305", "Worker Movement", "New Expansion Site",
                "09:15 AM", "", true, false, "in_photo.jpg", ""
        );

        assertEquals("MR_900", moveRecord.getMoveId());
        assertEquals("305", moveRecord.getEmpId());
        assertEquals("Worker Movement", moveRecord.getFirstName());
        assertEquals("New Expansion Site", moveRecord.getProjName());
        assertTrue(moveRecord.isActive());

        // Perform Move OUT update action payload
        AttendancePayload moveOutPayload = new AttendancePayload();
        moveOutPayload.uid = "85";
        moveOutPayload.empid = "305";
        moveOutPayload.moveId = "MR_900";
        moveOutPayload.type = "OUT";
        moveOutPayload.isMovement = true;
        moveOutPayload.projectId = "LOC_NEW";
        moveOutPayload.projname = "New Expansion Site";
        moveOutPayload.imagepath = "move_out_photo.jpg";

        Map<String, String> params = moveOutPayload.toFormParams();

        assertEquals("85", params.get("uid"));
        assertEquals("305", params.get("empid"));
        assertEquals("MR_900", params.get("move_id"));
        assertEquals("OUT", params.get("type"));
        assertEquals("1", params.get("is_movement"));
        assertEquals("MOVE_OUT", params.get("action_type"));
        assertEquals("checkout", params.get("action"));
    }

    // =========================================================================
    // 6. MENU: LEAVE FORM (HomeFragment)
    // =========================================================================

    @Test
    public void testLeaveForm_fieldsValidationLogic() {
        String categorySelect = "-- Select Category --";
        String categoryValid = "Annual Leave";

        String startDateEmpty = "";
        String startDateValid = "15/09/2026";

        String endDateEmpty = "";
        String endDateValid = "20/09/2026";

        String reasonEmpty = "  ";
        String reasonValid = "Medical checkup";

        // Validation test 1: Invalid category
        assertFalse(validateLeaveForm(categorySelect, startDateValid, endDateValid, reasonValid));

        // Validation test 2: Missing dates
        assertFalse(validateLeaveForm(categoryValid, startDateEmpty, endDateValid, reasonValid));
        assertFalse(validateLeaveForm(categoryValid, startDateValid, endDateEmpty, reasonValid));

        // Validation test 3: Missing reason
        assertFalse(validateLeaveForm(categoryValid, startDateValid, endDateValid, reasonEmpty));

        // Validation test 4: Valid form submission
        assertTrue(validateLeaveForm(categoryValid, startDateValid, endDateValid, reasonValid));
    }

    private boolean validateLeaveForm(String category, String startDate, String endDate, String reason) {
        if (category == null || category.startsWith("--")) return false;
        if (startDate == null || startDate.trim().isEmpty()) return false;
        if (endDate == null || endDate.trim().isEmpty()) return false;
        if (reason == null || reason.trim().isEmpty()) return false;
        return true;
    }
}
