package com.app.fourscontracting;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.app.fourscontracting", appContext.getPackageName());
    }

    @Test
    public void testUpdateAssignedProjectsJson() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserLocalStore store = new UserLocalStore(appContext);

        // Arrange: Store initial dummy JSON user data
        String initialJson = "{\n" +
                "  \"uid\": \"85\",\n" +
                "  \"flag\": \"1\",\n" +
                "  \"username\": \"4S-Contracting\",\n" +
                "  \"projects\": [\n" +
                "    \"DF\"\n" +
                "  ]\n" +
                "}";
        store.storeUserData(new User(initialJson));

        // Act: Update assigned projects list dynamically
        store.updateAssignedProjects(Arrays.asList("DF", "RST/001", "NewProject"), appContext);

        // Assert: Parse and verify updated projects in UserLocalStore
        User updatedUser = store.getLoggedInUser();
        String[] userInfo = UserLocalStore.parseUserInfo(updatedUser.username);

        assertEquals("85", userInfo[0]); // uid
        assertEquals("1", userInfo[1]);  // role / flag
        assertEquals("4s-contracting", userInfo[2].toLowerCase()); // displayName
        assertEquals("DF,RST/001,NewProject", userInfo[3]); // projects list

        // Assert: Parse and verify updated projects in Default SharedPreferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext.getApplicationContext());
        String defaultPrefUsername = settings.getString("username", "");
        String[] defaultUserInfo = UserLocalStore.parseUserInfo(defaultPrefUsername);
        assertEquals("DF,RST/001,NewProject", defaultUserInfo[3]);
    }

    @Test
    public void testUpdateAssignedProjectsFallback() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserLocalStore store = new UserLocalStore(appContext);

        // Arrange: Store initial raw delimited text (## format)
        String initialRaw = "85##1##4S-Contracting##DF##extra1##extra2";
        store.storeUserData(new User(initialRaw));

        // Act: Update assigned projects list dynamically
        store.updateAssignedProjects(Arrays.asList("DF", "RST/001", "NewProject"), appContext);

        // Assert: Parse and verify updated projects in UserLocalStore
        User updatedUser = store.getLoggedInUser();
        String[] userInfo = UserLocalStore.parseUserInfo(updatedUser.username);

        assertEquals("85", userInfo[0]); // uid
        assertEquals("1", userInfo[1]);  // role / flag
        assertEquals("4s-contracting", userInfo[2].toLowerCase()); // displayName
        assertEquals("DF,RST/001,NewProject", userInfo[3]); // projects list

        // Assert: Parse and verify updated projects in Default SharedPreferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext.getApplicationContext());
        String defaultPrefUsername = settings.getString("username", "");
        String[] defaultUserInfo = UserLocalStore.parseUserInfo(defaultPrefUsername);
        assertEquals("DF,RST/001,NewProject", defaultUserInfo[3]);
    }
}