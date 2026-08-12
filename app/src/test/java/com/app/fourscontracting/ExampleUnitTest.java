package com.app.fourscontracting;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for authentication response handling, credential validation,
 * and user data parsing.
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testProjectConstructor() {
        // Verify the Project constructor sets fields correctly
        Project p = new Project("123", "Kuwait Site 4S", "1.5");
        
        assertEquals("123", p.id);
        assertEquals("Kuwait Site 4S", p.name);
        assertEquals("1.5", p.breakHours);
    }

    @Test
    public void testParseUserInfoWithProjectsArray() throws Exception {
        String jsonResponse = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"uid\": \"05\",\n" +
                "  \"flag\": \"1\",\n" +
                "  \"username\": \"4S-Contracting\",\n" +
                "  \"projects\": [\n" +
                "    \"DF\",\n" +
                "    \"RST/001\"\n" +
                "  ],\n" +
                "  \"start_lat_lng\": \",\",\n" +
                "  \"end_lat_lng\": \",\"\n" +
                "}";

        String[] userInfo = UserLocalStore.parseUserInfo(jsonResponse);
        
        assertEquals("05", userInfo[0]); // uid
        assertEquals("1", userInfo[1]);  // role / flag
        assertEquals("4S-Contracting", userInfo[2]); // displayName / username
        assertEquals("DF,RST/001", userInfo[3]); // projects
    }

    @Test
    public void testBuildEmployeeDetailsUrl() {
        // Case 1: Simple parameters with no location
        String url1 = UserLocalStore.buildEmployeeDetailsUrl("85", "7", "7", null);
        assertTrue(url1.contains("uid=85"));
        assertTrue(url1.contains("projname=7"));

        // Case 2: Parameters with a location ID
        String url2 = UserLocalStore.buildEmployeeDetailsUrl("85", "7", "7", "8");
        assertTrue(url2.contains("uid=85"));
        assertTrue(url2.contains("location_id=8"));

        // Case 3: Empty location ID
        String url3 = UserLocalStore.buildEmployeeDetailsUrl("85", "7", "7", "");
        assertTrue(url3.contains("uid=85"));
        assertFalse(url3.contains("location_id="));

        // Case 4: Null values for uid or projname
        String url4 = UserLocalStore.buildEmployeeDetailsUrl(null, null, null, null);
        assertTrue(url4.contains("uid="));

        // Case 5: Passing a project name (e.g. "DF") and location ID
        String url5 = UserLocalStore.buildEmployeeDetailsUrl("85", "DF", "DF", "8");
        assertTrue(url5.contains("projname=DF"));
        assertTrue(url5.contains("location_id=8"));
    }

    @Test
    public void testAutoStartVerificationParameters() {
        // Assert that the intent payload values for employee action mapping are case-correct
        String action1 = "in".toUpperCase();
        assertEquals("IN", action1);
        
        String action2 = "out".toUpperCase();
        assertEquals("OUT", action2);

        String rawLocId = "12 ";
        String cleanedLocId = rawLocId.trim();
        assertEquals("12", cleanedLocId);
    }

    // --- Authentication & Credential Validation Tests ---

    @Test
    public void testInvalidCredentialsJsonResponse() {
        // Server response returned when credentials are bad
        String jsonErrorResponse = "{\"status\":\"error\",\"message\":\"Invalid credentials\"}";

        // Verify that isLoginResponseSuccess identifies this as a failed login
        boolean success = UserLocalStore.isLoginResponseSuccess(jsonErrorResponse);
        assertFalse("Invalid credentials response must not be treated as success", success);

        // Verify that getLoginErrorMessage extracts the error message correctly
        String errorMessage = UserLocalStore.getLoginErrorMessage(jsonErrorResponse);
        assertEquals("Invalid credentials", errorMessage);
    }

    @Test
    public void testLiteralErrorResponse() {
        String literalError = "error";

        boolean success = UserLocalStore.isLoginResponseSuccess(literalError);
        assertFalse("Literal 'error' string must fail validation", success);

        String errorMessage = UserLocalStore.getLoginErrorMessage(literalError);
        assertEquals("Invalid Username or Password", errorMessage);
    }

    @Test
    public void testNullOrEmptyResponse() {
        assertFalse(UserLocalStore.isLoginResponseSuccess(null));
        assertFalse(UserLocalStore.isLoginResponseSuccess(""));
        assertFalse(UserLocalStore.isLoginResponseSuccess("   "));

        assertEquals("Invalid Username or Password", UserLocalStore.getLoginErrorMessage(null));
        assertEquals("Invalid Username or Password", UserLocalStore.getLoginErrorMessage(""));
    }

    @Test
    public void testSuccessfulLoginJsonResponse() {
        String successJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"uid\": \"85\",\n" +
                "  \"flag\": \"1\",\n" +
                "  \"username\": \"4S-Contracting\",\n" +
                "  \"projects\": [\"DF\", \"RST/001\"]\n" +
                "}";

        boolean success = UserLocalStore.isLoginResponseSuccess(successJson);
        assertTrue("Valid JSON login response must be treated as success", success);
    }

    @Test
    public void testSuccessfulDelimitedLoginResponse() {
        String legacyResponse = "85##1##4S-Contracting##DF,RST/001";

        boolean success = UserLocalStore.isLoginResponseSuccess(legacyResponse);
        assertTrue("Legacy delimited string response must be treated as success", success);
    }
}