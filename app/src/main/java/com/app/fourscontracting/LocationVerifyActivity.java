package com.app.fourscontracting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.app.fourscontracting.databinding.ActivityLocationVerifyBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationVerifyActivity extends AppCompatActivity {

    private static final String TAG = "LocationVerifyActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private ActivityLocationVerifyBinding binding;
    private SessionManager session;
    private LocationModel targetLocation;
    private Location gpsLocation;
    private String projectCode = "";
    private String capturedBase64 = "";
    private int locationId = 0;
    private FusedLocationProviderClient fusedLocationClient;

    private final Handler liveClockHandler = new Handler(Looper.getMainLooper());
    private final Runnable liveClockRunnable = new Runnable() {
        @Override
        public void run() {
            String currentDateTime = new SimpleDateFormat("EEE, dd MMM yyyy | hh:mm:ss a", Locale.getDefault()).format(new Date());
            if (binding != null && binding.tvVerifyDatetime != null) {
                binding.tvVerifyDatetime.setText(currentDateTime);
            }
            liveClockHandler.postDelayed(this, 1000);
        }
    };

    // Camera result launcher
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                setButtonsEnabled(true);
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = null;
                    if (result.getData().getExtras() != null) {
                        photo = (Bitmap) result.getData().getExtras().get("data");
                    }
                    if (photo == null && result.getData().getData() != null) {
                        try {
                            photo = MediaStore.Images.Media.getBitmap(getContentResolver(), result.getData().getData());
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding camera photo", e);
                        }
                    }
                    if (photo != null) {
                        capturedBase64 = ImageUtils.bitmapToBase64(photo);
                        if (binding != null) {
                            if (binding.imgSelfie != null) {
                                binding.imgSelfie.setImageBitmap(photo);
                            }
                            if (binding.cardSelfiePreview != null) {
                                binding.cardSelfiePreview.setVisibility(View.VISIBLE);
                            }
                        }
                        callPunchApi(); // Call API only after photo
                    } else {
                        showError("Photo capture failed. Please retry.");
                    }
                } else {
                    showError("Selfie photo is required to punch attendance.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initHeaderInfo();

        // Read incoming project parameters
        Intent intent = getIntent();
        if (intent != null) {
            projectCode = intent.getStringExtra("project_code");
            if (projectCode == null || projectCode.isEmpty()) {
                projectCode = intent.getStringExtra("projname");
            }
            if (projectCode == null || projectCode.isEmpty()) {
                projectCode = intent.getStringExtra("project_name");
            }
            if (projectCode == null || projectCode.isEmpty()) {
                projectCode = intent.getStringExtra("selected_project_id");
            }
            locationId = intent.getIntExtra("location_id", 0);
            if (locationId == 0) {
                String locStr = intent.getStringExtra("location_id");
                if (locStr != null && !locStr.isEmpty()) {
                    try { locationId = Integer.parseInt(locStr); } catch (Exception ignored) {}
                }
            }
        }

        // Initialize button click listeners
        if (binding.btnPunch != null) {
            binding.btnPunch.setOnClickListener(v -> onPunchClicked());
        }
        if (binding.btnVerify != null) {
            binding.btnVerify.setOnClickListener(v -> onPunchClicked());
        }

        setupProjectSelection();

        // Always load projects from server to populate dropdown spinner
        loadProjectsFromServer();
    }

    private void initHeaderInfo() {
        if (binding.tvVerifyUserName != null) {
            String u = session.getUsername();
            if (u == null || u.isEmpty()) {
                u = new UserLocalStore(this).getLoggedInUser().displayName;
            }
            if (u == null || u.isEmpty()) u = "User";
            binding.tvVerifyUserName.setText("USER: " + u);
        }
    }

    private void setupProjectSelection() {
        if (binding.myProjectSpinner == null) return;
        binding.myProjectSpinner.setOnClickListener(v -> {
            if (!binding.myProjectSpinner.isPopupShowing()) {
                binding.myProjectSpinner.showDropDown();
            }
        });
        if (binding.projectInputLayout != null) {
            binding.projectInputLayout.setOnClickListener(v -> {
                if (!binding.myProjectSpinner.isPopupShowing()) {
                    binding.myProjectSpinner.showDropDown();
                }
            });
        }
    }

    private void loadProjectsFromServer() {
        if (binding.progressProjectsLoading != null) {
            binding.progressProjectsLoading.setVisibility(View.VISIBLE);
        }
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Loading projects...");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        ApiClient.getService().getProjects().enqueue(new Callback<List<ProjectModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProjectModel>> call, @NonNull Response<List<ProjectModel>> response) {
                if (binding.progressProjectsLoading != null) {
                    binding.progressProjectsLoading.setVisibility(View.GONE);
                }
                List<ProjectModel> list = response.body();
                if (list != null && !list.isEmpty()) {
                    ArrayAdapter<ProjectModel> adapter = new ArrayAdapter<>(
                            LocationVerifyActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            list
                    );
                    binding.myProjectSpinner.setAdapter(adapter);
                    binding.myProjectSpinner.setOnItemClickListener((parent, view, position, id) -> {
                        ProjectModel sel = (ProjectModel) parent.getItemAtPosition(position);
                        if (sel != null) {
                            projectCode = sel.getProjectCode();
                            locationId = sel.id;
                            binding.myProjectSpinner.setText(sel.toString(), false);
                            loadTargetLocation();
                        }
                    });

                    boolean foundPreselected = false;
                    if (projectCode != null && !projectCode.trim().isEmpty()
                            && !"Verified Site".equalsIgnoreCase(projectCode.trim())
                            && !com.app.fourscontracting.data.AttendancePayload.isNumeric(projectCode.trim())) {
                        for (ProjectModel p : list) {
                            if (projectCode.equalsIgnoreCase(p.getProjectCode())) {
                                projectCode = p.getProjectCode();
                                locationId = p.id;
                                binding.myProjectSpinner.setText(p.toString(), false);
                                loadTargetLocation();
                                foundPreselected = true;
                                break;
                            }
                        }
                    }

                    if (!foundPreselected) {
                        // Prefer RST or first project if none selected yet
                        ProjectModel defaultProject = list.get(0);
                        for (ProjectModel p : list) {
                            if ("RST".equalsIgnoreCase(p.getProjectCode())) {
                                defaultProject = p;
                                break;
                            }
                        }
                        projectCode = defaultProject.getProjectCode();
                        locationId = defaultProject.id;
                        binding.myProjectSpinner.setText(defaultProject.toString(), false);
                        loadTargetLocation();
                    }
                } else {
                    showError("No projects available");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProjectModel>> call, @NonNull Throwable t) {
                if (binding.progressProjectsLoading != null) {
                    binding.progressProjectsLoading.setVisibility(View.GONE);
                }
                showError("Failed to load projects: " + t.getMessage());
            }
        });
    }

    private void loadTargetLocation() {
        if (projectCode == null || projectCode.isEmpty()) {
            showError("No Project Allocated");
            return;
        }

        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Loading location for " + projectCode + "...");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        ApiClient.getService().getLocations(projectCode).enqueue(new Callback<List<LocationModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<LocationModel>> c, @NonNull Response<List<LocationModel>> r) {
                if (r.body() == null || r.body().isEmpty()) {
                    if (binding.tvStatus != null) {
                        binding.tvStatus.setText("No location configured for this project");
                    }
                    setButtonsEnabled(false);
                    return;
                }
                targetLocation = r.body().get(0);
                if (locationId == 0) {
                    locationId = targetLocation.id;
                }

                if (binding.tvDetails != null) {
                    binding.tvDetails.setText("Site: " + targetLocation.location_name + "\nProject: " + projectCode);
                    binding.tvDetails.setVisibility(View.VISIBLE);
                }
                if (binding.tvVerifyProjectLocation != null) {
                    binding.tvVerifyProjectLocation.setText(targetLocation.location_name + " (" + projectCode + ")");
                }

                loadTodayStatus();
            }

            @Override
            public void onFailure(@NonNull Call<List<LocationModel>> c, @NonNull Throwable t) {
                showError("Failed to load location: " + t.getMessage());
            }
        });
    }

    private void loadTodayStatus() {
        int deptId = session.getDeptId();
        if (deptId == 0) deptId = new UserLocalStore(this).getDeptId();
        int uid = session.getUid();
        int empId = session.getEmpId();

        ApiClient.getService().getTodayStatus(deptId, locationId, uid, empId)
                .enqueue(new Callback<ManagerResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ManagerResponse> c, @NonNull Response<ManagerResponse> r) {
                        String next = calculateNextAction(r.body());
                        setButtonAction(next);
                        setButtonsEnabled(true);
                        if (binding.tvStatus != null) {
                            binding.tvStatus.setText("Ready to " + next);
                            binding.tvStatus.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ManagerResponse> c, @NonNull Throwable t) {
                        setButtonAction("IN");
                        setButtonsEnabled(true);
                    }
                });
    }

    private String calculateNextAction(ManagerResponse res) {
        if (res == null) return "IN";

        // Check structured shifts
        if (res.data != null && !res.data.isEmpty()) {
            for (ManagerResponse.AttendanceData att : res.data) {
                if (att.shifts != null && !att.shifts.isEmpty()) {
                    for (ManagerResponse.ShiftData s : att.shifts) {
                        if (s.out_at == null || s.out_at.isEmpty() || "-".equals(s.out_at)) {
                            return "OUT";
                        }
                    }
                }
            }
            return "IN";
        }

        // Check labour employees list format
        if (res.employees != null && !res.employees.isEmpty()) {
            String myEmpIdStr = String.valueOf(session.getEmpId());
            for (ManagerResponse.LabourEmployeeItem item : res.employees) {
                if (item.id != null && item.id.trim().equals(myEmpIdStr)) {
                    if (item.can_out || "IN_HERE".equalsIgnoreCase(item.status_code)) {
                        return "OUT";
                    } else if (item.can_in || "OFF_DUTY".equalsIgnoreCase(item.status_code) || "COMPLETED".equalsIgnoreCase(item.status_code)) {
                        return "IN";
                    }
                }
            }
        }

        return "IN"; // Shift 1 closed, next IN = Shift 2 on server
    }

    private void setButtonAction(String action) {
        String label = "PUNCH " + action;
        if (binding.btnPunch != null) {
            binding.btnPunch.setText(label);
            binding.btnPunch.setTag(action);
            binding.btnPunch.setVisibility(View.VISIBLE);
        }
        if (binding.btnVerify != null) {
            binding.btnVerify.setText(label);
            binding.btnVerify.setTag(action);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (binding.btnPunch != null) binding.btnPunch.setEnabled(enabled);
        if (binding.btnVerify != null) binding.btnVerify.setEnabled(enabled);
    }

    private void onPunchClicked() {
        if (targetLocation == null) {
            showError("Location not loaded yet");
            return;
        }
        if (!PermissionHelper.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        setButtonsEnabled(false);
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Acquiring GPS fix...");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        fetchGpsAndVerify();
    }

    private void fetchGpsAndVerify() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission required");
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(1)
                .setDurationMillis(12000)
                .build();

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    processGpsLocation(loc);
                } else {
                    tryFallbackLastLocation();
                }
            }
        }, Looper.getMainLooper());
    }

    private void tryFallbackLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission required");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                processGpsLocation(loc);
            } else {
                showError("Unable to acquire GPS fix. Please ensure Location is ON and move outdoors.");
            }
        }).addOnFailureListener(e -> showError("GPS failed: " + e.getMessage()));
    }

    private void processGpsLocation(@NonNull Location loc) {
        this.gpsLocation = loc;
        updateGpsDisplay(loc);

        if (LocationVerifier.isMockLocation(loc)) {
            showError("Mock/Fake GPS detected. Please turn off mock location apps.");
            return;
        }

        if (loc.getAccuracy() > 55.0f) {
            showError("GPS accuracy is " + (int) loc.getAccuracy() + "m. Please wait for accuracy under 50m.");
            return;
        }

        if (targetLocation == null) {
            showError("Target location not loaded yet.");
            return;
        }

        float distance = LocationVerifier.distanceBetween(
                loc.getLatitude(), loc.getLongitude(),
                targetLocation.latitude, targetLocation.longitude
        );

        if (distance > 200.0f) {
            showError("You are " + (int) distance + "m away from site. Maximum allowed distance is 200m.");
            return;
        }

        // Within geofence! Open camera for mandatory selfie photo
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("GPS Verified (" + (int) distance + "m). Opening camera for selfie...");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        openCamera();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(cameraIntent);
        } catch (Exception e) {
            showError("Camera unavailable: " + e.getMessage());
        }
    }

    private void callPunchApi() {
        if (capturedBase64 == null || capturedBase64.isEmpty()) {
            showError("Please take selfie photo");
            return;
        }
        if (gpsLocation == null) {
            showError("GPS not found");
            return;
        }

        String type = "IN";
        if (binding.btnPunch != null && binding.btnPunch.getTag() != null) {
            type = binding.btnPunch.getTag().toString();
        } else if (binding.btnVerify != null && binding.btnVerify.getTag() != null) {
            type = binding.btnVerify.getTag().toString();
        }

        int isMock = LocationVerifier.isMockLocation(gpsLocation) ? 1 : 0;
        int deptId = session.getDeptId();
        if (deptId == 0) deptId = new UserLocalStore(this).getDeptId();
        if (deptId == 0) deptId = 5;

        int punchUid = session.getUid();
        int punchEmpId = session.getEmpId();

        Intent inc = getIntent();
        if (inc != null) {
            String extraUid = inc.getStringExtra("uid");
            if (extraUid != null && !extraUid.isEmpty() && com.app.fourscontracting.data.AttendancePayload.isNumeric(extraUid)) {
                try { punchUid = Integer.parseInt(extraUid.trim()); } catch (Exception ignored) {}
            }
            String extraEmp = inc.getStringExtra("empid");
            if (extraEmp == null || extraEmp.isEmpty()) extraEmp = inc.getStringExtra("pending_eid");
            if (extraEmp != null && !extraEmp.isEmpty() && com.app.fourscontracting.data.AttendancePayload.isNumeric(extraEmp)) {
                try { punchEmpId = Integer.parseInt(extraEmp.trim()); } catch (Exception ignored) {}
            }
        }
        if (punchEmpId == 0) punchEmpId = punchUid;

        if (binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        ApiClient.getService().punchAttendance(
                punchUid,
                punchEmpId,
                deptId,
                projectCode,
                type,
                gpsLocation.getLatitude(),
                gpsLocation.getLongitude(),
                isMock,
                capturedBase64,
                ""
        ).enqueue(new Callback<PunchResponse>() {
            @Override
            public void onResponse(@NonNull Call<PunchResponse> c, @NonNull Response<PunchResponse> r) {
                if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);

                if (r.body() == null) {
                    showError("Empty server response");
                    return;
                }
                PunchResponse res = r.body();
                if ("success".equalsIgnoreCase(res.status)) {
                    String msg = (res.message != null && !res.message.isEmpty()) ? res.message : "Attendance saved successfully";
                    if (binding.tvStatus != null) {
                        binding.tvStatus.setText(msg);
                        binding.tvStatus.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(LocationVerifyActivity.this, msg, Toast.LENGTH_LONG).show();

                    SessionPrefs sessionPrefs = new SessionPrefs(LocationVerifyActivity.this);
                    String freshToken = sessionPrefs.issueFreshVerificationToken();
                    sessionPrefs.saveVerifiedLocation(String.valueOf(locationId), projectCode, freshToken);
                    sessionPrefs.saveProject(projectCode, projectCode, "");

                    Intent resultData = new Intent();
                    resultData.putExtra("location_id", String.valueOf(locationId));
                    resultData.putExtra("project_id", projectCode);
                    resultData.putExtra("project_code", projectCode);
                    resultData.putExtra("project_name", projectCode);
                    resultData.putExtra("VERIFIED", "true");
                    resultData.putExtra("VERIFICATION_TOKEN", freshToken);
                    setResult(RESULT_OK, resultData);

                    capturedBase64 = ""; // Clear photo for next shift
                    loadTodayStatus(); // Auto IN <-> OUT
                } else {
                    showError(res.message != null ? res.message : "Punch failed");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PunchResponse> c, @NonNull Throwable t) {
                if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                setButtonsEnabled(true);
                showError("Network failed. Please try again.");
            }
        });
    }

    private void updateGpsDisplay(Location loc) {
        if (loc == null) return;
        runOnUiThread(() -> {
            if (binding.tvGpsLat != null) {
                binding.tvGpsLat.setText(String.format(Locale.US, "+%.5f° N", loc.getLatitude()));
            }
            if (binding.tvGpsLng != null) {
                binding.tvGpsLng.setText(String.format(Locale.US, "+%.5f° E", loc.getLongitude()));
            }
            if (binding.tvVerifyProgressPct != null && binding.progressVerifyBar != null) {
                int pct = Math.min(100, (int) (100.0f * Math.min(1.0f, 50.0f / Math.max(1.0f, loc.getAccuracy()))));
                pct = Math.max(30, pct);
                binding.tvVerifyProgressPct.setText(pct + "%");
                binding.progressVerifyBar.setProgress(pct);
            }
        });
    }

    private void showError(String msg) {
        if (binding.tvStatus != null) {
            binding.tvStatus.setText(msg);
            binding.tvStatus.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        setButtonsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPunchClicked();
            } else {
                showError("Location permission is required to verify GPS");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        liveClockHandler.removeCallbacks(liveClockRunnable);
        liveClockHandler.post(liveClockRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        liveClockHandler.removeCallbacks(liveClockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveClockHandler.removeCallbacks(liveClockRunnable);
    }
}
