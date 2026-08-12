package com.app.fourscontracting;

import com.app.fourscontracting.data.AttendanceCodes;
import com.app.fourscontracting.data.AttendanceResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttendanceResultTest {

    @Test
    public void parseLegacySuccess() {
        AttendanceResult r = AttendanceResult.parse("success");
        assertTrue(r.success);
        assertEquals(AttendanceCodes.OK, r.code);
    }

    @Test
    public void parseLegacyError() {
        AttendanceResult r = AttendanceResult.parse("error");
        assertFalse(r.success);
        assertEquals(AttendanceCodes.ERROR, r.code);
    }

    @Test
    public void parseJsonOutsideGeofence() {
        AttendanceResult r = AttendanceResult.parse(
                "{\"status\":\"error\",\"code\":\"OUTSIDE_GEOFENCE\",\"message\":\"Too far from site\"}");
        assertFalse(r.success);
        assertEquals(AttendanceCodes.OUTSIDE_GEOFENCE, r.code);
        assertEquals("Too far from site", r.message);
    }

    @Test
    public void parseJsonOk() {
        AttendanceResult r = AttendanceResult.parse(
                "{\"status\":\"success\",\"code\":\"OK\",\"message\":\"Saved\"}");
        assertTrue(r.success);
        assertEquals(AttendanceCodes.OK, r.code);
    }
}
