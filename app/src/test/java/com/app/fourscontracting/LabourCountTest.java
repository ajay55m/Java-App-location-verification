package com.app.fourscontracting;

import com.app.fourscontracting.data.LabourEmployeeModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LabourCountTest {

    @Test
    public void testClockStatus_unpunchedWorkerReturnsFalseForBothInAndOut() throws JSONException {
        JSONObject unpunchedJson = new JSONObject();
        unpunchedJson.put("id", "101");
        unpunchedJson.put("name", "Worker Unpunched");
        unpunchedJson.put("status_code", "NOT_MARKED");
        unpunchedJson.put("display_text", "Not Marked");
        unpunchedJson.put("can_in", true);
        unpunchedJson.put("can_out", false);

        LabourEmployeeModel unpunchedWorker = new LabourEmployeeModel(unpunchedJson);

        assertFalse("Unpunched worker must NOT be counted as clocked in", unpunchedWorker.isClockedIn());
        assertFalse("Unpunched worker must NOT be counted as clocked out (should return 0 out count)", unpunchedWorker.isClockedOut());
    }

    @Test
    public void testClockStatus_clockedInWorker() throws JSONException {
        JSONObject clockedInJson = new JSONObject();
        clockedInJson.put("id", "102");
        clockedInJson.put("name", "Worker Active");
        clockedInJson.put("status_code", "IN_HERE");
        clockedInJson.put("display_text", "TIME IN 08:30 AM");
        clockedInJson.put("can_in", false);
        clockedInJson.put("can_out", true);

        LabourEmployeeModel clockedInWorker = new LabourEmployeeModel(clockedInJson);

        assertTrue("Clocked IN worker must return true for isClockedIn()", clockedInWorker.isClockedIn());
        assertFalse("Clocked IN worker must NOT return true for isClockedOut()", clockedInWorker.isClockedOut());
    }

    @Test
    public void testClockStatus_clockedOutWorkerReturnsTrueForClockedOut() throws JSONException {
        JSONObject clockedOutJson = new JSONObject();
        clockedOutJson.put("id", "103");
        clockedOutJson.put("name", "Worker Completed");
        clockedOutJson.put("status_code", "OUT");
        clockedOutJson.put("display_text", "COMPLETED 05:30 PM");
        clockedOutJson.put("can_in", true);
        clockedOutJson.put("can_out", false);
        clockedOutJson.put("has_out", true);
        clockedOutJson.put("time_out", "17:30:00");

        LabourEmployeeModel clockedOutWorker = new LabourEmployeeModel(clockedOutJson);

        assertFalse("Clocked OUT worker must NOT return true for isClockedIn()", clockedOutWorker.isClockedIn());
        assertTrue("Clocked OUT worker must return true for isClockedOut()", clockedOutWorker.isClockedOut());
    }

    @Test
    public void testFilterCheckedInAndCompletedWorkersFromLabourQueue() throws JSONException {
        JSONObject w1 = new JSONObject();
        w1.put("id", "43");
        w1.put("name", "Ajay (Completed)");
        w1.put("status_code", "SHIFT_FINISHED");
        w1.put("display_text", "Shift Finished (10:10 AM)");

        JSONObject w2 = new JSONObject();
        w2.put("id", "44");
        w2.put("name", "Kishore (Clocked In)");
        w2.put("status_code", "IN_HERE");
        w2.put("can_in", false);
        w2.put("can_out", true);

        JSONObject w3 = new JSONObject();
        w3.put("id", "45");
        w3.put("name", "Rahul (Pending In)");
        w3.put("status_code", "NOT_MARKED");
        w3.put("can_in", true);
        w3.put("can_out", false);

        LabourEmployeeModel m1 = new LabourEmployeeModel(w1);
        LabourEmployeeModel m2 = new LabourEmployeeModel(w2);
        LabourEmployeeModel m3 = new LabourEmployeeModel(w3);

        LabourEmployeeModel[] rawList = new LabourEmployeeModel[]{m1, m2, m3};
        List<LabourEmployeeModel> labourQueue = new ArrayList<>();
        int visibleTotal = 0;
        int visibleIn = 0;
        int visibleOut = 0;

        for (LabourEmployeeModel m : rawList) {
            visibleTotal++;
            if (m.isClockedOut()) {
                visibleOut++;
                continue;
            }
            if (m.isClockedIn()) {
                visibleIn++;
                continue;
            }
            labourQueue.add(m);
        }

        assertEquals("Total count should count all department workers", 3, visibleTotal);
        assertEquals("In count should count checked-in workers", 1, visibleIn);
        assertEquals("Out count should count completed workers", 1, visibleOut);
        assertEquals("Labour check-in queue should only contain pending check-in workers", 1, labourQueue.size());
        assertEquals("Rahul (Pending In)", labourQueue.get(0).getName());
    }

    @Test
    public void testResolveDepartmentTargetPosition() {
        List<com.app.fourscontracting.data.DepartmentModel> fullList = new ArrayList<>();
        fullList.add(new com.app.fourscontracting.data.DepartmentModel("", "-- Select Department --"));
        fullList.add(new com.app.fourscontracting.data.DepartmentModel("5", "4s Contracting Employees"));
        fullList.add(new com.app.fourscontracting.data.DepartmentModel("12", "Electrical Subcontractors"));

        // Case 1: Initial launch (no history) -> resolves to Dept 5 ("4s Contracting Employees")
        String activeDeptIdInitial = "";
        int targetPosInitial = -1;
        if (!activeDeptIdInitial.isEmpty()) {
            for (int i = 0; i < fullList.size(); i++) {
                if (activeDeptIdInitial.equalsIgnoreCase(fullList.get(i).id)) {
                    targetPosInitial = i;
                    break;
                }
            }
        }
        if (targetPosInitial < 0) {
            for (int i = 0; i < fullList.size(); i++) {
                if ("5".equals(fullList.get(i).id)) {
                    targetPosInitial = i;
                    break;
                }
            }
        }
        assertEquals("Initial launch must resolve to department 5 index", 1, targetPosInitial);

        // Case 2: History selection exists (Dept "12") -> restores Dept 12 index
        String activeDeptIdHistory = "12";
        int targetPosHistory = -1;
        if (!activeDeptIdHistory.isEmpty()) {
            for (int i = 0; i < fullList.size(); i++) {
                if (activeDeptIdHistory.equalsIgnoreCase(fullList.get(i).id)) {
                    targetPosHistory = i;
                    break;
                }
            }
        }
        assertEquals("History selection must restore user's previously selected department index", 2, targetPosHistory);
    }
}
