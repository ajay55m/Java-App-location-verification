package com.app.fourscontracting;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginAllocationTest {

    @Test
    public void testPostmanLiveLoginResponseDefaultsToNoAllocation() {
        // Exact JSON returned from live subadminlogin.php in the Postman screenshot
        String postmanJson = "{\n" +
                "    \"status\": \"success\",\n" +
                "    \"uid\": \"141\",\n" +
                "    \"flag\": \"1\",\n" +
                "    \"username\": \"gurdeepsinghpiara\",\n" +
                "    \"email\": \"\",\n" +
                "    \"staffid\": \"31\",\n" +
                "    \"userrole\": \"employee\",\n" +
                "    \"start_lat_lng\": \"0.0,0.0\",\n" +
                "    \"end_lat_lng\": \"0.0,0.0\"\n" +
                "}";

        LoginResponse res = LoginResponse.fromJson(postmanJson);
        assertEquals("success", res.status);
        assertEquals("141", res.uid);
        assertEquals("1", res.flag);
        assertEquals("gurdeepsinghpiara", res.username);
        assertEquals("31", res.staffid);
        assertEquals("employee", res.userrole);
        assertEquals("0.0,0.0", res.start_lat_lng);
        assertEquals("0.0,0.0", res.end_lat_lng);
        // DB has no allocation for this user, so it defaults to "0"
        assertEquals("0", res.has_project_allocation);

        User user = new User(postmanJson);
        assertEquals("141", user.uid);
        assertEquals("31", user.staffId);
        assertEquals("employee", user.userRole);
        assertEquals("0", user.hasProjectAllocation);

        assertTrue(UserLocalStore.isLoginResponseSuccess(postmanJson));
    }

    @Test
    public void testExplicitHasProjectAllocationZero() {
        String json = "{\n" +
                "    \"status\": \"success\",\n" +
                "    \"uid\": \"141\",\n" +
                "    \"flag\": \"1\",\n" +
                "    \"username\": \"gurdeepsinghpiara\",\n" +
                "    \"email\": \"\",\n" +
                "    \"staffid\": \"31\",\n" +
                "    \"userrole\": \"employee\",\n" +
                "    \"has_project_allocation\": \"0\",\n" +
                "    \"start_lat_lng\": \"0.0,0.0\",\n" +
                "    \"end_lat_lng\": \"0.0,0.0\"\n" +
                "}";

        LoginResponse res = LoginResponse.fromJson(json);
        assertEquals("0", res.has_project_allocation);

        User user = new User(json);
        assertEquals("0", user.hasProjectAllocation);
    }

    @Test
    public void testExplicitHasProjectAllocationOne() {
        String json = "{\n" +
                "    \"status\": \"success\",\n" +
                "    \"uid\": \"141\",\n" +
                "    \"flag\": \"1\",\n" +
                "    \"username\": \"gurdeepsinghpiara\",\n" +
                "    \"email\": \"\",\n" +
                "    \"staffid\": \"31\",\n" +
                "    \"userrole\": \"employee\",\n" +
                "    \"has_project_allocation\": \"1\",\n" +
                "    \"start_lat_lng\": \"0.0,0.0\",\n" +
                "    \"end_lat_lng\": \"0.0,0.0\"\n" +
                "}";

        LoginResponse res = LoginResponse.fromJson(json);
        assertEquals("1", res.has_project_allocation);

        User user = new User(json);
        assertEquals("1", user.hasProjectAllocation);
    }
}

