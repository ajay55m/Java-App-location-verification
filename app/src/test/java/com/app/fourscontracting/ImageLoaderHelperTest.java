package com.app.fourscontracting;

import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.LabourEmployeeModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for ImageLoaderHelper URL sanitization, PHP photo stream handling,
 * and JSON employee response parsing.
 */
public class ImageLoaderHelperTest {

    @Test
    public void testSanitizeUrl_httpToHttps() {
        String raw = "http://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14";
        String sanitized = ImageLoaderHelper.sanitizeUrl(raw);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14", sanitized);
    }

    @Test
    public void testSanitizeUrl_relativeUrlWithLeadingSlash() {
        String raw = "/SMCS_APP/subcontractor/get_photo.php?id=14";
        String sanitized = ImageLoaderHelper.sanitizeUrl(raw);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14", sanitized);
    }

    @Test
    public void testSanitizeUrl_relativeUrlWithoutLeadingSlash() {
        String raw = "get_photo.php?id=14";
        String sanitized = ImageLoaderHelper.sanitizeUrl(raw);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14", sanitized);
    }

    @Test
    public void testSanitizeUrl_spacesHandling() {
        String raw = "https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14 &name=MOHAN";
        String sanitized = ImageLoaderHelper.sanitizeUrl(raw);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14%20&name=MOHAN", sanitized);
    }

    @Test
    public void testSanitizeUrl_nullAndEmpty() {
        assertNull(ImageLoaderHelper.sanitizeUrl(null));
        assertNull(ImageLoaderHelper.sanitizeUrl(""));
        assertNull(ImageLoaderHelper.sanitizeUrl("   "));
        assertNull(ImageLoaderHelper.sanitizeUrl("null"));
        assertNull(ImageLoaderHelper.sanitizeUrl("NULL"));
    }

    @Test
    public void testBuildImageRequestHeaders_includesApiKey() {
        Map<String, String> headers = ImageLoaderHelper.buildImageRequestHeaders(null);
        assertNotNull(headers);
        assertEquals("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36", headers.get("User-Agent"));
        assertEquals("image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8", headers.get("Accept"));
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/", headers.get("Referer"));
        assertEquals("4S_Secure_Access_Token_2024_#$", headers.get("x-api-key"));
    }

    @Test
    public void testBuildImageAuthRetryHeaders_includesApiKey() {
        Map<String, String> headers = ImageLoaderHelper.buildImageAuthRetryHeaders(null);
        assertNotNull(headers);
        assertEquals("4S_Secure_Access_Token_2024_#$", headers.get("x-api-key"));
        assertEquals("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36", headers.get("User-Agent"));
    }

    @Test
    public void testParseUserEmployeeJsonResponse() throws Exception {
        String jsonPayload = "{\n" +
                "    \"stats\": {\n" +
                "        \"total_staff\": 17,\n" +
                "        \"present_here\": 0,\n" +
                "        \"absent\": 17\n" +
                "    },\n" +
                "    \"employees\": [\n" +
                "        {\n" +
                "            \"id\": \"14\",\n" +
                "            \"name\": \"MOHAN\",\n" +
                "            \"status_code\": \"OFF_DUTY\",\n" +
                "            \"display_text\": \"Not Clocked In\",\n" +
                "            \"can_in\": true,\n" +
                "            \"can_out\": false,\n" +
                "            \"site_name\": \"\",\n" +
                "            \"photo\": \"https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": \"15\",\n" +
                "            \"name\": \"MOHAMMED\",\n" +
                "            \"status_code\": \"OFF_DUTY\",\n" +
                "            \"display_text\": \"Not Clocked In\",\n" +
                "            \"can_in\": true,\n" +
                "            \"can_out\": false,\n" +
                "            \"site_name\": \"\",\n" +
                "            \"photo\": \"https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=15\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        JSONObject response = new JSONObject(jsonPayload);
        JSONArray arr = response.getJSONArray("employees");
        assertEquals(2, arr.length());

        List<LabourEmployeeModel> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            list.add(new LabourEmployeeModel(obj));
        }

        assertEquals("14", list.get(0).getId());
        assertEquals("MOHAN", list.get(0).getName());
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=14", list.get(0).getPhotoUrl());

        assertEquals("15", list.get(1).getId());
        assertEquals("MOHAMMED", list.get(1).getName());
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/get_photo.php?id=15", list.get(1).getPhotoUrl());

        // Verify ImageLoaderHelper sanitizeUrl processes both URLs cleanly
        for (LabourEmployeeModel model : list) {
            String sanitized = ImageLoaderHelper.sanitizeUrl(model.getPhotoUrl());
            assertNotNull(sanitized);
            assertTrue(sanitized.startsWith("https://"));
            assertTrue(sanitized.contains("get_photo.php?id="));
        }
    }

    @Test
    public void testSanitizeUrl_moveInAndMoveOutEndpoint() {
        String rawMoveIn = "view_attendance_img.php?move_id=60&type=in";
        String sanitizedIn = ImageLoaderHelper.sanitizeUrl(rawMoveIn);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/view_attendance_img.php?move_id=60&type=in&source=hrms", sanitizedIn);

        String rawMoveOut = "view_attendance_img.php?move_id=60&type=out";
        String sanitizedOut = ImageLoaderHelper.sanitizeUrl(rawMoveOut);
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/view_attendance_img.php?move_id=60&type=out&source=hrms", sanitizedOut);
    }

    @Test
    public void testMoveRecordModel_InAndOutPhotoUrlResolution() {
        // Case 1: Direct photo URLs provided
        com.app.fourscontracting.data.MoveRecordModel directRecord = new com.app.fourscontracting.data.MoveRecordModel(
                "60", "301", "Ajay", "Site A", "09:00 AM", "05:00 PM", true, true,
                "https://4scontracting.com/uploads/move_in_60.jpg",
                "https://4scontracting.com/uploads/move_out_60.jpg"
        );
        assertEquals("https://4scontracting.com/uploads/move_in_60.jpg", directRecord.getInPhotoUrl());
        assertEquals("https://4scontracting.com/uploads/move_out_60.jpg", directRecord.getOutPhotoUrl());

        // Case 2: Fallback photo URLs when empty
        com.app.fourscontracting.data.MoveRecordModel fallbackRecord = new com.app.fourscontracting.data.MoveRecordModel(
                "75", "302", "Worker", "Site B", "08:30 AM", "04:30 PM", true, true,
                "", ""
        );
        assertEquals("view_attendance_img.php?move_id=75&type=in&source=hrms", fallbackRecord.getInPhotoUrl());
        assertEquals("view_attendance_img.php?move_id=75&type=out&source=hrms", fallbackRecord.getOutPhotoUrl());

        // Sanitized versions of fallback URLs
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/view_attendance_img.php?move_id=75&type=in&source=hrms", ImageLoaderHelper.sanitizeUrl(fallbackRecord.getInPhotoUrl()));
        assertEquals("https://4scontracting.com/SMCS_APP/subcontractor/view_attendance_img.php?move_id=75&type=out&source=hrms", ImageLoaderHelper.sanitizeUrl(fallbackRecord.getOutPhotoUrl()));
    }

    @Test
    public void testMoveRecordModel_NullAndZeroPhotoUrlHandling() {
        com.app.fourscontracting.data.MoveRecordModel recordZeroNull = new com.app.fourscontracting.data.MoveRecordModel(
                "80", "303", "Worker Null", "Site C", "08:30 AM", "04:30 PM", true, true,
                "0", "null"
        );
        assertEquals("view_attendance_img.php?move_id=80&type=in&source=hrms", recordZeroNull.getInPhotoUrl());
        assertEquals("view_attendance_img.php?move_id=80&type=out&source=hrms", recordZeroNull.getOutPhotoUrl());
    }
}
