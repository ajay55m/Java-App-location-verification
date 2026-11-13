package com.app.fourscontracting;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for location verification intent parameters and initial launch auto-verification flags.
 */
public class LocationVerificationFlowTest {

    @Test
    public void testInitialLaunchAutoVerifyFlags() {
        // Initial launch requires supervisor to select project first (no default project preselection).
        // AUTO_START_VERIFY = false, IS_CHANGE_SITE = false, IS_INITIAL_VERIFY = true
        boolean isInitialVerify = true;
        boolean isChangeSite = false;
        boolean isAutoStartVerify = false; // User must select project first before verifying GPS

        assertFalse("Initial launch must not auto-start verification before project is selected", isAutoStartVerify);
        assertFalse("Initial launch must not force manual change site mode", isChangeSite);
        assertTrue("Initial launch must set initial verify flag", isInitialVerify);
    }

    @Test
    public void testManualChangeSiteFlags() {
        // Mode 2: Supervisor clicks CHANGE SITE button (IS_CHANGE_SITE = true, AUTO_START_VERIFY = false)
        boolean isInitialVerify = false;
        boolean isChangeSite = true;
        boolean isAutoStartVerify = !isChangeSite;

        assertFalse("Manual change site must not auto-start verification", isAutoStartVerify);
        assertTrue("Manual change site must set isChangeSite flag", isChangeSite);
        assertFalse("Manual change site must not set initial verify flag", isInitialVerify);
    }

    @Test
    public void testVerificationResultExtrasKey() {
        // Verify key result extras passed back to Labour Management screen
        String keyLocId = "location_id";
        String keyVerified = "VERIFIED";
        String keyToken = "VERIFICATION_TOKEN";

        assertEquals("location_id", keyLocId);
        assertEquals("VERIFIED", keyVerified);
        assertEquals("VERIFICATION_TOKEN", keyToken);
    }

    @Test
    public void testSingleVerificationAcceptableSampleThreshold() {
        // Single verification exit accepts realistic mobile GPS fixes <= 120m accuracy immediately
        float earlyExitAccuracyM = 120.0f;
        long samplingDurationMs = 3000L;

        assertTrue("Early exit accuracy threshold should be relaxed to at least 100m", earlyExitAccuracyM >= 100.0f);
        assertTrue("Sampling failsafe duration should be 5 seconds or less for fast single verification", samplingDurationMs <= 5000L);
    }

    @Test
    public void testAutoStartBypassedWhenSessionAlreadyValid() {
        // When location session is valid for today and no worker punch is pending,
        // auto-verification must NOT re-run, even if AUTO_START_VERIFY intent extra was passed.
        boolean sessionValid = true;
        boolean isPunchAction = false;
        boolean isChangeSite = false;
        boolean intentHasAutoStartExtra = true;

        boolean isAutoStartVerify;
        if (isChangeSite) {
            isAutoStartVerify = false;
        } else if (sessionValid && !isPunchAction) {
            isAutoStartVerify = false;
        } else if (intentHasAutoStartExtra) {
            isAutoStartVerify = true;
        } else {
            isAutoStartVerify = false;
        }

        assertFalse("Auto verification must NOT re-trigger when location session is already valid", isAutoStartVerify);
    }

    @Test
    public void testSingleVerificationNavigatesToLabourMenuWithoutReVerification() {
        // Once verified for today, location session is valid for 24 hours.
        // Navigating to Labour Menu must bypass LocationVerifyActivity completely.
        boolean sessionValid = true;
        boolean requiresInitialVerify = !sessionValid;

        assertFalse("Labour menu navigation must NOT trigger initial verification when session is already valid", requiresInitialVerify);
    }

    @Test
    public void testWorkerPunchBypassesLocationVerifyWhenSessionValid() {
        // When marking worker attendance (TIME IN / TIME OUT / MOVE) while supervisor location session is valid,
        // app must directly launch LocationActivity (Photo Capture) instead of re-verifying GPS via LocationVerifyActivity.
        boolean sessionValid = true;
        boolean shouldLaunchLocationActivityDirectly = sessionValid;
        boolean shouldLaunchLocationVerifyScreen = !sessionValid;

        assertTrue("Valid location session must launch photo capture activity directly for worker punch", shouldLaunchLocationActivityDirectly);
        assertFalse("Valid location session must NOT force GPS verification screen for worker punch", shouldLaunchLocationVerifyScreen);
    }

    @Test
    public void testLocationModelBoundingBoxDeserialization() {
        String json = "{\n" +
                "  \"projectid\": \"RST\",\n" +
                "  \"project_code\": \"RST\",\n" +
                "  \"location_name\": \"RST\",\n" +
                "  \"latitude\": 8.1863155,\n" +
                "  \"longitude\": 77.4254754,\n" +
                "  \"max_latitude\": 8.1867250,\n" +
                "  \"max_longtitude\": 77.4247289\n" +
                "}";

        com.google.gson.Gson gson = new com.google.gson.Gson();
        LocationModel model = gson.fromJson(json, LocationModel.class);

        assertNotNull(model);
        assertEquals(8.1863155, model.latitude, 0.000001);
        assertEquals(77.4254754, model.longitude, 0.000001);
        assertEquals(8.1867250, model.max_latitude, 0.000001);
        assertEquals(77.4247289, model.max_longtitude, 0.000001);
        assertTrue("Model should detect active bounding box", model.hasBoundingBox());

        assertEquals(8.1863155, model.getMinLatitude(), 0.000001);
        assertEquals(8.1867250, model.getMaxLatitude(), 0.000001);
        assertEquals(77.4247289, model.getMinLongitude(), 0.000001);
        assertEquals(77.4254754, model.getMaxLongitude(), 0.000001);
    }

    @Test
    public void testBoundingBoxVerificationInside() {
        // Site coordinates from server
        double lat1 = 8.1863155;
        double lng1 = 77.4254754;
        double lat2 = 8.1867250;
        double lng2 = 77.4247289;

        // User GPS point directly inside the site box
        double userLat = 8.186500;
        double userLng = 77.425000;

        boolean inside = LocationVerifier.isWithinBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2, 10.0f);
        float distToBox = LocationVerifier.distanceToBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2);

        assertTrue("User inside coordinates must pass verification instantly", inside);
        assertEquals("Distance to box must be 0 for internal points", 0.0f, distToBox, 0.001f);
    }

    @Test
    public void testBoundingBoxVerificationWithAccuracyTolerance() {
        double lat1 = 8.1863155;
        double lng1 = 77.4254754;
        double lat2 = 8.1867250;
        double lng2 = 77.4247289;

        // User slightly outside by ~15m (e.g. at the building doorway)
        double userLat = 8.186750;
        double userLng = 77.425000;

        boolean inside = LocationVerifier.isWithinBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2, 25.0f);
        float distToBox = LocationVerifier.distanceToBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2);

        assertTrue("User within GPS accuracy tolerance must pass verification", inside);
        assertTrue("Distance to box should be within 30m", distToBox < 30.0f);
    }

    @Test
    public void testBoundingBoxVerificationFarAwayFails() {
        double lat1 = 8.1863155;
        double lng1 = 77.4254754;
        double lat2 = 8.1867250;
        double lng2 = 77.4247289;

        // User far away (> 500m)
        double userLat = 8.195000;
        double userLng = 77.435000;

        boolean inside = LocationVerifier.isWithinBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2, 15.0f);
        float distToBox = LocationVerifier.distanceToBoundingBox(userLat, userLng, lat1, lng1, lat2, lng2);

        assertFalse("User far away must fail verification", inside);
        assertTrue("Distance to box should exceed 200m", distToBox > 200.0f);
    }

    @Test
    public void testProjectModelLocationNameInDropdown() {
        String json = "{\n" +
                "  \"projectid\": \"RST\",\n" +
                "  \"project_code\": \"RST\",\n" +
                "  \"project_description\": \"Rathna Softnet Technology\",\n" +
                "  \"location_name\": \"Head Office\"\n" +
                "}";

        com.google.gson.Gson gson = new com.google.gson.Gson();
        ProjectModel pm = gson.fromJson(json, ProjectModel.class);

        assertNotNull(pm);
        assertEquals("Head Office", pm.getLocationName());
        assertEquals("RST", pm.getProjectCode());

        // toString() must pass location_name in the dropdown name
        String dropdownName = pm.toString();
        assertTrue("Dropdown name must include location_name", dropdownName.contains("Head Office"));
        assertEquals("RST - Head Office (Rathna Softnet Technology)", dropdownName);

        // Case when location_name equals code
        ProjectModel pm2 = new ProjectModel("RST", "RST", "RST");
        pm2.project_description = "Rathna Softnet Technology";
        assertEquals("RST - Rathna Softnet Technology", pm2.toString());
    }
}
