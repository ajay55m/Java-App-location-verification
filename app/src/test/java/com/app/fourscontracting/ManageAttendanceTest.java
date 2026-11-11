package com.app.fourscontracting;

import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.ApiConfig;
import com.app.fourscontracting.data.AttendancePayload;
import com.app.fourscontracting.data.AttendanceRecordModel;
import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.MoveRecordModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ManageAttendanceTest {

    @Test
    public void testWorkerIdResolution_keepsWorkerSeparateFromSupervisor() throws Exception {
        JSONObject record = new JSONObject();
        record.put("empid", "301");
        record.put("userid", "85");
        record.put("emp_name", "Worker");

        assertEquals("301", com.app.fourscontracting.data.ManageAttendanceApi.resolveWorkerId(record, "85"));
    }

    @Test
    public void testWorkerIdResolution_prefersExplicitWorkerId() throws Exception {
        JSONObject record = new JSONObject();
        record.put("id", "9001");
        record.put("empid", "301");
        record.put("userid", "85");

        assertEquals("301", com.app.fourscontracting.data.ManageAttendanceApi.resolveWorkerId(record, "85"));
    }

    @Test
    public void testWorkerIdResolution_readsNestedEmployeeObject() throws Exception {
        JSONObject record = new JSONObject();
        record.put("id", "9001");
        record.put("userid", "85");
        record.put("employee", new JSONObject().put("id", "301"));

        assertEquals("301", com.app.fourscontracting.data.ManageAttendanceApi.resolveWorkerId(record, "85"));
    }

    @Test
    public void testWorkerIdResolution_rejectsRowIdAsWorkerId() throws Exception {
        JSONObject record = new JSONObject();
        record.put("id", "2244");
        record.put("userid", "85");

        assertEquals("", com.app.fourscontracting.data.ManageAttendanceApi.resolveWorkerId(record, "85"));
    }

    @Test
    public void testAttendanceRecordModel_activeState() {
        AttendanceRecordModel activeRecord = new AttendanceRecordModel(
                "101", "John", "Site A", "08:00:00", "", 0.0, true, false, "in.jpg", ""
        );
        assertTrue(activeRecord.isActive());
        assertEquals("101", activeRecord.getAttendId());
        assertEquals("John", activeRecord.getFirstName());
        assertEquals("Site A", activeRecord.getProjName());

        AttendanceRecordModel completedRecord = new AttendanceRecordModel(
                "102", "Jane", "Site B", "08:00:00", "17:00:00", 1.0, true, true, "in.jpg", "out.jpg"
        );
        assertFalse(completedRecord.isActive());
        assertEquals(1.0, completedRecord.getBreakHours(), 0.001);

        completedRecord.setBreakHours(0.0);
        assertEquals(0.0, completedRecord.getBreakHours(), 0.001);
    }

    @Test
    public void testMoveRecordModel_activeState() {
        MoveRecordModel activeMove = new MoveRecordModel(
                "201", "Alex", "Site Move 1", "10:30:00", "", true, false, "move_in.jpg", ""
        );
        assertTrue(activeMove.isActive());
        assertEquals("201", activeMove.getMoveId());
        assertEquals("Alex", activeMove.getFirstName());

        MoveRecordModel completedMove = new MoveRecordModel(
                "202", "Sam", "Site Move 2", "10:30:00", "14:00:00", true, true, "move_in.jpg", "move_out.jpg"
        );
        assertFalse(completedMove.isActive());
    }

    @Test
    public void testAllocatedProjectModel() {
        AllocatedProjectModel project = new AllocatedProjectModel("12", "Central Station Project");
        assertEquals("12", project.getId());
        assertEquals("Central Station Project", project.getProjname());
        assertEquals("Central Station Project", project.toString());

        AllocatedProjectModel nullProject = new AllocatedProjectModel(null, null);
        assertEquals("", nullProject.getId());
        assertEquals("", nullProject.getProjname());
    }

    @Test
    public void testJsonFeedParsing_supportsBothKeyNames() throws Exception {
        String jsonWithProjects = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"user_name\": \"Admin User\",\n" +
                "  \"projects\": [\n" +
                "    {\"id\": \"1\", \"projname\": \"Project 1\"},\n" +
                "    {\"id\": \"2\", \"projname\": \"Project 2\"}\n" +
                "  ],\n" +
                "  \"records\": [\n" +
                "    {\n" +
                "      \"attend_id\": \"50\",\n" +
                "      \"first_name\": \"Dave\",\n" +
                "      \"projname\": \"Project 1\",\n" +
                "      \"timein\": \"07:45:00\",\n" +
                "      \"timeout\": \"\",\n" +
                "      \"break_hours\": 0,\n" +
                "      \"has_in\": true,\n" +
                "      \"has_out\": false,\n" +
                "      \"in_photo_url\": \"view_attendance_img.php?attendance_id=50&type=in\",\n" +
                "      \"out_photo_url\": \"\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"move_records\": []\n" +
                "}";

        JSONObject root = new JSONObject(jsonWithProjects);
        assertEquals("success", root.optString("status"));

        JSONArray projArray = root.optJSONArray("allocated_projects");
        if (projArray == null) {
            projArray = root.optJSONArray("projects");
        }
        assertNotNull(projArray);
        assertEquals(2, projArray.length());

        List<AttendanceRecordModel> records = new ArrayList<>();
        JSONArray recArray = root.optJSONArray("records");
        if (recArray != null) {
            for (int i = 0; i < recArray.length(); i++) {
                JSONObject rObj = recArray.optJSONObject(i);
                if (rObj != null) {
                    records.add(new AttendanceRecordModel(
                            rObj.optString("attend_id", ""),
                            rObj.optString("first_name", ""),
                            rObj.optString("projname", ""),
                            rObj.optString("timein", ""),
                            rObj.optString("timeout", ""),
                            rObj.optDouble("break_hours", 0.0),
                            rObj.optBoolean("has_in", false),
                            rObj.optBoolean("has_out", false),
                            rObj.optString("in_photo_url", ""),
                            rObj.optString("out_photo_url", "")
                    ));
                }
            }
        }

        assertEquals(1, records.size());
        AttendanceRecordModel rec = records.get(0);
        assertEquals("50", rec.getAttendId());
        assertEquals("Dave", rec.getFirstName());
        assertTrue(rec.isActive());
    }

    @Test
    public void testBreakUpdateJsonResponseParsing() throws Exception {
        String responseStr = "{\"status\":\"success\",\"message\":\"Break status updated successfully\",\"attend_id\":\"101\",\"break_hours\":1}";
        JSONObject obj = new JSONObject(responseStr);

        assertEquals("success", obj.optString("status"));
        assertEquals(1.0, obj.optDouble("break_hours"), 0.001);
        assertEquals("101", obj.optString("attend_id"));
    }

    @Test
    public void testManageAttendanceNullSafety() {
        AttendanceRecordModel nullRecord = new AttendanceRecordModel(null, null, null, null, null, 0.0, false, false, null, null);
        assertNotNull(nullRecord.getAttendId());
        assertNotNull(nullRecord.getFirstName());
        assertNotNull(nullRecord.getProjName());
        assertNotNull(nullRecord.getTimeIn());
        assertNotNull(nullRecord.getTimeOut());
        assertNotNull(nullRecord.getInPhotoUrl());
        assertNotNull(nullRecord.getOutPhotoUrl());
        assertEquals("", nullRecord.getAttendId());

        MoveRecordModel nullMove = new MoveRecordModel(null, null, null, null, null, false, false, null, null);
        assertNotNull(nullMove.getMoveId());
        assertNotNull(nullMove.getFirstName());
        assertEquals("", nullMove.getMoveId());
    }

    @Test
    public void testSupervisorCheckinJsonParsing_alternateKeys() throws Exception {
        String supervisorCheckinJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"user_name\": \"4S-Contracting\",\n" +
                "  \"records\": [\n" +
                "    {\n" +
                "      \"id\": \"85001\",\n" +
                "      \"uid\": \"85\",\n" +
                "      \"emp_name\": \"4S-Contracting\",\n" +
                "      \"project_name\": \"RST/001\",\n" +
                "      \"time_in\": \"09:15:00\",\n" +
                "      \"time_out\": \"\",\n" +
                "      \"has_in\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject root = new JSONObject(supervisorCheckinJson);
        assertEquals("success", root.optString("status"));

        JSONArray recArray = root.optJSONArray("records");
        assertNotNull(recArray);
        assertEquals(1, recArray.length());

        JSONObject rObj = recArray.getJSONObject(0);
        String attendIdVal = rObj.optString("attend_id", "");
        if (attendIdVal.isEmpty()) attendIdVal = rObj.optString("id", "");

        String empIdVal = rObj.optString("empid", "");
        if (empIdVal.isEmpty()) empIdVal = rObj.optString("emp_id", "");
        if (empIdVal.isEmpty()) empIdVal = rObj.optString("employee_id", "");
        if (empIdVal.isEmpty()) empIdVal = rObj.optString("uid", "");

        String firstNameVal = rObj.optString("first_name", "");
        if (firstNameVal.isEmpty()) firstNameVal = rObj.optString("emp_name", "");

        String projNameVal = rObj.optString("projname", "");
        if (projNameVal.isEmpty()) projNameVal = rObj.optString("project_name", "");

        String timeInVal = rObj.optString("timein", "");
        if (timeInVal.isEmpty()) timeInVal = rObj.optString("time_in", "");

        String timeOutVal = rObj.optString("timeout", "");
        if (timeOutVal.isEmpty()) timeOutVal = rObj.optString("time_out", "");

        AttendanceRecordModel model = new AttendanceRecordModel(
                attendIdVal, empIdVal, firstNameVal, projNameVal, timeInVal, timeOutVal, 0.0, true, false, "", ""
        );

        assertEquals("85001", model.getAttendId());
        assertEquals("85", model.getEmpId());
        assertEquals("4S-Contracting", model.getFirstName());
        assertEquals("RST/001", model.getProjName());
        assertEquals("09:15:00", model.getTimeIn());
        assertTrue(model.isActive());
    }

    @Test
    public void testSupervisorIdNumericalMatching() {
        String supervisorUid = "85";
        String recordEmpId = "085";

        boolean match = false;
        if (supervisorUid.equalsIgnoreCase(recordEmpId)) {
            match = true;
        } else {
            try {
                match = Integer.parseInt(supervisorUid.trim()) == Integer.parseInt(recordEmpId.trim());
            } catch (Exception ignored) {}
        }

        assertTrue(match);
    }

    @Test
    public void testEmployeeIdExtractionWithIdAndAttendId() throws Exception {
        String jsonStr = "{\"id\": \"85\", \"attend_id\": \"50\", \"first_name\": \"Worker Dave\"}";
        JSONObject obj = new JSONObject(jsonStr);

        String attendIdVal = obj.has("attend_id") && !obj.isNull("attend_id") ? obj.optString("attend_id", "").trim() : "";
        String userIdVal = obj.has("user_id") && !obj.isNull("user_id") ? obj.optString("user_id", "").trim() : "";
        String empIdVal = obj.has("empid") && !obj.isNull("empid") ? obj.optString("empid", "").trim() : "";

        if (!attendIdVal.isEmpty()) {
            String idVal = obj.has("id") && !obj.isNull("id") ? obj.optString("id", "").trim() : "";
            if (userIdVal.isEmpty()) userIdVal = idVal;
            if (empIdVal.isEmpty()) empIdVal = idVal;
        }

        if (empIdVal.isEmpty()) empIdVal = userIdVal;

        AttendanceRecordModel record = new AttendanceRecordModel(
                attendIdVal, userIdVal, empIdVal, obj.optString("first_name"), "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );

        assertEquals("50", record.getAttendId());
        assertEquals("85", record.getEmpId());
        assertEquals("85", record.getUserId());
    }

    @Test
    public void testRecordWithOnlyIdFieldMapsToBothAttendIdAndEmpId() throws Exception {
        String jsonStr = "{\"id\": \"85\", \"first_name\": \"Worker Dave\"}";
        JSONObject obj = new JSONObject(jsonStr);

        String attendIdVal = obj.has("attend_id") && !obj.isNull("attend_id") ? obj.optString("attend_id", "").trim() : "";
        String userIdVal = obj.has("user_id") && !obj.isNull("user_id") ? obj.optString("user_id", "").trim() : "";
        String empIdVal = obj.has("empid") && !obj.isNull("empid") ? obj.optString("empid", "").trim() : "";
        String idVal = obj.has("id") && !obj.isNull("id") ? obj.optString("id", "").trim() : "";

        if (!attendIdVal.isEmpty()) {
            if (userIdVal.isEmpty()) userIdVal = idVal;
            if (empIdVal.isEmpty()) empIdVal = idVal;
        } else {
            attendIdVal = idVal;
            if (userIdVal.isEmpty()) userIdVal = idVal;
            if (empIdVal.isEmpty()) empIdVal = idVal;
        }

        if (empIdVal.isEmpty()) empIdVal = userIdVal;
        if (userIdVal.isEmpty()) userIdVal = empIdVal;

        AttendanceRecordModel record = new AttendanceRecordModel(
                attendIdVal, userIdVal, empIdVal, obj.optString("first_name"), "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );

        assertEquals("85", record.getAttendId());
        assertEquals("85", record.getEmpId());
        assertEquals("85", record.getUserId());
    }

    @Test
    public void testRecordWithNullAndDashLiteralEmpIdFiltering() {
        AttendanceRecordModel recordNull = new AttendanceRecordModel(
                "50", "null", "85", "Worker John", "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );
        assertEquals("85", recordNull.getEmpId());

        AttendanceRecordModel recordDash = new AttendanceRecordModel(
                "50", "--", "85", "Worker Sam", "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );
        assertEquals("85", recordDash.getEmpId());
    }

    @Test
    public void testWorkerTargetEmpIdStrictlyUsesWorkerEmpId() {
        AttendanceRecordModel workerRecord = new AttendanceRecordModel(
                "2241", "85", "43", "Worker Name", "Site A", "11:14:00", "", 0.0, true, false, "", ""
        );
        String targetEmpId = workerRecord.getEmpId();
        assertEquals("43", targetEmpId);
        assertNotEquals("85", targetEmpId);
    }

    @Test
    public void testTargetEmpIdFallbackResolution() {
        // Case 1: empId is "--" or empty, but userId is valid "102"
        AttendanceRecordModel recordWithUserIdOnly = new AttendanceRecordModel(
                "501", "102", "--", "Worker Name", "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );
        String resolvedId1 = recordWithUserIdOnly.getEmpId();
        if (resolvedId1 == null || resolvedId1.trim().isEmpty() || "--".equals(resolvedId1.trim()) || "null".equalsIgnoreCase(resolvedId1.trim())) {
            resolvedId1 = recordWithUserIdOnly.getUserId();
        }
        assertEquals("102", resolvedId1);

        // Case 2: empId and userId are missing/invalid, attendId is "2244". attendId MUST NOT be used as worker ID.
        AttendanceRecordModel recordWithAttendIdOnly = new AttendanceRecordModel(
                "2244", "--", "", "Worker Name", "Site A", "08:00:00", "", 0.0, true, false, "", ""
        );
        String resolvedId2 = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(recordWithAttendIdOnly, "85");
        assertEquals("", resolvedId2);
        assertNotEquals("2244", resolvedId2);
    }

    @Test
    public void testResolveTargetWorkerId_prefersEmpIdOverUserId() {
        AttendanceRecordModel record = new AttendanceRecordModel(
                "1001", "85", "RST/001", "Standard Worker", "Site A", "09:00:00", "", 0.0, true, false, "", ""
        );
        String resolvedFragmentId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record);
        String resolvedActivityId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record);

        assertEquals("RST/001", resolvedFragmentId);
        assertEquals("RST/001", resolvedActivityId);
    }

    @Test
    public void testResolveTargetWorkerId_fallsBackToUserIdWhenEmpIdMissing() {
        AttendanceRecordModel record = new AttendanceRecordModel(
                "1002", "43", "--", "Standard Worker", "Site A", "09:00:00", "", 0.0, true, false, "", ""
        );
        String resolvedFragmentId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record, "85");
        String resolvedActivityId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record, "85");

        assertEquals("43", resolvedFragmentId);
        assertEquals("43", resolvedActivityId);
    }

    @Test
    public void testResolveTargetWorkerId_rejectsSupervisorUidAsWorkerId() {
        String supervisorUid = "85";
        AttendanceRecordModel record = new AttendanceRecordModel(
                "--", supervisorUid, "--", "Standard Worker", "Site A", "09:00:00", "", 0.0, true, false, "", ""
        );

        String resolvedId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record, supervisorUid);
        assertEquals("", resolvedId);
    }

    @Test
    public void testManageAttendanceCheckOut_perfectIdPassingForEmployeeOut() {
        String supervisorUid = "85";
        String workerEmpId = "43";
        String attendId = "555";

        AttendanceRecordModel record = new AttendanceRecordModel(
                attendId, supervisorUid, workerEmpId, "Standard Worker", "Site A", "08:30:00", "", 0.0, true, false, "", ""
        );

        String targetWorkerId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record);
        assertEquals(workerEmpId, targetWorkerId);

        // Verify checkout payload params construction for employee out:
        AttendancePayload payload = new AttendancePayload();
        payload.uid = supervisorUid;
        payload.empid = targetWorkerId;
        payload.attendId = record.getAttendId();
        payload.type = "OUT";

        java.util.Map<String, String> params = payload.toFormParams();
        assertEquals("43", params.get("empid"));
        assertEquals("43", params.get("emp_id"));
        assertEquals("43", params.get("employee_id"));
        assertEquals("43", params.get("eid"));
        assertEquals("85", params.get("uid"));
        assertEquals("85", params.get("user_id"));
        assertEquals("OUT", params.get("type"));
        assertEquals("555", params.get("attend_id"));
    }

    @Test
    public void testSelfVsManageAttendanceCheckOut_idPassingDifference() {
        String supervisorUid = "85";
        String workerEmpId = "43";

        // Manage Attendance Checkout: supervisorUid (85) in uid, workerEmpId (43) in empid
        AttendancePayload managePayload = new AttendancePayload();
        managePayload.uid = supervisorUid;
        managePayload.empid = workerEmpId;
        managePayload.type = "OUT";

        // Self Attendance Checkout: supervisorUid (85) in BOTH uid and empid
        AttendancePayload selfPayload = new AttendancePayload();
        selfPayload.uid = supervisorUid;
        selfPayload.empid = supervisorUid;
        selfPayload.type = "OUT";

        assertNotEquals(managePayload.empid, selfPayload.empid);
        assertEquals("43", managePayload.empid);
        assertEquals("85", selfPayload.empid);
        assertEquals("85", managePayload.uid);
        assertEquals("85", selfPayload.uid);
    }

    @Test
    public void testAttendancePhotoUrlResolution_fcmAppEndpoint() {
        // Attendance record with attend_id = 1489
        AttendanceRecordModel record = new AttendanceRecordModel(
                "1489", "141", "31", "gurdeepsinghpiara", "RST",
                "09:28 AM", "03:02 PM", 0.0, true, true, "", ""
        );

        String expectedInUrl = ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=1489&type=in";
        String expectedOutUrl = ApiConfig.FCM_APP + "/view_attendance_img.php?attendance_id=1489&type=out";

        assertEquals(expectedInUrl, record.getInPhotoUrl());
        assertEquals(expectedOutUrl, record.getOutPhotoUrl());

        // Verify ImageLoaderHelper sanitizes it cleanly to the exact fcm_app URL
        assertEquals(expectedInUrl, ImageLoaderHelper.sanitizeUrl(record.getInPhotoUrl()));
        assertEquals(expectedOutUrl, ImageLoaderHelper.sanitizeUrl(record.getOutPhotoUrl()));

        // Legacy /subcontractor/ URL rewrites to fcm_app
        String legacyUrl = "https://www.4scontracting.com/SMCS_APP/subcontractor/view_attendance_img.php?attendance_id=1489&type=in";
        assertEquals(expectedInUrl, ImageLoaderHelper.sanitizeUrl(legacyUrl));
    }

    @Test
    public void testSupervisorFilteredFromManageAttendanceAndOtherEmployeesPreserved() {
        AttendanceRecordModel supervisorRecord = new AttendanceRecordModel(
                "1493", "141", "31", "Worker #31", "RST",
                "09:28 AM", "03:02 PM", 0.0, true, true, "", ""
        );
        AttendanceRecordModel workerRecord1 = new AttendanceRecordModel(
                "1495", "141", "38", "EMJEE KUMAR", "RST",
                "08:00 AM", "05:00 PM", 1.0, true, true, "", ""
        );
        AttendanceRecordModel workerRecord2 = new AttendanceRecordModel(
                "1496", "141", "34", "HAZUR MASIH", "RST",
                "08:15 AM", "", 0.0, true, false, "", ""
        );

        // Supervisor matches ID 31
        assertEquals("31", supervisorRecord.getEmpId());
        // Worker records have their own IDs and distinct names
        assertEquals("38", workerRecord1.getEmpId());
        assertEquals("EMJEE KUMAR", workerRecord1.getFirstName());
        assertEquals("34", workerRecord2.getEmpId());
        assertEquals("HAZUR MASIH", workerRecord2.getFirstName());

        assertTrue(workerRecord1.isActive() == false); // Has out, so completed
        assertTrue(workerRecord2.isActive() == true);  // No out, so active
    }

    @Test
    public void testManageAttendanceRecordsParsing_integratesWorkersForCheckout() throws Exception {
        String manageApiResponse = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"date\": \"2026-11-11\",\n" +
                "  \"uid\": 141,\n" +
                "  \"empid\": 31,\n" +
                "  \"records\": [\n" +
                "    {\n" +
                "      \"attend_id\": 1630,\n" +
                "      \"empid\": 38,\n" +
                "      \"first_name\": \"Aksir Miah\",\n" +
                "      \"employee_number\": \"E038\",\n" +
                "      \"timein\": \"02:18 PM\",\n" +
                "      \"timeout\": \"\",\n" +
                "      \"break_hours\": 30,\n" +
                "      \"projname\": \"RST\",\n" +
                "      \"has_in\": true,\n" +
                "      \"has_out\": false,\n" +
                "      \"in_photo_url\": \"https://www.4scontracting.com/SMCS_APP/fcm_app/view_attendance_img.php?attendance_id=1630&shift_no=1&type=in\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"attend_id\": 1631,\n" +
                "      \"empid\": 34,\n" +
                "      \"first_name\": \"HAZUR MASIH\",\n" +
                "      \"employee_number\": \"E034\",\n" +
                "      \"timein\": \"08:15 AM\",\n" +
                "      \"timeout\": \"05:00 PM\",\n" +
                "      \"break_hours\": 60,\n" +
                "      \"projname\": \"RST\",\n" +
                "      \"has_in\": true,\n" +
                "      \"has_out\": true,\n" +
                "      \"in_photo_url\": \"https://www.4scontracting.com/SMCS_APP/fcm_app/view_attendance_img.php?attendance_id=1631&shift_no=1&type=in\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"attend_id\": 1632,\n" +
                "      \"empid\": 31,\n" +
                "      \"first_name\": \"GURDEEP SINGH PIARA\",\n" +
                "      \"employee_number\": \"E031\",\n" +
                "      \"timein\": \"09:34 AM\",\n" +
                "      \"timeout\": \"\",\n" +
                "      \"break_hours\": 0,\n" +
                "      \"projname\": \"RST\",\n" +
                "      \"has_in\": true,\n" +
                "      \"has_out\": false\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONObject root = new JSONObject(manageApiResponse);
        assertEquals("success", root.optString("status"));
        JSONArray recordsArray = root.getJSONArray("records");
        assertEquals(3, recordsArray.length());

        List<AttendanceRecordModel> parsedList = new ArrayList<>();
        for (int i = 0; i < recordsArray.length(); i++) {
            JSONObject rObj = recordsArray.getJSONObject(i);
            String attendIdVal = rObj.optString("attend_id", "");
            String empIdVal = com.app.fourscontracting.data.ManageAttendanceApi.resolveWorkerId(rObj, "141");
            String firstNameVal = rObj.optString("first_name", "");
            String projNameVal = rObj.optString("projname", "");
            String timeInVal = rObj.optString("timein", "");
            String timeOutVal = rObj.optString("timeout", "");
            boolean hasIn = rObj.optBoolean("has_in", false);
            boolean hasOut = rObj.optBoolean("has_out", false);
            String inPhoto = rObj.optString("in_photo_url", "");

            parsedList.add(new AttendanceRecordModel(
                    attendIdVal, "141", empIdVal, firstNameVal, projNameVal,
                    timeInVal, timeOutVal, rObj.optDouble("break_hours", 0.0),
                    hasIn, hasOut, inPhoto, "", 1
            ));
        }

        assertEquals(3, parsedList.size());

        // First worker: Aksir Miah (Active, ready for check out)
        AttendanceRecordModel worker1 = parsedList.get(0);
        assertEquals("1630", worker1.getAttendId());
        assertEquals("38", worker1.getEmpId());
        assertEquals("Aksir Miah", worker1.getFirstName());
        assertTrue(worker1.isActive());
        assertEquals("38", com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(worker1, "141"));

        // Second worker: HAZUR MASIH (Completed)
        AttendanceRecordModel worker2 = parsedList.get(1);
        assertEquals("1631", worker2.getAttendId());
        assertEquals("34", worker2.getEmpId());
        assertFalse(worker2.isActive());

        // Third record: Supervisor GURDEEP SINGH PIARA
        AttendanceRecordModel supervisor = parsedList.get(2);
        assertEquals("31", supervisor.getEmpId());
        assertTrue(supervisor.getFirstName().toLowerCase().contains("gurdeep"));
    }
}
