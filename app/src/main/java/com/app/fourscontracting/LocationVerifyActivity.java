package com.app.fourscontracting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationVerifyActivity extends AppCompatActivity {

    private static final String TAG = "LocationVerifyActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private static volatile boolean isVerificationActive = false;

    public static boolean isVerificationActive() {
        return isVerificationActive;
    }

    private ActivityLocationVerifyBinding binding;
    private SessionManager session;
    private SessionPrefs sessionPrefs;
    private LocationModel targetLocation;
    private Location gpsLocation;
    private String projectCode = "";
    private String selectedLocationName = "";
    private int locationId = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAutoStartVerify = false;
    private boolean hasStartedAutoVerify = false;

    private boolean isAutoDetected = false;
    private boolean isAutoDetecting = false;
    private final List<LocationModel> cachedSiteLocations = new CopyOnWriteArrayList<>();
    private final Handler autoDetectTimeoutHandler = new Handler(Looper.getMainLooper());

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isVerificationActive) {
            Log.w(TAG, "LocationVerifyActivity already active, dismissing duplicate instance.");
            finish();
            return;
        }
        isVerificationActive = true;

        session = new SessionManager(this);
        sessionPrefs = new SessionPrefs(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if location session is already valid for today (prevents showing multiple times)
        Intent intent = getIntent();
        boolean isChangeSite = intent != null && intent.getBooleanExtra("IS_CHANGE_SITE", false);

        if (sessionPrefs.isLocationSessionValid() && !isChangeSite) {
            Log.d(TAG, "Location session already valid for today. Finishing immediately.");
            Intent resultIntent = new Intent();
            String savedId = sessionPrefs.getLocationId();
            String savedName = sessionPrefs.getProjectName();
            resultIntent.putExtra("location_id", savedId);
            resultIntent.putExtra("project_id", sessionPrefs.getProjectId());
            resultIntent.putExtra("projname", savedName);
            resultIntent.putExtra("project_name", savedName);
            resultIntent.putExtra("location_name", savedName);
            resultIntent.putExtra("VERIFIED", "true");
            resultIntent.putExtra("VERIFICATION_TOKEN", sessionPrefs.getVerificationToken());
            setResult(RESULT_OK, resultIntent);
            isVerificationActive = false;
            finish();
            return;
        }

        binding = ActivityLocationVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initHeaderInfo();

        if (intent != null) {
            isAutoStartVerify = intent.getBooleanExtra("AUTO_START_VERIFY", false);
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
            if (intent.hasExtra("location_name")) {
                String ln = intent.getStringExtra("location_name");
                if (ln != null && !ln.trim().isEmpty()) {
                    selectedLocationName = ln.trim();
                }
            }
        }

        // Initialize single button - prompt user to select project first
        if (binding.btnVerify != null) {
            binding.btnVerify.setText("SELECT PROJECT FIRST");
            binding.btnVerify.setEnabled(false);
            binding.btnVerify.setOnClickListener(v -> onVerifyClicked());
        }

        setupProjectSelection();

        // 1. Immediately begin background GPS acquisition for fast auto-verification (< 100ms)
        startGpsPreAcquisition();

        // 2. Always load projects from server to populate dropdown spinner and run auto-detection
        loadProjectsFromServer();
    }

    private void initHeaderInfo() {
        if (binding.tvVerifyUserName != null) {
            String u = session.getEmpName();
            if (u == null || u.trim().isEmpty()) {
                u = session.getUsername();
            }
            if (u == null || u.trim().isEmpty()) {
                try {
                    User user = new UserLocalStore(this).getLoggedInUser();
                    if (user != null) {
                        u = user.displayName;
                        if (u == null || u.trim().isEmpty()) {
                            String[] parts = UserLocalStore.parseUserInfo(user.username);
                            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                                u = parts[2].trim();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (u == null || u.trim().isEmpty()) u = "User";
            binding.tvVerifyUserName.setText(u.trim());
        }
    }

    private void setupProjectSelection() {
        if (binding.myProjectSpinner == null) return;
        binding.myProjectSpinner.setOnClickListener(v -> {
            if (isAutoDetecting) {
                onAutoDetectFailed("Please choose your project manually from the dropdown.");
            }
            if (!binding.myProjectSpinner.isPopupShowing()) {
                binding.myProjectSpinner.showDropDown();
            }
        });
        binding.myProjectSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (isAutoDetecting) {
                    onAutoDetectFailed("Please choose your project manually from the dropdown.");
                }
                if (!binding.myProjectSpinner.isPopupShowing()) {
                    binding.myProjectSpinner.showDropDown();
                }
            }
        });
        if (binding.projectInputLayout != null) {
            binding.projectInputLayout.setOnClickListener(v -> {
                if (isAutoDetecting) {
                    onAutoDetectFailed("Please choose your project manually from the dropdown.");
                }
                if (!binding.myProjectSpinner.isPopupShowing()) {
                    binding.myProjectSpinner.showDropDown();
                }
            });
            binding.projectInputLayout.setEndIconOnClickListener(v -> {
                if (isAutoDetecting) {
                    onAutoDetectFailed("Please choose your project manually from the dropdown.");
                }
                if (!binding.myProjectSpinner.isPopupShowing()) {
                    binding.myProjectSpinner.showDropDown();
                }
            });
        }
    }

    private void startGpsPreAcquisition() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && !isFinishing() && !isDestroyed()) {
                long ageMs = System.currentTimeMillis() - loc.getTime();
                if (ageMs < 120_000 && loc.getAccuracy() <= 150.0f) {
                    gpsLocation = loc;
                    updateGpsDisplay(loc);
                    tryAutoMatchSite();
                }
            }
        });

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdates(2)
                .setDurationMillis(8000)
                .build();

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                Location loc = locationResult.getLastLocation();
                if (loc != null && !isFinishing() && !isDestroyed()) {
                    gpsLocation = loc;
                    updateGpsDisplay(loc);
                    tryAutoMatchSite();
                }
            }
        }, Looper.getMainLooper());
    }

    private void loadProjectsFromServer() {
        if (binding.progressProjectsLoading != null) {
            binding.progressProjectsLoading.setVisibility(View.VISIBLE);
        }
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Loading projects & detecting site...");
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
                            onManualProjectSelected(sel);
                        }
                    });

                    // Kick off Auto Location Detection with the project list
                    startAutoLocationDetection(list);
                } else {
                    showError("No projects available");
                    onAutoDetectFailed("No projects configured on server.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProjectModel>> call, @NonNull Throwable t) {
                if (binding.progressProjectsLoading != null) {
                    binding.progressProjectsLoading.setVisibility(View.GONE);
                }
                showError("Failed to load projects: " + t.getMessage());
                onAutoDetectFailed("Network error loading projects. Select manually.");
            }
        });
    }

    private void startAutoLocationDetection(List<ProjectModel> list) {
        if (isAutoDetected) return;
        isAutoDetecting = true;

        if (binding.cardAutoDetectBanner != null) {
            binding.cardAutoDetectBanner.setVisibility(View.VISIBLE);
            binding.cardAutoDetectBanner.setCardBackgroundColor(Color.parseColor("#F0F9FF"));
            binding.cardAutoDetectBanner.setStrokeColor(Color.parseColor("#BAE6FD"));
            binding.imgAutoDetectIcon.setImageResource(R.drawable.ic_target_gps_24);
            binding.imgAutoDetectIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
            binding.tvAutoDetectTitle.setText("Auto-Detecting Site...");
            binding.tvAutoDetectTitle.setTextColor(Color.parseColor("#03045E"));
            binding.tvAutoDetectDesc.setText("Checking your current GPS location against site geofences...");
            binding.tvAutoDetectDesc.setTextColor(Color.parseColor("#0077B6"));
        }

        // Load cached site locations from local storage
        cachedSiteLocations.clear();
        List<LocationModel> saved = sessionPrefs.getCachedSiteLocations();
        if (saved != null && !saved.isEmpty()) {
            cachedSiteLocations.addAll(saved);
        }

        // Immediate check if GPS and cached locations are available (< 50ms)
        if (gpsLocation != null && !cachedSiteLocations.isEmpty()) {
            tryAutoMatchSite();
            if (isAutoDetected) return;
        }

        // Prewarm locations from candidate projects
        prewarmCandidateSiteLocations(list);

        // Failsafe timeout: if not auto-detected within 3 seconds, gracefully fallback to manual selection
        autoDetectTimeoutHandler.removeCallbacksAndMessages(null);
        autoDetectTimeoutHandler.postDelayed(() -> {
            if (isAutoDetecting && !isAutoDetected) {
                onAutoDetectFailed("Could not auto-detect site at current location. Please select your project below.");
            }
        }, 3000);
    }

    private void prewarmCandidateSiteLocations(List<ProjectModel> list) {
        if (list == null || list.isEmpty()) return;

        for (ProjectModel p : list) {
            final String code = p.getProjectCode();
            if (code.isEmpty()) continue;

            boolean alreadyCached = false;
            for (LocationModel loc : cachedSiteLocations) {
                if (code.equalsIgnoreCase(loc.getProjectCode())) {
                    alreadyCached = true;
                    break;
                }
            }

            if (!alreadyCached) {
                ApiClient.getService().getLocations(code).enqueue(new Callback<List<LocationModel>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<LocationModel>> call, @NonNull Response<List<LocationModel>> resp) {
                        List<LocationModel> locs = resp.body();
                        if (locs != null && !locs.isEmpty()) {
                            for (LocationModel lm : locs) {
                                if (lm != null) {
                                    if (lm.project_code == null || lm.project_code.isEmpty()) {
                                        lm.project_code = code;
                                    }
                                    cachedSiteLocations.add(lm);
                                }
                            }
                            sessionPrefs.saveCachedSiteLocations(new ArrayList<>(cachedSiteLocations));
                            if (isAutoDetecting && !isAutoDetected) {
                                tryAutoMatchSite();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<LocationModel>> call, @NonNull Throwable t) {
                        // Ignore individual failure during prewarm
                    }
                });
            }
        }
    }

    private synchronized void tryAutoMatchSite() {
        if (isAutoDetected || isFinishing() || isDestroyed()) return;
        if (gpsLocation == null || cachedSiteLocations.isEmpty()) return;

        LocationModel matched = LocationVerifier.findMatchingSite(gpsLocation, new ArrayList<>(cachedSiteLocations));
        if (matched != null) {
            onSiteAutoDetected(matched);
        }
    }

    private void onSiteAutoDetected(LocationModel matched) {
        if (isFinishing() || isDestroyed()) return;
        isAutoDetected = true;
        isAutoDetecting = false;
        autoDetectTimeoutHandler.removeCallbacksAndMessages(null);

        targetLocation = matched;
        locationId = matched.id;
        projectCode = matched.getProjectCode();
        selectedLocationName = matched.getLocationName();

        // 1. Banner to Green (Success)
        if (binding.cardAutoDetectBanner != null) {
            binding.cardAutoDetectBanner.setVisibility(View.VISIBLE);
            binding.cardAutoDetectBanner.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
            binding.cardAutoDetectBanner.setStrokeColor(Color.parseColor("#A7F3D0"));
            binding.imgAutoDetectIcon.setImageResource(R.drawable.ic_target_gps_24);
            binding.imgAutoDetectIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
            binding.tvAutoDetectTitle.setText("Site Auto-Detected");
            binding.tvAutoDetectTitle.setTextColor(Color.parseColor("#065F46"));
            String siteDisplay = matched.getLocationName();
            if (!matched.getProjectCode().isEmpty() && !matched.getProjectCode().equalsIgnoreCase(siteDisplay)) {
                siteDisplay += " (" + matched.getProjectCode() + ")";
            }
            binding.tvAutoDetectDesc.setText("You are at " + siteDisplay + ". Tap Confirm below or select another project.");
            binding.tvAutoDetectDesc.setTextColor(Color.parseColor("#047857"));
        }

        // 2. Pre-select in the Dropdown (still editable!)
        if (binding.myProjectSpinner != null) {
            String displayDropdown = matched.getProjectCode();
            if (!matched.getLocationName().isEmpty()) {
                displayDropdown += " - " + matched.getLocationName();
            }
            binding.myProjectSpinner.setText(displayDropdown, false);
        }

        if (binding.tvVerifyProjectLocation != null) {
            binding.tvVerifyProjectLocation.setText(matched.getLocationName() + " (" + matched.getProjectCode() + ")");
        }
        if (binding.tvDetails != null) {
            binding.tvDetails.setText("Site: " + matched.getLocationName() + "\nProject: " + matched.getProjectCode() + (matched.hasBoundingBox() ? " (GPS Geofenced)" : ""));
            binding.tvDetails.setVisibility(View.VISIBLE);
        }

        // 3. Update Progress & Status
        if (binding.progressVerifyBar != null) binding.progressVerifyBar.setProgress(100);
        if (binding.tvVerifyProgressPct != null) binding.tvVerifyProgressPct.setText("100%");
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Site detected via GPS: " + matched.getLocationName());
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        // 4. Update Button to Confirm
        if (binding.btnVerify != null) {
            binding.btnVerify.setText("CONFIRM & CONTINUE");
            binding.btnVerify.setEnabled(true);
            binding.btnVerify.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
            binding.btnVerify.setVisibility(View.VISIBLE);
        }
    }

    private void onAutoDetectFailed(String reason) {
        if (isFinishing() || isDestroyed() || isAutoDetected) return;
        isAutoDetecting = false;
        autoDetectTimeoutHandler.removeCallbacksAndMessages(null);

        // 1. Update the Banner to Amber (Informative Fallback)
        if (binding.cardAutoDetectBanner != null) {
            binding.cardAutoDetectBanner.setVisibility(View.VISIBLE);
            binding.cardAutoDetectBanner.setCardBackgroundColor(Color.parseColor("#FFFBEB"));
            binding.cardAutoDetectBanner.setStrokeColor(Color.parseColor("#FDE68A"));
            binding.imgAutoDetectIcon.setImageResource(R.drawable.ic_info_circle_24);
            binding.imgAutoDetectIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D97706")));
            binding.tvAutoDetectTitle.setText("Manual Project Selection");
            binding.tvAutoDetectTitle.setTextColor(Color.parseColor("#92400E"));
            binding.tvAutoDetectDesc.setText(reason != null ? reason : "Could not auto-detect site at current location. Please select your project below.");
            binding.tvAutoDetectDesc.setTextColor(Color.parseColor("#B45309"));
        }

        // 2. Guide user to Manual Dropdown
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Please select your project from the dropdown");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }
        if (binding.tvDetails != null) {
            binding.tvDetails.setText("Select your working project from the dropdown above.");
            binding.tvDetails.setVisibility(View.VISIBLE);
        }

        // 3. Reset Button for manual verification
        if (targetLocation == null) {
            if (binding.btnVerify != null) {
                binding.btnVerify.setText("SELECT PROJECT FIRST");
                binding.btnVerify.setEnabled(false);
                binding.btnVerify.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03045E")));
            }
            binding.myProjectSpinner.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    binding.myProjectSpinner.showDropDown();
                }
            });
        }
    }

    private void onManualProjectSelected(ProjectModel sel) {
        isAutoDetecting = false;
        isAutoDetected = false;
        autoDetectTimeoutHandler.removeCallbacksAndMessages(null);

        projectCode = sel.getProjectCode();
        selectedLocationName = sel.getLocationName();
        binding.myProjectSpinner.setText(sel.toString(), false);

        if (binding.cardAutoDetectBanner != null) {
            binding.cardAutoDetectBanner.setVisibility(View.VISIBLE);
            binding.cardAutoDetectBanner.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
            binding.cardAutoDetectBanner.setStrokeColor(Color.parseColor("#E2E8F0"));
            binding.imgAutoDetectIcon.setImageResource(R.drawable.ic_target_gps_24);
            binding.imgAutoDetectIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#03045E")));
            binding.tvAutoDetectTitle.setText("Selected Site: " + (!selectedLocationName.isEmpty() ? selectedLocationName : projectCode));
            binding.tvAutoDetectTitle.setTextColor(Color.parseColor("#03045E"));
            binding.tvAutoDetectDesc.setText("Tap VERIFY GPS CHECK below to verify your location.");
            binding.tvAutoDetectDesc.setTextColor(Color.parseColor("#64748B"));
        }

        if (binding.btnVerify != null) {
            binding.btnVerify.setEnabled(false);
            binding.btnVerify.setText("LOADING LOCATION...");
            binding.btnVerify.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03045E")));
        }

        loadLocationsForProject(projectCode);
    }

    private void loadLocationsForProject(String pCode) {
        if (pCode == null || pCode.isEmpty()) {
            showError("No Project Allocated");
            return;
        }

        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Loading location for " + pCode + "...");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }
        if (binding.btnVerify != null) {
            binding.btnVerify.setEnabled(false);
            binding.btnVerify.setText("LOADING LOCATION...");
        }

        ApiClient.getService().getLocations(pCode).enqueue(new Callback<List<LocationModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<LocationModel>> c, @NonNull Response<List<LocationModel>> r) {
                List<LocationModel> locList = r.body();
                if (locList == null || locList.isEmpty()) {
                    if (binding.tvStatus != null) {
                        binding.tvStatus.setText("No location configured for this project");
                    }
                    if (binding.tvDetails != null) {
                        binding.tvDetails.setText("No location coordinates found for " + pCode + ".");
                    }
                    targetLocation = null;
                    if (binding.btnVerify != null) {
                        binding.btnVerify.setEnabled(false);
                        binding.btnVerify.setText("NO LOCATION CONFIGURED");
                    }
                    return;
                }

                // Auto-select location matching locationId or locationName or default to first location
                LocationModel selLoc = locList.get(0);
                if (locationId > 0) {
                    for (LocationModel lm : locList) {
                        if (lm.id == locationId) {
                            selLoc = lm;
                            break;
                        }
                    }
                } else if (selectedLocationName != null && !selectedLocationName.trim().isEmpty()) {
                    for (LocationModel lm : locList) {
                        if (selectedLocationName.trim().equalsIgnoreCase(lm.getLocationName().trim())) {
                            selLoc = lm;
                            break;
                        }
                    }
                }
                applySelectedLocation(selLoc);
            }

            @Override
            public void onFailure(@NonNull Call<List<LocationModel>> c, @NonNull Throwable t) {
                showError("Failed to load location: " + t.getMessage());
            }
        });
    }

    private void applySelectedLocation(LocationModel loc) {
        if (loc == null) return;
        targetLocation = loc;
        locationId = loc.id;

        boolean alreadyCached = false;
        for (LocationModel lm : cachedSiteLocations) {
            if (lm.id == loc.id) {
                alreadyCached = true;
                break;
            }
        }
        if (!alreadyCached) {
            cachedSiteLocations.add(loc);
            sessionPrefs.saveCachedSiteLocations(new ArrayList<>(cachedSiteLocations));
        }

        if (binding.tvDetails != null) {
            String boundedTag = loc.hasBoundingBox() ? " (GPS Geofenced)" : "";
            binding.tvDetails.setText("Site: " + loc.location_name + "\nProject: " + projectCode + boundedTag);
            binding.tvDetails.setVisibility(View.VISIBLE);
        }
        if (binding.tvVerifyProjectLocation != null) {
            binding.tvVerifyProjectLocation.setText(loc.location_name + " (" + projectCode + ")");
        }

        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Ready to verify location for " + projectCode);
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        if (binding.btnVerify != null) {
            binding.btnVerify.setText("VERIFY GPS CHECK");
            binding.btnVerify.setEnabled(true);
            binding.btnVerify.setVisibility(View.VISIBLE);
            binding.btnVerify.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#03045E")));
        }

        if (isAutoStartVerify && !hasStartedAutoVerify && targetLocation != null && projectCode != null && !projectCode.trim().isEmpty()) {
            hasStartedAutoVerify = true;
            onVerifyClicked();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (binding.btnVerify != null) binding.btnVerify.setEnabled(enabled);
    }

    private void onVerifyClicked() {
        if (isAutoDetected && targetLocation != null && gpsLocation != null) {
            completeVerification(targetLocation, gpsLocation);
            return;
        }

        if (targetLocation == null || projectCode == null || projectCode.trim().isEmpty()) {
            showError("Please select a project from the dropdown first");
            if (binding.myProjectSpinner != null && !binding.myProjectSpinner.isPopupShowing()) {
                binding.myProjectSpinner.showDropDown();
            }
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

        if (!LocationHelper.isGpsEnabled(this)) {
            LocationHelper.checkAndPromptLocationSettings(this, new LocationHelper.LocationSettingsCallback() {
                @Override
                public void onLocationSettingsEnabled() {
                    startGpsVerification();
                }

                @Override
                public void onLocationSettingsFailed(String reason) {
                    showError("Please enable device location / GPS to verify location.");
                }
            });
            return;
        }

        startGpsVerification();
    }

    private void startGpsVerification() {
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

        // Fast-path: Check last known location first for immediate verification (< 100ms)
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && !isFinishing() && !isDestroyed()) {
                long ageMs = System.currentTimeMillis() - loc.getTime();
                if (ageMs < 120_000 && loc.getAccuracy() <= 100.0f && isLocationValidForSite(loc)) {
                    processGpsLocation(loc);
                    return;
                }
            }
            requestFreshGpsLocation();
        }).addOnFailureListener(e -> requestFreshGpsLocation());
    }

    private void requestFreshGpsLocation() {
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

    private boolean isLocationValidForSite(Location loc) {
        if (loc == null || targetLocation == null) return false;
        if (LocationVerifier.isMockLocation(loc)) return false;
        if (targetLocation.hasBoundingBox()) {
            boolean strictlyInside = LocationVerifier.isWithinBoundingBox(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude,
                    targetLocation.max_latitude, targetLocation.max_longtitude,
                    loc.getAccuracy()
            );
            if (strictlyInside) return true;
            float boxDistance = LocationVerifier.distanceToBoundingBox(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude,
                    targetLocation.max_latitude, targetLocation.max_longtitude
            );
            float accuracyBuffer = Math.min(loc.getAccuracy() * 0.75f, 150.0f);
            return boxDistance <= (200.0f + accuracyBuffer);
        } else {
            float distance = LocationVerifier.distanceBetween(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude
            );
            float accuracyBuffer = Math.min(loc.getAccuracy() * 0.75f, 150.0f);
            return distance <= (200.0f + accuracyBuffer);
        }
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

        if (loc.getAccuracy() > 200.0f) {
            showError("GPS accuracy is weak (" + (int) loc.getAccuracy() + "m). Please move outdoors.");
            return;
        }

        if (targetLocation == null) {
            showError("Target location not loaded yet.");
            return;
        }

        float distance;
        boolean isInsideGeofence;

        if (targetLocation.hasBoundingBox()) {
            boolean strictlyInside = LocationVerifier.isWithinBoundingBox(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude,
                    targetLocation.max_latitude, targetLocation.max_longtitude,
                    loc.getAccuracy()
            );
            float boxDistance = LocationVerifier.distanceToBoundingBox(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude,
                    targetLocation.max_latitude, targetLocation.max_longtitude
            );
            float accuracyBuffer = Math.min(loc.getAccuracy() * 0.75f, 150.0f);
            float maxAllowedDistance = 200.0f + accuracyBuffer;

            if (strictlyInside || boxDistance <= maxAllowedDistance) {
                isInsideGeofence = true;
                distance = boxDistance;
            } else {
                isInsideGeofence = false;
                distance = boxDistance;
            }
        } else {
            distance = LocationVerifier.distanceBetween(
                    loc.getLatitude(), loc.getLongitude(),
                    targetLocation.latitude, targetLocation.longitude
            );
            float accuracyBuffer = Math.min(loc.getAccuracy() * 0.75f, 150.0f);
            float maxAllowedDistance = 200.0f + accuracyBuffer;
            isInsideGeofence = (distance <= maxAllowedDistance);
        }

        if (!isInsideGeofence) {
            if (loc.getAccuracy() > 350.0f) {
                showError("GPS accuracy is weak (" + (int) loc.getAccuracy() + "m). Please move outdoors.");
                return;
            }
            showError("You are " + (int) distance + "m away from site. Maximum allowed distance is 200m.");
            return;
        }

        // Within geofence! Location is verified
        completeVerification(targetLocation, loc);
    }

    private void completeVerification(LocationModel loc, Location gps) {
        if (loc == null || gps == null) return;
        if (binding.progressVerifyBar != null) binding.progressVerifyBar.setProgress(100);
        if (binding.tvVerifyProgressPct != null) binding.tvVerifyProgressPct.setText("100%");
        if (binding.tvStatus != null) {
            binding.tvStatus.setText("Location Verified!");
            binding.tvStatus.setVisibility(View.VISIBLE);
        }

        String verifiedSiteName = loc.location_name != null && !loc.location_name.isEmpty()
                ? loc.location_name
                : projectCode;

        SessionPrefs sessionPrefs = new SessionPrefs(this);
        String token = sessionPrefs.issueFreshVerificationToken();
        sessionPrefs.saveVerifiedLocation(String.valueOf(locationId), verifiedSiteName, token);
        sessionPrefs.saveProject(projectCode, verifiedSiteName, "");
        sessionPrefs.saveLocationId(String.valueOf(locationId));

        session.save("location_id", locationId);
        session.save("project_code", projectCode);
        session.save("location_name", verifiedSiteName);
        session.save("project_name", verifiedSiteName);

        new UserLocalStore(this).storeUserLocationData(new UserLocation(verifiedSiteName));
        new UserLocalStore(this).storeUserProjectData(new UserProject(projectCode));

        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("selected_project_name", projectCode)
                .putString("selected_location_name", verifiedSiteName)
                .putString("location_id", String.valueOf(locationId))
                .apply();

        Toast.makeText(this, "Location verified successfully: " + verifiedSiteName, Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("location_id", String.valueOf(locationId));
        resultIntent.putExtra("project_id", projectCode);
        resultIntent.putExtra("project_code", projectCode);
        resultIntent.putExtra("project_name", verifiedSiteName);
        resultIntent.putExtra("location_name", verifiedSiteName);
        resultIntent.putExtra("VERIFIED", "true");
        resultIntent.putExtra("VERIFICATION_TOKEN", token);
        setResult(RESULT_OK, resultIntent);

        Intent inc = getIntent();
        boolean hasPendingPunch = inc != null && (
                (inc.hasExtra("pending_eid") && !TextUtils.isEmpty(inc.getStringExtra("pending_eid"))) ||
                (inc.hasExtra("empid") && !TextUtils.isEmpty(inc.getStringExtra("empid")))
        );

        if (hasPendingPunch) {
            Intent punchIntent = new Intent(this, LocationActivity.class);
            punchIntent.putExtras(inc);
            punchIntent.putExtra("VERIFIED", "true");
            punchIntent.putExtra("VERIFICATION_TOKEN", token);
            punchIntent.putExtra("location_id", String.valueOf(locationId));
            punchIntent.putExtra("project_id", projectCode);
            punchIntent.putExtra("project_code", projectCode);
            punchIntent.putExtra("project_name", verifiedSiteName);
            punchIntent.putExtra("location_name", verifiedSiteName);
            punchIntent.putExtra("lat", gps.getLatitude());
            punchIntent.putExtra("lng", gps.getLongitude());
            punchIntent.putExtra("gps_accuracy", gps.getAccuracy());
            startActivity(punchIntent);
        }

        isVerificationActive = false;
        finish();
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
                onVerifyClicked();
            } else {
                showError("Location permission is required to verify GPS");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LocationHelper.REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) {
                onVerifyClicked();
            } else {
                showError("Please enable device location / GPS to continue.");
            }
        }
    }

    @Override
    public void onBackPressed() {
        isVerificationActive = false;
        super.onBackPressed();
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
        isVerificationActive = false;
        liveClockHandler.removeCallbacks(liveClockRunnable);
        autoDetectTimeoutHandler.removeCallbacksAndMessages(null);
    }
}
