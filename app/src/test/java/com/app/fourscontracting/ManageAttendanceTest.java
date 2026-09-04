package com.app.fourscontracting;

import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.AttendanceRecordModel;
import com.app.fourscontracting.data.MoveRecordModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ManageAttendanceTest {

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
}
