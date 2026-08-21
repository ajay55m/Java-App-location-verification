package com.app.fourscontracting;

import es.dmoral.toasty.Toasty;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class LocationActivity extends AppActivity implements LocationListener {
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_LOCATION = 101; 
    AlertDialog alertDialog;
    private android.app.AlertDialog timeDialog;
    String app_server_url = com.app.fourscontracting.data.ApiConfig.MULTI_SAVE_TIME_IN;
    String userid, projectid;
    AlertDialog.Builder builderDialog;
    int hour;
    private ImageView imageView1;
    /* access modifiers changed from: private */
    public String imagepath;
    LoadingManager loadingManager;
    LocationManager locationManager;
    int minute;
    Button timeButton;
    UserLocalStore userLocalStore;
    String selectedProjectId = "";
    String employeeId = "";
    String attendanceType = "IN";
    private String isVerified = "false";
    String selectedLocationId = "";
    String managerUid = "";
    private boolean isMovement = false;
    private double punchLat = Double.NaN;
    private double punchLng = Double.NaN;
    private float punchAccuracy = -1f;
    private com.app.fourscontracting.ui.attendance.AttendanceViewModel attendanceViewModel;

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saved_is_verified", isVerified);
        outState.putString("saved_employee_id", employeeId);
        outState.putString("saved_selected_project_id", selectedProjectId);
        outState.putString("saved_attendance_type", attendanceType);
        outState.putString("saved_selected_location_id", selectedLocationId);
        outState.putString("saved_image_path", imagepath);
        outState.putString("saved_manager_uid", managerUid);
        outState.putBoolean("saved_is_movement", isMovement);
        outState.putDouble("saved_punch_lat", punchLat);
        outState.putDouble("saved_punch_lng", punchLng);
        outState.putFloat("saved_punch_accuracy", punchAccuracy);
    }

    private void getIncomingData() {
        try {
            Intent intent = getIntent();
            if (intent == null || intent.getExtras() == null) {
                return;
            }
            Bundle bundle = intent.getExtras();

            if (bundle.containsKey("VERIFIED")) {
                Object obj = bundle.get("VERIFIED");
                if (obj != null) isVerified = String.valueOf(obj);
            }

            if (bundle.containsKey("empid")) {
                Object obj = bundle.get("empid");
                if (obj != null) employeeId = String.valueOf(obj);
            }

            if (bundle.containsKey("departmentid")) {
                Object obj = bundle.get("departmentid");
                if (obj != null) selectedProjectId = String.valueOf(obj);
            }

            if (bundle.containsKey("locationid")) {
                Object obj = bundle.get("locationid");
                if (obj != null) selectedLocationId = String.valueOf(obj);
            } else if (bundle.containsKey("loc_id")) {
                Object obj = bundle.get("loc_id");
                if (obj != null) selectedLocationId = String.valueOf(obj);
            } else if (bundle.containsKey("MATCHED_LOC_ID")) {
                Object obj = bundle.get("MATCHED_LOC_ID");
                if (obj != null) selectedLocationId = String.valueOf(obj);
            }

            if (bundle.containsKey("uid")) {
                Object obj = bundle.get("uid");
                if (obj != null) managerUid = String.valueOf(obj);
            }

            if (bundle.containsKey("project_id")) {
                Object obj = bundle.get("project_id");
                if (obj != null) selectedProjectId = String.valueOf(obj);
            }

            if (bundle.containsKey("type")) {
                Object obj = bundle.get("type");
                if (obj != null) {
                    String rawType = String.valueOf(obj);
                    if (rawType.equalsIgnoreCase("checkout") || rawType.equalsIgnoreCase("out")) {
                        attendanceType = "OUT";
                    } else if (rawType.equalsIgnoreCase("move")) {
                        attendanceType = "IN";
                        isMovement = true;
                    } else {
                        attendanceType = "IN";
                    }
                }
            }

            if (bundle.containsKey("is_movement")) {
                isMovement = bundle.getBoolean("is_movement", isMovement);
            }
            if (intent.hasExtra("is_movement")) {
                isMovement = intent.getBooleanExtra("is_movement", isMovement);
            }

            if (bundle.containsKey("lat") || intent.hasExtra("lat")) {
                punchLat = intent.getDoubleExtra("lat", bundle.containsKey("lat") ? bundle.getDouble("lat", Double.NaN) : Double.NaN);
            }
            if (bundle.containsKey("lng") || intent.hasExtra("lng")) {
                punchLng = intent.getDoubleExtra("lng", bundle.containsKey("lng") ? bundle.getDouble("lng", Double.NaN) : Double.NaN);
            }
            if (bundle.containsKey("gps_accuracy") || intent.hasExtra("gps_accuracy")) {
                punchAccuracy = intent.getFloatExtra("gps_accuracy",
                        bundle.containsKey("gps_accuracy") ? bundle.getFloat("gps_accuracy", -1f) : -1f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isVerified = "false";
            employeeId = "";
            selectedProjectId = "";
            attendanceType = "IN";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_location);

        if (savedInstanceState != null) {
            isVerified = savedInstanceState.getString("saved_is_verified", "false");
            employeeId = savedInstanceState.getString("saved_employee_id", "");
            selectedProjectId = savedInstanceState.getString("saved_selected_project_id", "");
            attendanceType = savedInstanceState.getString("saved_attendance_type", "IN");
            selectedLocationId = savedInstanceState.getString("saved_selected_location_id", "");
            imagepath = savedInstanceState.getString("saved_image_path", "");
            managerUid = savedInstanceState.getString("saved_manager_uid", "");
            isMovement = savedInstanceState.getBoolean("saved_is_movement", false);
            punchLat = savedInstanceState.getDouble("saved_punch_lat", Double.NaN);
            punchLng = savedInstanceState.getDouble("saved_punch_lng", Double.NaN);
            punchAccuracy = savedInstanceState.getFloat("saved_punch_accuracy", -1f);
        } else {
            getIncomingData();
        }

        attendanceViewModel = new androidx.lifecycle.ViewModelProvider(this)
                .get(com.app.fourscontracting.ui.attendance.AttendanceViewModel.class);
        attendanceViewModel.getLastResult().observe(this, this::onAttendanceResult);

            if (!"true".equals(isVerified)) {
            Toast.makeText(this, AppMessages.LOCATION_NOT_VERIFIED, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Secure Verification Token & Session check
        String intentToken = getIntent().getStringExtra("VERIFICATION_TOKEN");
        SessionPrefs sessionPrefs = new SessionPrefs(this);
        if (!sessionPrefs.isVerificationTokenValid(intentToken)) {
            Toast.makeText(this, AppMessages.SESSION_EXPIRED, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Anti-Tampering Time Lock check
        checkAutoTimeOrPrompt();

        Button savebtnTextSetter = (Button) findViewById(R.id.saveattendance);
        if ("OUT".equals(attendanceType)) {
            savebtnTextSetter.setText("SUBMIT CHECK-OUT");
            savebtnTextSetter.setBackgroundColor(Color.RED);
        } else {
            savebtnTextSetter.setText("SUBMIT CHECK-IN");
            savebtnTextSetter.setBackgroundColor(Color.parseColor("#0EA5E9"));
        }

        TextView tvHeaderStatusBadge = findViewById(R.id.tv_header_status_badge);
        if (tvHeaderStatusBadge != null) {
            if ("OUT".equals(attendanceType)) {
                tvHeaderStatusBadge.setText("OUT");
                tvHeaderStatusBadge.setBackgroundResource(R.drawable.bg_luxury_status_out_badge);
            } else {
                tvHeaderStatusBadge.setText("IN");
                tvHeaderStatusBadge.setBackgroundResource(R.drawable.bg_luxury_status_in_badge);
            }
        }

        TextView tvStoryEmpId = findViewById(R.id.tv_story_emp_id);
        if (tvStoryEmpId != null && employeeId != null && !employeeId.isEmpty()) {
            tvStoryEmpId.setText("ID: #" + employeeId);
        }

        TextView txtStatusLabel = findViewById(R.id.txtStatusLabel);
        if (isMovement && txtStatusLabel != null) {
            txtStatusLabel.setText("Site Movement: Travel time will be added automatically.");
            txtStatusLabel.setTextColor(Color.parseColor("#6366F1")); // Indigo color for Movement
        }

        // Permissions are requested once on first app open (Splash). No popup here.
        this.loadingManager = new LoadingManager();
        this.imageView1 = (ImageView) findViewById(R.id.imageView1);
        
        // Restore bitmap from base64 if active process death was recovered
        if (imagepath != null && !imagepath.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imagepath, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    this.imageView1.setImageBitmap(decodedByte);
                    Button savebtn = (Button) findViewById(R.id.saveattendance);
                    if (savebtn != null) {
                        savebtn.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ((ImageButton) findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!AppPermissions.ensureOrGuide(LocationActivity.this, true, false)) {
                    return;
                }
                LocationActivity.this.startActivityForResult(new Intent("android.media.action.IMAGE_CAPTURE"), LocationActivity.CAMERA_REQUEST);
            }
        });
        UserLocalStore userLocalStore2 = new UserLocalStore(this);
        this.userLocalStore = userLocalStore2;
        SessionPrefs sessionPrefsForProject = new SessionPrefs(this);
        String resolvedProjectName = sessionPrefsForProject.getProjectName();
        if (resolvedProjectName.isEmpty()) {
            resolvedProjectName = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .getString("selected_project_name", "");
        }
        final String projname = resolvedProjectName;
        projectid = projname;
        String usernameRaw = "";
        if (this.userLocalStore.getLoggedInUser() != null
                && this.userLocalStore.getLoggedInUser().username != null) {
            usernameRaw = this.userLocalStore.getLoggedInUser().username;
        }
        String[] val_list = UserLocalStore.parseUserInfo(usernameRaw);
        final String uid = val_list.length > 0 ? val_list[0] : "";
        userid = uid;

        //saveUserInfo();

        // getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#16A116")));
        this.timeButton = (Button) findViewById(R.id.timeButton);
        this.timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LocationActivity.this, "Time is locked to the present live time.", Toast.LENGTH_SHORT).show();
            }
        });
        Calendar calender = Calendar.getInstance();
        calender.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
        LocationActivity.this.hour = calender.get(Calendar.HOUR_OF_DAY);
        LocationActivity.this.minute = calender.get(Calendar.MINUTE);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
        this.timeButton.setText(sdf.format(calender.getTime()));

        ((Button) findViewById(R.id.saveattendance)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Double check Auto Time Lock
                int autoTime = 0;
                try {
                    autoTime = android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.AUTO_TIME);
                } catch (Exception e) {
                    autoTime = 0;
                }

                if (autoTime == 0) {
                    Toast.makeText(LocationActivity.this, AppMessages.AUTO_TIME_REQUIRED, Toast.LENGTH_LONG).show();
                    return;
                }

                if (!AppPermissions.ensureOrGuide(LocationActivity.this, true, true)) {
                    return;
                }

                if (LocationActivity.this.loadingManager != null) {
                    LocationActivity.this.loadingManager.showLoading(LocationActivity.this);
                }

                final String currentUserId = (employeeId != null && !employeeId.isEmpty()) ? employeeId : ((managerUid != null && !managerUid.isEmpty()) ? managerUid : uid);
                RuntimeLogger.log(LocationActivity.this, currentUserId, "INFO", "Attendance", "User initiated attendance check-" + attendanceType + " for project: " + projname);

                // Fresh GPS for server-side geofence check; fall back to verify-time coordinates
                new LocationHelper().getCurrentLocation(LocationActivity.this, 8000, new LocationHelper.LocationCallback() {
                    @Override
                    public void onSuccess(Location location) {
                        punchLat = location.getLatitude();
                        punchLng = location.getLongitude();
                        punchAccuracy = location.getAccuracy();
                        submitAttendanceViaViewModel(uid, projname, currentUserId, location.getTime());
                    }

                    @Override
                    public void onError(String message) {
                        if (!Double.isNaN(punchLat) && !Double.isNaN(punchLng)) {
                            submitAttendanceViaViewModel(uid, projname, currentUserId, System.currentTimeMillis());
                        } else {
                            if (LocationActivity.this.loadingManager != null) {
                                LocationActivity.this.loadingManager.hideLoading();
                            }
                            Toast.makeText(LocationActivity.this, AppMessages.GPS_REQUIRED_SAVE, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavLoc = findViewById(R.id.bottom_nav_view_loc);
        if (bottomNavLoc != null) {
            android.view.Menu menu = bottomNavLoc.getMenu();
            if (menu.findItem(R.id.nav_profile) != null) {
                menu.findItem(R.id.nav_profile).setChecked(true);
            }
            bottomNavLoc.setOnNavigationItemSelectedListener(new com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
                    Intent intent = new Intent(LocationActivity.this, WebviewActivity.class);
                    intent.putExtra("destination_id", item.getItemId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
            });
        }
    }

    private void submitAttendanceViaViewModel(String uid, String projname, String currentUserId, long gpsTimeMs) {
        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String breakHoursVal = new SessionPrefs(this).getBreakHours();
        if (breakHoursVal.isEmpty()) {
            breakHoursVal = prefs.getString("selected_project_break_hours", "");
            if (breakHoursVal == null || "EMPTY_IN_PREFS".equals(breakHoursVal) || "null".equalsIgnoreCase(breakHoursVal)) {
                breakHoursVal = "";
            }
        }

        com.app.fourscontracting.data.AttendancePayload payload = new com.app.fourscontracting.data.AttendancePayload();
        payload.uid = managerUid != null && !managerUid.isEmpty() ? managerUid : uid;
        payload.empid = employeeId != null ? employeeId : "";
        payload.timein = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date());
        payload.imagepath = imagepath != null ? imagepath : "";
        payload.type = attendanceType != null ? attendanceType.toUpperCase() : "IN";
        payload.isMovement = isMovement;
        payload.projectId = selectedProjectId != null ? selectedProjectId : "";
        payload.projname = projname != null ? projname : "";
        payload.locationId = selectedLocationId != null ? selectedLocationId : "";
        payload.breakHours = breakHoursVal;
        payload.lat = punchLat;
        payload.lng = punchLng;
        payload.accuracy = punchAccuracy;
        payload.gpsTimeMs = gpsTimeMs;

        RuntimeLogger.log(this, currentUserId, "INFO", "Attendance",
                "Submitting via repository type=" + payload.type + " move=" + payload.isMovement
                        + " lat=" + payload.lat + " lng=" + payload.lng);
        attendanceViewModel.submit(payload);
    }

    private void onAttendanceResult(com.app.fourscontracting.data.AttendanceResult result) {
        if (result == null || isFinishing()) {
            return;
        }
        if (loadingManager != null) {
            loadingManager.hideLoading();
        }

        if (result.success) {
            if (result.queuedOffline) {
                Toast.makeText(this, result.userMessage(), Toast.LENGTH_LONG).show();
            } else if (isMovement) {
                Toasty.success(this, "Attendance Captured (Travel Time Synced)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, result.userMessage(), Toast.LENGTH_SHORT).show();
            }
            Intent intentHome = new Intent(LocationActivity.this, WebviewActivity.class);
            if (userLocalStore != null && userLocalStore.getLoggedInUser() != null) {
                intentHome.putExtra("key", userLocalStore.getLoggedInUser().username);
            }
            intentHome.putExtra("attendance_submitted", true);
            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intentHome);
            finish();
        } else {
            Toast.makeText(this, result.userMessage(), Toast.LENGTH_LONG).show();
            RuntimeLogger.log(this,
                    employeeId != null && !employeeId.isEmpty() ? employeeId : "unknown",
                    "ERROR", "Attendance",
                    "FAILED code=" + result.code + " msg=" + result.message);
        }
    }

    // Method to get user information
    private void saveUserInfo() {
        // Get mobile version
        String mobileVersion = Build.VERSION.RELEASE;
        // Get network type
        String networkType = getNetworkType();

        // Get current location
        String curloc = getCurrentLocation();

        Toast.makeText(LocationActivity.this, "uid:"+ userid, Toast.LENGTH_LONG).show();
        Toast.makeText(LocationActivity.this, "projname:"+ projectid, Toast.LENGTH_LONG).show();

        String app_info_url = "https://4scontracting.com/SMCS_APP/fcm_app/saveuserinfo.php";

        // Now you can save the information (mobileVersion, networkType, latitude, longitude) as needed
        MySingleton.getmInstance(LocationActivity.this).addToRequestque(new StringRequest(1, app_info_url, new Response.Listener<String>() {
            public void onResponse(String response) {
                if (response.equals("error")) {
                    Toast.makeText(LocationActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(LocationActivity.this, response, Toast.LENGTH_SHORT).show();
                //LocationActivity.this.startActivity(new Intent(LocationActivity.this, WebviewActivity.class));
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (!(response == null || response.data == null)) {
                    Toast.makeText(LocationActivity.this, new String(response.data), Toast.LENGTH_LONG).show();
                }
                Toast.makeText(LocationActivity.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        }) {
            /* access modifiers changed from: protected */
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("projname", projectid);
                params.put("uid", userid);
                params.put("mobileVersion", mobileVersion);
                params.put("networkType", networkType);
                params.put("curloc", curloc);
                return params;
            }
        });
    }

    // Method to get the current location
    @SuppressLint("MissingPermission")
    private String getCurrentLocation() {
        if (!AppPermissions.hasLocation(this)) {
            return null;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            return "Latitude: " + latitude + ", Longitude: " + longitude;
            // Save or use the latitude and longitude as needed
            //Toast.makeText(this, "Latitude: " + latitude + ", Longitude: " + longitude, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    // Method to get the network type
    private String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return "Wi-Fi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "Mobile Data";
            }
        }
        return "No Connection";
    }


    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Button savebtn = (Button) findViewById(R.id.saveattendance);
        if (requestCode == CAMERA_REQUEST && resultCode == -1) {
            if (data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    this.imageView1.setImageBitmap(photo);
                    
                    // Offload CPU-heavy image compression to a background thread to prevent ANR
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String encodedImage = getStringImage(photo);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (LocationActivity.this.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && LocationActivity.this.isDestroyed())) {
                                        return;
                                    }
                                    LocationActivity.this.imagepath = encodedImage;
                                    if (savebtn != null) {
                                        savebtn.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(this, "Failed to capture image data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getStringImage(Bitmap bmp) {
        if (bmp == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Resize bitmap first to a max width/height of 800px
        Bitmap resizedBitmap = getResizedBitmap(bmp, 800); 
        
        // Compress quality to 70%
        if (resizedBitmap != null) {
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        }
        
        byte[] imageBytes = baos.toByteArray();
        
        if (resizedBitmap != null && resizedBitmap != bmp) {
            resizedBitmap.recycle();
        }
        
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    // Helper to keep image size small
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void popTimePicker(View view) {
        Toast.makeText(LocationActivity.this, "Time is locked to the present live time.", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
        try {
            LocationManager locationManager2 = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            this.locationManager = locationManager2;
            if (!AppPermissions.hasLocation(this)) {
                AppPermissions.ensureOrGuide(this, false, true);
                return;
            }
            locationManager2.requestLocationUpdates("gps", 100, 5.0f, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location access verified", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location permission is required for site check-in", Toast.LENGTH_LONG).show();
                }
                break;
                
            case MY_CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera access verified", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera permission is required for photo verification", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void onLocationChanged(Location location) {
    }

    private void showAlertDialog(int myLayout) {
        this.builderDialog = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(myLayout, (ViewGroup) null);
        this.builderDialog.setView(layoutView);
        android.app.AlertDialog create = this.builderDialog.create();
        this.alertDialog = create;
        create.show();
        ((AppCompatButton) layoutView.findViewById(R.id.buttonOk)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LocationActivity.this.alertDialog.dismiss();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAutoTimeOrPrompt();
    }

    private void checkAutoTimeOrPrompt() {
        int autoTime = 0;
        try {
            autoTime = android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.AUTO_TIME);
        } catch (Exception e) {
            autoTime = 0;
        }

        if (autoTime == 1) {
            if (timeDialog != null && timeDialog.isShowing()) {
                timeDialog.dismiss();
            }
        } else {
            if (timeDialog == null || !timeDialog.isShowing()) {
                timeDialog = new android.app.AlertDialog.Builder(this)
                    .setTitle("Automatic Time Required")
                    .setMessage("To prevent timesheet tampering, this app requires your device clock to be set to automatic network time. Please enable 'Set time automatically' in settings.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        try {
                            startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS));
                        } catch (Exception e) {
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Exit", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            }
        }
    }

    @Override
    protected void onPause() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (loadingManager != null) {
            loadingManager.hideLoading();
        }
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}