package com.app.fourscontracting;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AndroidBridge {
    private static final int REQUEST_CAPTURE_PHOTO = 1002;

    private final Activity activity;
    private final WebView webView;
    private final UserLocalStore userLocalStore;
    private final String currentUid;

    public AndroidBridge(Activity activity, WebView webView, UserLocalStore userLocalStore, String currentUid) {
        this.activity = activity;
        this.webView = webView;
        this.userLocalStore = userLocalStore;
        this.currentUid = currentUid;
    }

    @JavascriptInterface
    public void performClick(final String id, final String type) {
        performClick(id, type, "");
    }

    @JavascriptInterface
    public void performClick(final String id, final String type, final String locId) {
        performClick(id, type, locId, "");
    }

    @JavascriptInterface
    public void performClick(final String id, final String type, final String locId, final String empName) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        if ("VERIFY_LOCATION".equalsIgnoreCase(type)) {
            final String uid = currentUid != null && !currentUid.isEmpty() ? currentUid : getStoredUid();
            activity.runOnUiThread(() -> {
                Intent intent = new Intent(activity, LocationVerifyActivity.class);
                intent.putExtra("IS_INITIAL_VERIFY", true);
                intent.putExtra("uid", uid);
                activity.startActivity(intent);
            });
            return;
        }

        if ("IN".equalsIgnoreCase(type) || "OUT".equalsIgnoreCase(type)) {
            final String action = type.toUpperCase();
            final String empId = id;
            final String uid = currentUid != null && !currentUid.isEmpty() ? currentUid : getStoredUid();

            if (empId == null || empId.isEmpty()) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Employee selection is missing", Toast.LENGTH_SHORT).show());
                return;
            }

            if (uid.isEmpty()) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, AppMessages.USER_SESSION_MISSING, Toast.LENGTH_SHORT).show());
                return;
            }

            String projName = "";
            String projectId = "";
            if (userLocalStore != null) {
                UserProject up = userLocalStore.getLoggedInUserProject();
                if (up != null) {
                    projName = up.projectname;
                }
                projectId = userLocalStore.getLoggedInUserProjectID();
            }

            final String locationId = (locId != null && !locId.trim().isEmpty()) ? locId :
                    new SessionPrefs(activity).getLocationId();

            if (projectId == null || projectId.isEmpty()) {
                projectId = new SessionPrefs(activity).getProjectId();
            }
            if (projName == null || projName.isEmpty()) {
                projName = new SessionPrefs(activity).getProjectName();
            }

            final String finalProjName = projName;
            final String finalProjectId = projectId;
            activity.runOnUiThread(() -> {
                Intent intent = new Intent(activity, LocationVerifyActivity.class);
                intent.putExtra("empid", empId);
                if (empName != null && !empName.isEmpty()) {
                    intent.putExtra("emp_name", empName);
                }
                intent.putExtra("type", action);
                intent.putExtra("uid", uid);
                intent.putExtra("projname", finalProjName);
                intent.putExtra("selected_project_id", finalProjectId);
                intent.putExtra("AUTO_START_VERIFY", true);
                intent.putExtra("target_location_id", locationId);
                intent.putExtra("pending_eid", empId);
                intent.putExtra("pending_action", action);
                activity.startActivity(intent);
            });
            return;
        }

        if ("MOVE".equalsIgnoreCase(type)) {
            final String empId = id;
            final String uid = currentUid != null && !currentUid.isEmpty() ? currentUid : getStoredUid();
            if (empId == null || empId.isEmpty()) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Employee selection is missing", Toast.LENGTH_SHORT).show());
                return;
            }
            if (uid.isEmpty()) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, AppMessages.USER_SESSION_MISSING, Toast.LENGTH_SHORT).show());
                return;
            }

            SessionPrefs session = new SessionPrefs(activity);
            String projectId = userLocalStore != null ? userLocalStore.getLoggedInUserProjectID() : "";
            String projName = "";
            if (userLocalStore != null) {
                UserProject up = userLocalStore.getLoggedInUserProject();
                if (up != null) projName = up.projectname;
            }
            if (projectId == null || projectId.isEmpty()) projectId = session.getProjectId();
            if (projName == null || projName.isEmpty()) projName = session.getProjectName();
            final String finalProjectId = projectId;
            final String finalProjName = projName;

            activity.runOnUiThread(() -> {
                Intent intent = new Intent(activity, LocationVerifyActivity.class);
                intent.putExtra("empid", empId);
                if (empName != null && !empName.isEmpty()) {
                    intent.putExtra("emp_name", empName);
                }
                intent.putExtra("type", "MOVE");
                intent.putExtra("uid", uid);
                intent.putExtra("is_movement", true);
                intent.putExtra("selected_project_id", finalProjectId);
                intent.putExtra("projname", finalProjName);
                intent.putExtra("FROM_MOVE_MENU", true);
                intent.putExtra("AUTO_START_VERIFY", false);
                intent.putExtra("IS_INITIAL_VERIFY", false);
                activity.startActivity(intent);
            });
        }
    }

    public void handleCaptureResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CAPTURE_PHOTO) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            final String photoPath = data.getStringExtra("photo_path");
            final String eid = data.getStringExtra("eid");
            final String action = data.getStringExtra("action");
            final String uid = data.getStringExtra("uid");
            final String projectId = data.getStringExtra("project_id");
            final String locationId = data.getStringExtra("location_id");
            final double lat = data.getDoubleExtra("lat", 0.0);
            final double lng = data.getDoubleExtra("lng", 0.0);
            if (photoPath == null || photoPath.isEmpty()) {
                Toast.makeText(activity, "Photo capture failed", Toast.LENGTH_SHORT).show();
                return;
            }
            submitAttendance(eid, action, uid, projectId, locationId, lat, lng, photoPath);
        }
    }

    private void submitAttendance(final String eid, final String action, final String uid, final String projectId,
                                  final String locationId, final double lat, final double lng, final String photoPath) {
        new Thread(() -> {
            try {
                String boundary = "----AndroidBridgeBoundary";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                File file = new File(photoPath);
                if (!file.exists()) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Photo file missing", Toast.LENGTH_SHORT).show());
                    return;
                }

                String urlString = "https://4scontracting.com/SMCS_APP/subcontractor/submit_attendance.php";
                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    writeFormField(outputStream, boundary, "eid", eid);
                    writeFormField(outputStream, boundary, "action", action);
                    writeFormField(outputStream, boundary, "location_id", locationId);
                    writeFormField(outputStream, boundary, "lat", String.valueOf(lat));
                    writeFormField(outputStream, boundary, "lng", String.valueOf(lng));
                    writeFormField(outputStream, boundary, "uid", uid);
                    writeFormField(outputStream, boundary, "project_id", projectId);
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"attendance.jpg\"" + lineEnd);
                    outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                    outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd + lineEnd);
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = fileInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                    }
                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Attendance submitted", Toast.LENGTH_SHORT).show();
                        if (webView != null) {
                            webView.reload();
                        }
                    });
                } else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Attendance submission failed", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Unable to submit attendance", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void writeFormField(DataOutputStream outputStream, String boundary, String fieldName, String value) throws Exception {
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n");
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.writeBytes("\r\n");
    }

    private String getStoredUid() {
        if (userLocalStore == null) {
            return "";
        }
        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] valList = UserLocalStore.parseUserInfo(val);
        return valList.length > 0 ? valList[0] : "";
    }
}
