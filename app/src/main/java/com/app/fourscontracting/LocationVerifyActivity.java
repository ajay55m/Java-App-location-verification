package com.app.fourscontracting;

import android.Manifest;
// ProgressDialog removed
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.airbnb.lottie.LottieAnimationView;
import java.util.Collections;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationVerifyActivity extends AppActivity {
    MaterialAutoCompleteTextView myProjectSpinner;
    Button btnVerify;
    private ProgressBar progressProjectsLoading;
    private TextView tvGpsLat;
    private TextView tvGpsLng;
    private TextView tvVerifyProgressPct;
    private ProgressBar progressVerifyBar;

    private void updateGpsDisplay(Location loc) {
        if (loc == null) return;
        runOnUiThread(() -> {
            if (tvGpsLat != null) {
                tvGpsLat.setText(String.format(Locale.US, "+%.5f° N", loc.getLatitude()));
            }
            if (tvGpsLng != null) {
                tvGpsLng.setText(String.format(Locale.US, "+%.5f° E", loc.getLongitude()));
            }
            if (tvVerifyProgressPct != null && progressVerifyBar != null) {
                int pct = Math.min(100, (int) (100.0f * Math.min(1.0f, 50.0f / Math.max(1.0f, loc.getAccuracy()))));
                pct = Math.max(30, pct);
                tvVerifyProgressPct.setText(pct + "%");
                progressVerifyBar.setProgress(pct);
            }
        });
    }
    List<JSONObject> locationData = new ArrayList<>();
    String empId = "", empName = "", projectId = "", deptId = "", type = "IN", managerUid = "";
    String selectedProjectId = "";
    private boolean isInitialVerify = false;
    private String lockedLocationId = null;
    private FusedLocationProviderClient fusedLocationClient;
    private LoadingManager loadingManager;

    // Hybrid Sampling System Fields
    private final List<Location> locationSamples = new ArrayList<>();
    private boolean isSampling = false;
    /** Max GPS reported-accuracy to accept a fix (lower = stricter). Raised to 55m to handle first-fix jitter indoors. */
    private static final float ACCURACY_GUARD = 55.0f;
    /** Base geofence radius in metres. */
    private static final float GEOFENCE_RADIUS_M = 50.0f;
    /** Extra buffer added to the geofence radius equal to GPS accuracy (capped). Raised so jitter on 1st/2nd attempt does not cause false failures. */
    private static final float GEOFENCE_ACCURACY_BUFFER_CAP_M = 35.0f;
    /** Total sampling window in ms. Extended to 15 s so GPS has time to stabilise on the first two attempts. */
    private static final long SAMPLING_DURATION = 15000;
    /** Early-exit accuracy threshold (lower = better fix). Relaxed to 25m — realistic outdoor GPS. */
    private static final float EARLY_EXIT_ACCURACY_M = 25.0f;
    /** Require only 1 good sample to early-exit — waiting for 2 was causing the 10 s timeout on attempts 1 & 2. */
    private static final int EARLY_EXIT_MIN_GOOD_SAMPLES = 1;
    private int goodAccuracySampleCount = 0;
    private LocationCallback locationCallback;
    /** Separate callback for silent GPS pre-warm — runs from onCreate until Verify is tapped. */
    private LocationCallback warmUpCallback;
    private android.os.Handler samplingHandler;
    private Runnable samplingTimeoutRunnable;

    // Fallback LocationManager tracking fields
    private LocationManager fallbackLocationManager;
    private LocationListener fallbackListener;
    private Runnable fallbackTimeoutRunnable;
    private android.os.CountDownTimer countDownTimer;

    private void cancelCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void stopProgressAnimation() {
        if (verifyProgress != null) {
            verifyProgress.clearAnimation();
            android.view.View icon = verifyProgress.findViewById(R.id.construction_icon);
            if (icon != null) {
                icon.clearAnimation();
            }
        }
    }

    // Blueprint Overlay Fields
    private View inputSelectionCard;
    private View blueprintOverlay;
    private android.view.View verifyProgress;
    private android.widget.ImageView verifyStatusIcon;
    private android.view.View verifyStatusBadge;
    private TextView blueprintStatus;
    private TextView tvAccuracyBadge;
    private TextView tvSelectedLocation;
    private View containerSuccess, containerFailure;
    private Button btnBlueprintProceed, btnBlueprintRetry, btnBlueprintLeave, btnBlueprintReselect;

    private TextView tvVerifyUserName;
    private TextView tvVerifyDateTime;
    private TextView tvVerifyProjectLocation;
    private TextView tvBlueprintEmpName;
    private TextView tvBlueprintDateTime;
    private TextView tvBlueprintProjectLocation;
    private View cardBlueprintEmpAvatar;
    private ImageView imgBlueprintEmpPhoto;
    private View imgBlueprintEmpAvatarFallback;
    private TextView tvBlueprintEmpSubtitle;
    private String photoUrl = "";
    private final android.os.Handler liveClockHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable liveClockRunnable = new Runnable() {
        @Override
        public void run() {
            String currentDateTime = new java.text.SimpleDateFormat("EEE, dd MMM yyyy | hh:mm:ss a", java.util.Locale.getDefault()).format(new java.util.Date());
            if (tvVerifyDateTime != null) {
                tvVerifyDateTime.setText(currentDateTime);
            }
            if (tvBlueprintDateTime != null) {
                tvBlueprintDateTime.setText(currentDateTime);
            }
            liveClockHandler.postDelayed(this, 1000);
        }
    };

    private boolean isAutoStartVerify = false;
    private static final int REQUEST_CAPTURE_PHOTO = 1002;
    private String targetLocationId = null;
    private String pendingEid = null;
    private String pendingAction = null;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saved_emp_id", empId);
        outState.putString("saved_emp_name", empName);
        outState.putString("saved_photo_url", photoUrl);
        outState.putString("saved_project_id", projectId);
        outState.putString("saved_dept_id", deptId);
        outState.putString("saved_type", type);
        outState.putString("saved_selected_project_id", selectedProjectId);
        outState.putString("saved_manager_uid", managerUid);
        outState.putBoolean("saved_is_initial_verify", isInitialVerify);
        outState.putString("saved_locked_location_id", lockedLocationId);
        outState.putBoolean("saved_is_auto_start_verify", isAutoStartVerify);
        outState.putString("saved_target_location_id", targetLocationId);
        outState.putString("saved_pending_eid", pendingEid);
        outState.putString("saved_pending_action", pendingAction);
    }

    private void getIncomingData() {
        Intent intent = getIntent();
        if (intent != null) {
            isInitialVerify = intent.getBooleanExtra("IS_INITIAL_VERIFY", false);
            lockedLocationId = intent.getStringExtra("LOCKED_LOCATION_ID");
            isAutoStartVerify = intent.getBooleanExtra("AUTO_START_VERIFY", false);
            if (isAutoStartVerify) {
                targetLocationId = intent.getStringExtra("target_location_id");
                pendingEid = intent.getStringExtra("pending_eid");
                pendingAction = intent.getStringExtra("pending_action");
                lockedLocationId = targetLocationId;
                selectedProjectId = intent.getStringExtra("selected_project_id");
            }

            empId = intent.getStringExtra("empid") != null ? intent.getStringExtra("empid") : "";
            empName = intent.getStringExtra("emp_name");
            if (empName == null || empName.isEmpty()) empName = intent.getStringExtra("empname");
            if (empName == null || empName.isEmpty()) empName = intent.getStringExtra("employee_name");
            if (empName == null || empName.isEmpty()) empName = intent.getStringExtra("name");
            if (empName == null) empName = "";

            photoUrl = intent.getStringExtra("photo_url");
            if (photoUrl == null || photoUrl.isEmpty()) photoUrl = intent.getStringExtra("photo");
            if (photoUrl == null || photoUrl.isEmpty()) photoUrl = intent.getStringExtra("emp_photo");
            if ((photoUrl == null || photoUrl.isEmpty()) && empId != null && !empId.isEmpty()) {
                photoUrl = com.app.fourscontracting.data.ApiConfig.SUBCONTRACTOR + "/get_photo.php?id=" + empId;
            }
            projectId = intent.getStringExtra("projname") != null ? intent.getStringExtra("projname") : "";
            managerUid = intent.getStringExtra("uid") != null ? intent.getStringExtra("uid") : "";
            type = intent.getStringExtra("type") != null ? intent.getStringExtra("type") : "IN";
            if (!isAutoStartVerify && !isInitialVerify && ("IN".equalsIgnoreCase(type) || "MOVE".equalsIgnoreCase(type))) {
                boolean fromMenu = intent.getBooleanExtra("FROM_MOVE_MENU", false);
                if ("MOVE".equalsIgnoreCase(type)) {
                    Toast.makeText(this,
                            fromMenu
                                    ? "Select and verify the NEW site for this worker move."
                                    : "Please ensure you have selected the correct NEW site before verifying.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Please ensure you have selected the correct NEW site in the dashboard before verifying.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_location_verify);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F8FAFC"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }

            myProjectSpinner = findViewById(R.id.myProjectSpinner);
            btnVerify = findViewById(R.id.btnVerify);
            inputSelectionCard = findViewById(R.id.inputSelectionCard);
            progressProjectsLoading = findViewById(R.id.progress_projects_loading);
            tvVerifyUserName = findViewById(R.id.tv_verify_user_name);
            tvVerifyDateTime = findViewById(R.id.tv_verify_datetime);
            tvVerifyProjectLocation = findViewById(R.id.tv_verify_project_location);
            tvGpsLat = findViewById(R.id.tv_gps_lat);
            tvGpsLng = findViewById(R.id.tv_gps_lng);
            tvVerifyProgressPct = findViewById(R.id.tv_verify_progress_pct);
            progressVerifyBar = findViewById(R.id.progress_verify_bar);

            bindVerifyUserInfo();
            startLiveClock();

            if (myProjectSpinner == null || btnVerify == null) {
                throw new NullPointerException("XML Error: myProjectSpinner or btnVerify not found in activity_location_verify.xml");
            }

            if (savedInstanceState != null) {
                empId = savedInstanceState.getString("saved_emp_id", "");
                empName = savedInstanceState.getString("saved_emp_name", "");
                photoUrl = savedInstanceState.getString("saved_photo_url", "");
                projectId = savedInstanceState.getString("saved_project_id", "");
                deptId = savedInstanceState.getString("saved_dept_id", "");
                type = savedInstanceState.getString("saved_type", "IN");
                selectedProjectId = savedInstanceState.getString("saved_selected_project_id", "");
                managerUid = savedInstanceState.getString("saved_manager_uid", "");
                isInitialVerify = savedInstanceState.getBoolean("saved_is_initial_verify", false);
                lockedLocationId = savedInstanceState.getString("saved_locked_location_id", null);
                isAutoStartVerify = savedInstanceState.getBoolean("saved_is_auto_start_verify", false);
                targetLocationId = savedInstanceState.getString("saved_target_location_id", null);
                pendingEid = savedInstanceState.getString("saved_pending_eid", null);
                pendingAction = savedInstanceState.getString("saved_pending_action", null);
            } else {
                getIncomingData();
            }

            getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    SessionPrefs session = new SessionPrefs(LocationVerifyActivity.this);
                    if (!session.isLocationSessionValid()) {
                        Toast.makeText(LocationVerifyActivity.this, "Supervisor location verification is required.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            });

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // ── GPS Pre-Warm ──────────────────────────────────────────────────────────
            // Start collecting GPS samples silently while the user selects a project.
            // By the time they tap Verify the chip is warm and samples are ready.
            // No background permission needed — this Activity is in the foreground.
            startGpsPreWarm();
            // ─────────────────────────────────────────────────────────────────────────

            loadingManager = new LoadingManager();

            blueprintOverlay = findViewById(R.id.blueprint_skeleton_overlay);
            if (blueprintOverlay != null) {
                verifyProgress = blueprintOverlay.findViewById(R.id.verify_progress);
                verifyStatusIcon = blueprintOverlay.findViewById(R.id.verify_status_icon);
                verifyStatusBadge = blueprintOverlay.findViewById(R.id.verify_status_icon_badge);
                blueprintStatus = blueprintOverlay.findViewById(R.id.blueprint_status);
                tvAccuracyBadge = blueprintOverlay.findViewById(R.id.tv_accuracy_badge);
                containerSuccess = blueprintOverlay.findViewById(R.id.container_blueprint_success);
                containerFailure = blueprintOverlay.findViewById(R.id.container_blueprint_failure);
                btnBlueprintProceed = blueprintOverlay.findViewById(R.id.btn_blueprint_proceed);
                btnBlueprintRetry = blueprintOverlay.findViewById(R.id.btn_blueprint_retry);
                btnBlueprintLeave = blueprintOverlay.findViewById(R.id.btn_blueprint_leave);
                btnBlueprintReselect = blueprintOverlay.findViewById(R.id.btn_blueprint_reselect);
                tvSelectedLocation = blueprintOverlay.findViewById(R.id.blueprint_selected_location);
                tvBlueprintEmpName = blueprintOverlay.findViewById(R.id.tv_blueprint_emp_name);
                tvBlueprintDateTime = blueprintOverlay.findViewById(R.id.tv_blueprint_datetime);
                tvBlueprintProjectLocation = blueprintOverlay.findViewById(R.id.tv_blueprint_project_location);
                cardBlueprintEmpAvatar = blueprintOverlay.findViewById(R.id.card_blueprint_emp_avatar);
                imgBlueprintEmpPhoto = blueprintOverlay.findViewById(R.id.img_blueprint_emp_photo);
                imgBlueprintEmpAvatarFallback = blueprintOverlay.findViewById(R.id.img_blueprint_emp_avatar_fallback);
                tvBlueprintEmpSubtitle = blueprintOverlay.findViewById(R.id.tv_blueprint_emp_subtitle);
                View btnTopBack = blueprintOverlay.findViewById(R.id.btn_blueprint_top_back);
                if (btnTopBack != null) {
                    btnTopBack.setOnClickListener(v -> finish());
                }
                if (cardBlueprintEmpAvatar != null) {
                    cardBlueprintEmpAvatar.setOnClickListener(v -> showWhatsAppProfileView());
                }
            }
            bindVerifyUserInfo();
            updateSelectedLocationDisplay();

            if (isAutoStartVerify) {
                if (inputSelectionCard != null) {
                    inputSelectionCard.setVisibility(View.GONE);
                }
                if (blueprintOverlay != null) {
                    blueprintOverlay.setVisibility(View.VISIBLE);
                    
                    // Show progress and hide checkmark/error icon
                    if (verifyProgress != null) {
                        verifyProgress.setVisibility(View.VISIBLE);
                        verifyProgress.clearAnimation();
                        verifyProgress.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_rotate_loading));
                        android.view.View icon = verifyProgress.findViewById(R.id.construction_icon);
                        if (icon != null) {
                            icon.clearAnimation();
                            icon.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_pulse_loading));
                        }
                    }
                    if (verifyStatusIcon != null) {
                        verifyStatusIcon.setVisibility(View.GONE);
                    }
                    
                    // Text & Badge
                    if (blueprintStatus != null) {
                        blueprintStatus.setText("LOADING SITE COORDINATES...");
                        blueprintStatus.setTextColor(android.graphics.Color.parseColor("#0EA5E9"));
                    }
                    updateSelectedLocationDisplay();
                    if (tvAccuracyBadge != null) {
                        tvAccuracyBadge.setVisibility(View.GONE);
                    }
                    
                    // Hide action containers
                    if (containerSuccess != null) containerSuccess.setVisibility(View.GONE);
                    if (containerFailure != null) containerFailure.setVisibility(View.GONE);
                }
            }

            if (isAutoStartVerify) {
                if (selectedProjectId == null || selectedProjectId.isEmpty()) {
                    selectedProjectId = new SessionPrefs(this).getProjectId();
                }
                if (selectedProjectId != null && !selectedProjectId.isEmpty()) {
                    fetchGeofenceBoundaries(selectedProjectId);
                } else {
                    showBlueprintFailure(AppMessages.PROJECT_SESSION_MISSING, 0.0f);
                }
            } else {
                loadAllProjectNames();
            }
            btnVerify.setEnabled(false); // always start disabled

            btnVerify.setOnClickListener(v -> performGpsCheck());
        } catch (Exception e) {
            android.util.Log.e("APP_ERROR", "Crash in LocationVerify", e);
            Toast.makeText(this, "System Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    LocationVerifyActivity.this.finish();
                }
            }, 5000);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getIncomingData();
        bindVerifyUserInfo();
        if (myProjectSpinner != null) {
            loadAllProjectNames();
        }
    }

    private void bindVerifyUserInfo() {
        String displayName = "";
        boolean isSelf = false;
        UserLocalStore userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String val = (user != null && user.username != null) ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        String selfName = (val_list.length > 2 && !val_list[2].isEmpty()) ? val_list[2] : "";

        if (empName != null && !empName.trim().isEmpty()) {
            displayName = empName.trim();
            if (!selfName.isEmpty() && displayName.equalsIgnoreCase(selfName.trim())) {
                isSelf = true;
            }
        } else {
            displayName = !selfName.isEmpty() ? selfName : "Supervisor";
            isSelf = true;
        }

        String formattedName = isSelf ? displayName + " (You)" : displayName;

        if (tvVerifyUserName != null) {
            tvVerifyUserName.setText(displayName);
        }
        if (tvBlueprintEmpName != null) {
            tvBlueprintEmpName.setText(formattedName);
        }
        if (tvBlueprintEmpSubtitle != null) {
            if (empId != null && !empId.trim().isEmpty()) {
                tvBlueprintEmpSubtitle.setText("Worker ID: #" + empId.trim());
            } else {
                tvBlueprintEmpSubtitle.setText("Refining GPS Coordinates");
            }
        }
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            com.app.fourscontracting.data.ImageLoaderHelper.loadImage(this, photoUrl, imgBlueprintEmpPhoto, imgBlueprintEmpAvatarFallback);
        }
        if (blueprintOverlay != null) {
            TextView tvCardWorkerId = blueprintOverlay.findViewById(R.id.tv_card_worker_id);
            if (tvCardWorkerId != null) {
                if (empId != null && !empId.trim().isEmpty()) {
                    tvCardWorkerId.setText("#" + empId.trim());
                } else {
                    tvCardWorkerId.setText("#--");
                }
            }
        }
    }

    private void startLiveClock() {
        liveClockHandler.removeCallbacks(liveClockRunnable);
        liveClockHandler.post(liveClockRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLiveClock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        liveClockHandler.removeCallbacks(liveClockRunnable);
    }

    private void loadAllProjectNames() {
        UserLocalStore userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String val = (user != null && user.username != null) ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        String uid = val_list.length > 0 ? val_list[0] : "";

        // Instant UI load from local cache or assigned projects (0ms delay)
        SessionPrefs session = new SessionPrefs(this);
        java.util.List<Project> cached = session.getCachedProjectList(true);
        if (!cached.isEmpty()) {
            bindProjectDropdown(cached);
            autoSelectIfNeeded(cached);
        } else {
            List<Project> instantLocal = getInstantLocalProjects(val_list);
            if (!instantLocal.isEmpty()) {
                bindProjectDropdown(instantLocal);
                autoSelectIfNeeded(instantLocal);
            } else if (myProjectSpinner != null) {
                myProjectSpinner.setText("Loading projects…", false);
                myProjectSpinner.setEnabled(false);
            }
        }

        if (progressProjectsLoading != null) {
            progressProjectsLoading.setVisibility(View.VISIBLE);
        }

        if (uid.isEmpty()) {
            if (progressProjectsLoading != null) {
                progressProjectsLoading.setVisibility(View.GONE);
            }
            useFallbackLocalProjects(val_list);
            return;
        }

        // Silent background update from server
        String getProjectsUrl = "https://4scontracting.com/SMCS_APP/fcm_app/getprojects.php?uid=" + uid;

        JsonObjectRequest getProjectsRequest = new JsonObjectRequest(Request.Method.GET, getProjectsUrl, null,
            response -> {
                if (progressProjectsLoading != null) {
                    progressProjectsLoading.setVisibility(View.GONE);
                }
                try {
                    String status = response.optString("status", "");
                    if (!"success".equalsIgnoreCase(status)) {
                        if (cached.isEmpty()) useFallbackLocalProjects(val_list);
                        return;
                    }

                    JSONArray projectsArray = response.optJSONArray("projects");
                    List<String> newProjectsList = new ArrayList<>();
                    List<Project> projectList = new ArrayList<>();
                    if (projectsArray != null) {
                        for (int i = 0; i < projectsArray.length(); i++) {
                            JSONObject projObj = projectsArray.optJSONObject(i);
                            if (projObj == null) continue;
                            String proj = projObj.optString("projname", "");
                            String id = projObj.optString("id", "");
                            if (proj.isEmpty()) continue;
                            newProjectsList.add(proj);
                            projectList.add(new Project(id, proj, projObj.optString("break_hours", "")));
                        }
                    }

                    userLocalStore.updateAssignedProjects(newProjectsList, this);

                    if (projectList.isEmpty()) {
                        if (cached.isEmpty()) useFallbackLocalProjects(val_list);
                        return;
                    }

                    session.saveProjectListCache(projectList);
                    if (myProjectSpinner != null) myProjectSpinner.setEnabled(true);
                    bindProjectDropdown(projectList);
                    autoSelectIfNeeded(projectList);

                    // Optional: enrich break_hours in background (does not block dropdown)
                    enrichBreakHoursInBackground(projectList);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (cached.isEmpty()) useFallbackLocalProjects(val_list);
                }
            },
            error -> {
                if (progressProjectsLoading != null) {
                    progressProjectsLoading.setVisibility(View.GONE);
                }
                error.printStackTrace();
                if (cached.isEmpty()) {
                    useFallbackLocalProjects(val_list);
                } else if (myProjectSpinner != null) {
                    myProjectSpinner.setEnabled(true);
                }
            }
        );

        getProjectsRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                8000,
                1,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        MySingleton.getmInstance(this).addToRequestque(getProjectsRequest);
    }

    private List<Project> getInstantLocalProjects(String[] val_list) {
        List<Project> list = new ArrayList<>();
        String assignedProjectsStr = val_list.length > 3 ? val_list[3] : "";
        if (!assignedProjectsStr.isEmpty()) {
            for (String p : assignedProjectsStr.split(",")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    list.add(new Project(trimmed, trimmed, ""));
                }
            }
        }
        return list;
    }

    private void autoSelectIfNeeded(List<Project> projectList) {
        if (projectList == null || projectList.isEmpty()) return;
        if (projectId == null || projectId.isEmpty()) return;

        int index = -1;
        for (int i = 0; i < projectList.size(); i++) {
            if (projectList.get(i).name.trim().equalsIgnoreCase(projectId.trim())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            if (isAutoStartVerify) {
                showBlueprintFailure("Project details not found: " + projectId, 0.0f);
            } else {
                Toast.makeText(this, "Select your project to continue", Toast.LENGTH_SHORT).show();
                btnVerify.setEnabled(false);
            }
            return;
        }

        Project selectedProject = projectList.get(index);
        myProjectSpinner.setText(selectedProject.name, false);
        selectedProjectId = selectedProject.id;
        projectId = selectedProject.name;
        new SessionPrefs(this).saveProject(selectedProject.id, selectedProject.name, selectedProject.breakHours);
        fetchGeofenceBoundaries(selectedProjectId);
    }

    /** Soft merge break_hours from get_project_name.php — dropdown already visible. */
    private void enrichBreakHoursInBackground(final List<Project> projectList) {
        String url = "https://4scontracting.com/SMCS_APP/fcm_app/get_project_name.php";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    java.util.Map<String, String> breaks = new java.util.HashMap<>();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj == null) continue;
                        String name = obj.optString("projname", "").trim().toLowerCase();
                        if (name.isEmpty()) continue;
                        String bh = obj.optString("break_hours", "");
                        if (bh.isEmpty()) bh = obj.optString("city", "");
                        if (bh.isEmpty()) bh = obj.optString("district", "");
                        if (!bh.isEmpty()) breaks.put(name, bh);
                    }
                    boolean changed = false;
                    for (Project p : projectList) {
                        if (p == null || p.name == null) continue;
                        if (p.breakHours != null && !p.breakHours.isEmpty()) continue;
                        String bh = breaks.get(p.name.trim().toLowerCase());
                        if (bh != null && !bh.isEmpty()) {
                            p.breakHours = bh;
                            changed = true;
                        }
                    }
                    if (changed) {
                        new SessionPrefs(this).saveProjectListCache(projectList);
                    }
                } catch (Exception ignored) {
                }
            },
            error -> { /* ignore — dropdown already works */ }
        );
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000, 0, 1f));
        MySingleton.getmInstance(this).addToRequestque(request);
    }

    private void useFallbackLocalProjects(String[] val_list) {
        String assignedProjectsStr = val_list.length > 3 ? val_list[3] : "";
        final java.util.Set<String> assignedProjects = new java.util.HashSet<>();
        if (!assignedProjectsStr.isEmpty()) {
            for (String p : assignedProjectsStr.split(",")) {
                assignedProjects.add(p.trim().toLowerCase());
            }
        }
        // Last resort: metadata API (slower). Prefer cache/getprojects path above.
        fetchProjectsMetadata(assignedProjects);
    }

    private void fetchProjectsMetadata(final java.util.Set<String> assignedProjects) {
        if (progressProjectsLoading != null) {
            progressProjectsLoading.setVisibility(View.VISIBLE);
        }
        String url = "https://4scontracting.com/SMCS_APP/fcm_app/get_project_name.php";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                if (progressProjectsLoading != null) {
                    progressProjectsLoading.setVisibility(View.GONE);
                }
                try {
                    List<Project> allProjects = new ArrayList<>();
                    List<Project> projectList = new ArrayList<>();

                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        String projname = obj.optString("projname", "");
                        if (projname.isEmpty()) continue;

                        String breakHoursVal = obj.optString("break_hours", "");
                        if (breakHoursVal.isEmpty()) {
                            breakHoursVal = obj.optString("city", "");
                        }
                        if (breakHoursVal.isEmpty()) {
                            breakHoursVal = obj.optString("district", "");
                        }
                        Project p = new Project(
                                obj.optString("id", ""),
                                projname,
                                breakHoursVal
                        );
                        allProjects.add(p);

                        // Filter: show only projects assigned to the user (when we know assignments)
                        if (assignedProjects == null || assignedProjects.isEmpty()
                                || assignedProjects.contains(projname.trim().toLowerCase())) {
                            projectList.add(p);
                        }
                    }

                    // If assignment filter wiped the list but server returned projects, show all
                    // (avoids empty dropdown on Poco/MIUI when assignment names don't match exactly)
                    if (projectList.isEmpty() && !allProjects.isEmpty()) {
                        projectList.addAll(allProjects);
                        Toast.makeText(this,
                                "Could not match assigned projects. Showing all projects — pick yours.",
                                Toast.LENGTH_LONG).show();
                    }

                    if (projectList.isEmpty()) {
                        if (isAutoStartVerify) {
                            showBlueprintFailure("No projects available for this account.", 0.0f);
                        } else {
                            Toast.makeText(this, "No projects found. Tap Verify card after retry.", Toast.LENGTH_LONG).show();
                            showRetryProjectsDialog();
                        }
                        return;
                    }

                    bindProjectDropdown(projectList);
                    new SessionPrefs(this).saveProjectListCache(projectList);
                    autoSelectIfNeeded(projectList);

                } catch (JSONException e) {
                    e.printStackTrace();
                    if (isAutoStartVerify) {
                        showBlueprintFailure("Failed to read project list.", 0.0f);
                    } else {
                        Toast.makeText(this, "Error reading projects. Retry.", Toast.LENGTH_LONG).show();
                        showRetryProjectsDialog();
                    }
                }
            },
            error -> {
                if (progressProjectsLoading != null) {
                    progressProjectsLoading.setVisibility(View.GONE);
                }
                if (isAutoStartVerify) {
                    showBlueprintFailure("Failed to load project details.", 0.0f);
                } else {
                    Toast.makeText(this, "Error loading projects", Toast.LENGTH_SHORT).show();
                    showRetryProjectsDialog();
                }
            }
        );

        MySingleton.getmInstance(this).addToRequestque(request);
    }

    /**
     * MIUI / Poco F5: default ArrayAdapter filtering + plain AutoCompleteTextView often
     * opens an empty popup. Use a non-filtering adapter and force showDropDown on tap.
     */
    private void bindProjectDropdown(final List<Project> projectList) {
        if (myProjectSpinner == null) return;
        if (projectList == null || projectList.isEmpty()) return;

        final List<Project> masterList = new ArrayList<>(projectList);

        final ArrayAdapter<Project> adapter = new ArrayAdapter<Project>(
                this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(masterList)) {
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = masterList;
                        results.count = masterList.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        // Do not clear/addAll/notifyDataSetChanged asynchronously here.
                        // The adapter already contains masterList, so updating it asynchronously
                        // triggers a dataset change that closes the dropdown popup on tap ("blink and go").
                    }
                };
            }
        };

        myProjectSpinner.setAdapter(adapter);
        myProjectSpinner.setEnabled(true);
        myProjectSpinner.setThreshold(1);
        myProjectSpinner.setKeyListener(null);
        // Keep current selection text if already chosen; otherwise clear loading placeholder
        String current = myProjectSpinner.getText() != null ? myProjectSpinner.getText().toString() : "";
        if (current.startsWith("Loading") || current.isEmpty()) {
            myProjectSpinner.setText("", false);
        } else {
            myProjectSpinner.setText(current, false);
        }

        View.OnClickListener openAll = v -> {
            if (!myProjectSpinner.isPopupShowing()) {
                myProjectSpinner.showDropDown();
            }
        };
        myProjectSpinner.setOnClickListener(openAll);

        com.google.android.material.textfield.TextInputLayout projectInputLayout = findViewById(R.id.project_input_layout);
        if (projectInputLayout != null) {
            projectInputLayout.setOnClickListener(openAll);
        }

        myProjectSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !myProjectSpinner.isPopupShowing()) {
                myProjectSpinner.post(myProjectSpinner::showDropDown);
            }
        });

        myProjectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (!(item instanceof Project)) return;
            Project selectedProject = (Project) item;
            LocationVerifyActivity.this.selectedProjectId = selectedProject.id;
            LocationVerifyActivity.this.projectId = selectedProject.name;
            myProjectSpinner.setText(selectedProject.name, false);
            new SessionPrefs(LocationVerifyActivity.this).saveProject(
                    selectedProject.id, selectedProject.name, selectedProject.breakHours);
            fetchGeofenceBoundaries(LocationVerifyActivity.this.selectedProjectId);
        });
    }

    private void fetchGeofenceBoundaries(String selectedProjectId) {
        final String projId = (selectedProjectId != null && !selectedProjectId.isEmpty())
                ? selectedProjectId
                : (this.selectedProjectId != null && !this.selectedProjectId.isEmpty())
                ? this.selectedProjectId
                : new SessionPrefs(this).getProjectId();

        locationData.clear();
        btnVerify.setEnabled(false);
        btnVerify.setText("Verify GPS Check");
        String url = "https://4scontracting.com/SMCS_APP/fcm_app/get_locations.php";
        StringRequest request = new StringRequest(Request.Method.POST, url, response -> {
            try {
                JSONArray array = new JSONArray(response);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    locationData.add(obj);
                }
                
                if (!locationData.isEmpty()) {
                    btnVerify.setEnabled(true);
                    if (isAutoStartVerify) {
                        performGpsCheck();
                    } else {
                        Toast.makeText(this, "Locations loaded. You can verify GPS now.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isAutoStartVerify) {
                        showBlueprintFailure(AppMessages.NO_SITE_LOCATIONS, 0.0f);
                    } else {
                        Toast.makeText(this, AppMessages.NO_SITE_LOCATIONS, Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAutoStartVerify) {
                    showBlueprintFailure("Error parsing site coordinates.", 0.0f);
                } else {
                    Toast.makeText(this, "Error parsing locations", Toast.LENGTH_SHORT).show();
                }
            }
        }, error -> {
            android.util.Log.e("LOCATION_VERIFY", "Failed to load locations: " + error.toString());
            if (isAutoStartVerify) {
                showBlueprintFailure("Failed to load locations from server.", 0.0f);
            } else {
                Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show();
                showRetryLocationsDialog(projId);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("project_id", projId != null ? projId : "");
                return params;
            }
        };

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000,
                2,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        MySingleton.getmInstance(this).addToRequestque(request);
    }

    private void showPreciseLocationRequiredDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Precise Location Required")
            .setMessage("This app requires precise GPS location to verify that you are on the construction site. Please enable \"Precise Location\" in the app settings.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }

    private void startBlueprintScanning() {
        if (inputSelectionCard != null) {
            inputSelectionCard.setVisibility(View.GONE);
        }
        if (blueprintOverlay != null) {
            blueprintOverlay.setVisibility(View.VISIBLE);
            
            // Show progress and hide checkmark/error icon
            if (verifyProgress != null) {
                verifyProgress.setVisibility(View.VISIBLE);
                verifyProgress.clearAnimation();
                verifyProgress.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_rotate_loading));
                android.view.View icon = verifyProgress.findViewById(R.id.construction_icon);
                if (icon != null) {
                    icon.clearAnimation();
                    icon.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_pulse_loading));
                }
            }
            if (verifyStatusIcon != null) {
                verifyStatusIcon.setVisibility(View.GONE);
            }
            if (verifyStatusBadge != null) {
                verifyStatusBadge.setVisibility(View.GONE);
            }
            
            // Text & Badge
            blueprintStatus.setText("DRAFTING COORDINATES...");
            blueprintStatus.setTextColor(android.graphics.Color.parseColor("#0EA5E9"));
            updateSelectedLocationDisplay();
            tvAccuracyBadge.setVisibility(View.GONE);
            
            // Hide action containers
            containerSuccess.setVisibility(View.GONE);
            containerFailure.setVisibility(View.GONE);
        }
    }

    private void showBlueprintSuccess(String matchType, String matchedPointId, String matchedPointName, double liveLat, double liveLon, float accuracy) {
        if (inputSelectionCard != null) {
            inputSelectionCard.setVisibility(View.GONE);
        }
        if (blueprintOverlay != null) {
            blueprintOverlay.setVisibility(View.VISIBLE);
            
            // Hide progress and show success checkmark icon
            if (verifyProgress != null) {
                stopProgressAnimation();
                verifyProgress.setVisibility(View.GONE);
            }
            if (verifyStatusBadge != null) {
                verifyStatusBadge.setVisibility(View.VISIBLE);
                if (verifyStatusIcon != null) {
                    verifyStatusIcon.setVisibility(View.VISIBLE);
                    verifyStatusIcon.setImageResource(R.drawable.ic_baseline_check_24);
                    verifyStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#16A34A")));
                }
                verifyStatusBadge.setScaleX(0f);
                verifyStatusBadge.setScaleY(0f);
                verifyStatusBadge.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                    .start();
            } else if (verifyStatusIcon != null) {
                verifyStatusIcon.setVisibility(View.VISIBLE);
                verifyStatusIcon.setImageResource(R.drawable.ic_baseline_check_24);
                verifyStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#22C55E")));
                
                // Spring overshoot animation on checkmark
                verifyStatusIcon.setScaleX(0f);
                verifyStatusIcon.setScaleY(0f);
                verifyStatusIcon.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                    .start();
            }
            
            // Text & Badge
            if (matchedPointName != null && !matchedPointName.trim().isEmpty() && !isNumeric(matchedPointName.trim())) {
                projectId = matchedPointName.trim();
            }
            blueprintStatus.setText("VERIFICATION SUCCESSFUL\n" + matchType);
            blueprintStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"));
            updateSelectedLocationDisplay();
            
            tvAccuracyBadge.setVisibility(View.VISIBLE);
            tvAccuracyBadge.setText("GPS ACCURACY: " + Math.round(accuracy) + "m");
            tvAccuracyBadge.setTextColor(android.graphics.Color.parseColor("#22C55E"));
            
            // Setup buttons
            containerSuccess.setVisibility(View.VISIBLE);
            containerFailure.setVisibility(View.GONE);
            
            // Generate Secure verification token & Save Session (24h site + 5min punch token)
            String secureToken = java.util.UUID.randomUUID().toString();
            new SessionPrefs(this).saveVerifiedLocation(matchedPointId, matchedPointName, secureToken);

            if (isAutoStartVerify) {
                blueprintStatus.setText("VERIFICATION SUCCESSFUL!\nReady to proceed.");
                btnBlueprintProceed.setOnClickListener(v -> {
                    Intent intent = new Intent(LocationVerifyActivity.this, LocationActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("empid", pendingEid != null ? pendingEid : empId);
                    extras.putString("type", pendingAction != null ? pendingAction : (type != null ? type : "IN"));
                    boolean movementPunch = "MOVE".equalsIgnoreCase(type)
                            || "MOVE".equalsIgnoreCase(pendingAction)
                            || getIntent().getBooleanExtra("is_movement", false);
                    extras.putBoolean("is_movement", movementPunch);
                    intent.putExtra("is_movement", movementPunch);
                    extras.putString("uid", managerUid);
                    
                    String targetDept = (selectedProjectId != null && !selectedProjectId.isEmpty()) ? selectedProjectId : projectId;
                    if (targetDept != null) {
                        extras.putString("project_id", targetDept);
                        extras.putString("departmentid", targetDept);
                    }
                    
                    extras.putString("VERIFIED", "true");
                    extras.putString("VERIFICATION_TOKEN", secureToken);
                    extras.putDouble("lat", liveLat);
                    extras.putDouble("lng", liveLon);
                    extras.putFloat("gps_accuracy", accuracy);
                    if (matchedPointId != null) {
                        extras.putString("MATCHED_LOC_ID", matchedPointId);
                        extras.putString("locationid", matchedPointId);
                        extras.putString("loc_id", matchedPointId);
                    }
                    
                    intent.putExtras(extras);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });
                return;
            }

            if (isInitialVerify) {
                setResult(RESULT_OK);
                blueprintStatus.setText("SUPERVISOR LOCATION VERIFIED!\nSession locked for 24 hours.");
                if (btnBlueprintProceed != null) {
                    btnBlueprintProceed.setText("PROCEED");
                }
                final Runnable finishTask = new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            finish();
                        }
                    }
                };
                final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(finishTask, 1500);

                btnBlueprintProceed.setOnClickListener(v -> {
                    handler.removeCallbacks(finishTask);
                    finishTask.run();
                });
                return;
            }
                
            final Runnable redirectTask = new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {
                        Intent intent = new Intent(LocationVerifyActivity.this, LocationActivity.class);
                        Bundle extras = new Bundle();
                        if (empId != null) extras.putString("empid", empId);
                        extras.putString("type", type != null ? type : "IN");
                        extras.putBoolean("is_movement", "MOVE".equalsIgnoreCase(type));
                        intent.putExtra("is_movement", "MOVE".equalsIgnoreCase(type));
                        if (managerUid != null) extras.putString("uid", managerUid);
                        
                        String targetDept = (selectedProjectId != null && !selectedProjectId.isEmpty()) ? selectedProjectId : projectId;
                        if (targetDept != null) {
                            extras.putString("project_id", targetDept);
                            extras.putString("departmentid", targetDept);
                        }
                        
                        extras.putString("VERIFIED", "true");
                        extras.putString("VERIFICATION_TOKEN", secureToken);
                        extras.putDouble("lat", liveLat);
                        extras.putDouble("lng", liveLon);
                        extras.putFloat("gps_accuracy", accuracy);
                        if (matchedPointId != null) {
                            extras.putString("MATCHED_LOC_ID", matchedPointId);
                            extras.putString("locationid", matchedPointId);
                            extras.putString("loc_id", matchedPointId);
                        }
                        
                        intent.putExtras(extras);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                }
            };
            
            final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(redirectTask, 1500);
            
            btnBlueprintProceed.setOnClickListener(v -> {
                handler.removeCallbacks(redirectTask);
                redirectTask.run();
            });
        }
    }

    private void showBlueprintFailure(String statusMessage, float accuracy) {
        if (inputSelectionCard != null) {
            inputSelectionCard.setVisibility(View.GONE);
        }
        if (blueprintOverlay != null) {
            blueprintOverlay.setVisibility(View.VISIBLE);
            
            // Hide progress and show red warning icon
            if (verifyProgress != null) {
                stopProgressAnimation();
                verifyProgress.setVisibility(View.GONE);
            }
            if (verifyStatusBadge != null) {
                verifyStatusBadge.setVisibility(View.VISIBLE);
            }
            if (verifyStatusIcon != null) {
                verifyStatusIcon.setVisibility(View.VISIBLE);
                verifyStatusIcon.setImageResource(R.drawable.ic_warning_red);
                verifyStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444")));
                
                // Shake animation on failure icon
                verifyStatusIcon.setScaleX(1.0f);
                verifyStatusIcon.setScaleY(1.0f);
                verifyStatusIcon.setTranslationX(0f);
                verifyStatusIcon.animate()
                    .translationXBy(15f)
                    .setDuration(50)
                    .withEndAction(() -> verifyStatusIcon.animate()
                        .translationXBy(-30f)
                        .setDuration(100)
                        .withEndAction(() -> verifyStatusIcon.animate()
                            .translationXBy(15f)
                            .setDuration(50)
                            .start())
                        .start())
                    .start();
            }
            
            // Text & Badge
            blueprintStatus.setText(statusMessage);
            blueprintStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
            updateSelectedLocationDisplay();
            
            if (accuracy > 0.0f) {
                tvAccuracyBadge.setVisibility(View.VISIBLE);
                tvAccuracyBadge.setText("GPS ACCURACY: " + Math.round(accuracy) + "m");
                tvAccuracyBadge.setTextColor(android.graphics.Color.parseColor("#EF4444"));
            } else {
                tvAccuracyBadge.setVisibility(View.GONE);
            }
            
            // Setup buttons
            containerSuccess.setVisibility(View.GONE);
            containerFailure.setVisibility(View.VISIBLE);
            
            btnBlueprintRetry.setOnClickListener(v -> {
                blueprintOverlay.setVisibility(View.GONE);
                performGpsCheck();
            });

            if (btnBlueprintReselect != null) {
                btnBlueprintReselect.setOnClickListener(v -> {
                    blueprintOverlay.setVisibility(View.GONE);
                    inputSelectionCard.setVisibility(View.VISIBLE);
                    isAutoStartVerify = false;
                    isInitialVerify = false;
                    loadAllProjectNames();
                });
            }
            
            btnBlueprintLeave.setOnClickListener(v -> {
                Intent intent = new Intent(LocationVerifyActivity.this, DashboardActivity.class);
                intent.putExtra("destination_id", R.id.nav_home);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private String getResolvedProjectName() {
        if (projectId != null && !projectId.trim().isEmpty() && !isNumeric(projectId.trim())) {
            return projectId.trim();
        }
        SessionPrefs session = new SessionPrefs(this);
        String savedName = session.getProjectName();
        if (savedName != null && !savedName.trim().isEmpty() && !isNumeric(savedName.trim())) {
            return savedName.trim();
        }
        UserLocalStore userLocalStore = new UserLocalStore(this);
        UserLocation loc = userLocalStore.getLoggedInUserLocation();
        if (loc != null && loc.locationname != null && !loc.locationname.trim().isEmpty() && !isNumeric(loc.locationname.trim())) {
            return loc.locationname.trim();
        }
        String targetId = (selectedProjectId != null && !selectedProjectId.isEmpty()) ? selectedProjectId : targetLocationId;
        if (targetId != null && !targetId.isEmpty()) {
            java.util.List<Project> cached = session.getCachedProjectList();
            for (Project p : cached) {
                if (p != null && (targetId.equalsIgnoreCase(p.id) || targetId.equalsIgnoreCase(p.name))) {
                    if (p.name != null && !p.name.trim().isEmpty()) {
                        return p.name.trim();
                    }
                }
            }
        }
        if (projectId != null && !projectId.trim().isEmpty()) {
            return isNumeric(projectId.trim()) ? "Site " + projectId.trim() : projectId.trim();
        }
        if (targetId != null && !targetId.trim().isEmpty()) {
            return "Site " + targetId.trim();
        }
        return "Location Verification";
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private void showWhatsAppProfileView() {
        try {
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_whatsapp_profile_view, null);
            ImageView imgDialogPhoto = dialogView.findViewById(R.id.img_dialog_photo);
            View imgDialogFallback = dialogView.findViewById(R.id.img_dialog_fallback);
            TextView tvDialogName = dialogView.findViewById(R.id.tv_dialog_name);
            TextView tvDialogSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
            TextView tvDialogLocation = dialogView.findViewById(R.id.tv_dialog_location);

            String name = tvBlueprintEmpName != null ? tvBlueprintEmpName.getText().toString() : "Employee";
            tvDialogName.setText(name);
            if (empId != null && !empId.isEmpty()) {
                tvDialogSubtitle.setText("Worker ID: #" + empId);
            } else {
                tvDialogSubtitle.setText("Supervisor / Worker Profile");
            }
            tvDialogLocation.setText("Location: " + getResolvedProjectName());

            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                com.app.fourscontracting.data.ImageLoaderHelper.loadImage(this, photoUrl, imgDialogPhoto, imgDialogFallback);
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSelectedLocationDisplay() {
        String siteText = getResolvedProjectName();

        if (tvSelectedLocation != null) {
            tvSelectedLocation.setText("Selected Site: " + siteText);
            tvSelectedLocation.setVisibility(View.VISIBLE);
        }
        if (tvBlueprintProjectLocation != null) {
            tvBlueprintProjectLocation.setText(siteText);
        }
        if (tvVerifyProjectLocation != null) {
            tvVerifyProjectLocation.setText("Project: " + siteText);
        }
    }

    private static final int REQUEST_CHECK_SETTINGS = 1001;

    private void performGpsCheck() {
        try {
            if (locationData.isEmpty()) {
                Toast.makeText(this, "No geofenced locations loaded for this project.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!AppPermissions.hasLocation(this)) {
                AppPermissions.ensureOrGuide(this, false, true);
                return;
            }

            boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasFineLocation && hasCoarseLocation) {
                showPreciseLocationRequiredDialog();
                return;
            }

            checkLocationSettings();
        } catch (Exception e) {
            e.printStackTrace();
            fallbackToLocationManager();
        }
    }

    private void checkLocationSettings() {
        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                1000
        ).build();

        com.google.android.gms.location.LocationSettingsRequest.Builder builder = new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        com.google.android.gms.location.SettingsClient client = com.google.android.gms.location.LocationServices.getSettingsClient(this);
        com.google.android.gms.tasks.Task<com.google.android.gms.location.LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new com.google.android.gms.tasks.OnSuccessListener<com.google.android.gms.location.LocationSettingsResponse>() {
            @Override
            public void onSuccess(com.google.android.gms.location.LocationSettingsResponse locationSettingsResponse) {
                startHybridVerification();
            }
        });

        task.addOnFailureListener(this, new com.google.android.gms.tasks.OnFailureListener() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Exception e) {
                if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                    try {
                        com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) e;
                        resolvable.startResolutionForResult(LocationVerifyActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (android.content.IntentSender.SendIntentException sendEx) {
                        startHybridVerification();
                    }
                } else {
                    startHybridVerification();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Location enabled. Starting verification...", Toast.LENGTH_SHORT).show();
                startHybridVerification();
            } else {
                Toast.makeText(this, AppMessages.GPS_DISABLED, Toast.LENGTH_LONG).show();
                showBlueprintFailure(AppMessages.GPS_DISABLED, 0.0f);
            }
        } else if (requestCode == REQUEST_CAPTURE_PHOTO) {
            if (resultCode == RESULT_OK && data != null) {
                final String photoPath = data.getStringExtra("photo_path");
                final String eid = data.getStringExtra("eid");
                final String action = data.getStringExtra("action");
                final String uid = data.getStringExtra("uid");
                final String projectId = data.getStringExtra("project_id");
                final String locationId = data.getStringExtra("location_id");
                final double lat = data.getDoubleExtra("lat", 0.0);
                final double lng = data.getDoubleExtra("lng", 0.0);
                
                if (photoPath == null || photoPath.isEmpty()) {
                    Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                submitAttendance(eid, action, uid, projectId, locationId, lat, lng, photoPath);
            } else {
                Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startHybridVerification() {
        if (locationData.isEmpty()) {
            Toast.makeText(this, "No geofenced locations loaded for this project.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop pre-warm before we take over with the full sampling request
        stopGpsPreWarm();

        // ── Fast-path: use pre-warm samples if they are good enough ───────────────
        // Count how many pre-warm samples already pass the early-exit accuracy bar.
        int preWarmGoodCount = 0;
        for (Location s : locationSamples) {
            if (s.getAccuracy() <= EARLY_EXIT_ACCURACY_M) preWarmGoodCount++;
        }
        boolean hasEnoughPreWarmSamples = !locationSamples.isEmpty()
                && preWarmGoodCount >= EARLY_EXIT_MIN_GOOD_SAMPLES;

        if (hasEnoughPreWarmSamples) {
            Log.d("GPS_PREWARM", "Pre-warm gave " + locationSamples.size()
                    + " samples (" + preWarmGoodCount + " good). Skipping wait.");
            // Jump straight to result — no 15 s wait
            isSampling = true;  // processBestLocation() guards on this
            if (blueprintStatus != null) blueprintStatus.setText("PROCESSING BEST LOCATION...");
            processBestLocation();
            return;
        }
        // ─────────────────────────────────────────────────────────────────────────

        // Normal path — start fresh sampling window
        startBlueprintScanning();
        if (blueprintStatus != null) {
            blueprintStatus.setText("REFINING SITE COORDINATES... (10s)");
        }

        locationSamples.clear();
        goodAccuracySampleCount = 0;
        isSampling = true;

        if (samplingHandler == null) {
            samplingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        } else {
            samplingHandler.removeCallbacksAndMessages(null);
        }

        cancelCountDown();
        countDownTimer = new android.os.CountDownTimer(SAMPLING_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = Math.max(1, Math.round(millisUntilFinished / 1000.0));
                if (blueprintStatus != null && isSampling) {
                    blueprintStatus.setText("REFINING SITE COORDINATES... (" + secondsRemaining + "s)");
                }
            }

            @Override
            public void onFinish() {
                if (blueprintStatus != null && isSampling) {
                    blueprintStatus.setText("PROCESSING BEST LOCATION...");
                }
            }
        }.start();

        samplingTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSampling) {
                    cancelCountDown();
                    processBestLocation();
                }
            }
        };
        samplingHandler.postDelayed(samplingTimeoutRunnable, SAMPLING_DURATION);

        try {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@androidx.annotation.NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            // Ignore cached/stale fixes — raised to 60 s so valid first-fixes are not discarded
                            long ageMs = Math.abs(System.currentTimeMillis() - location.getTime());
                            if (ageMs > 60000L) {
                                Log.d("GPS_SAMPLING", "Skipping stale sample ageMs=" + ageMs);
                                continue;
                            }
                            Log.d("GPS_SAMPLING", "Acquired sample: Lat=" + location.getLatitude() + " Lng=" + location.getLongitude() + " Accuracy=" + location.getAccuracy() + "m");
                            locationSamples.add(location);
                            updateGpsDisplay(location);

                            // Require 2 good samples so worker #2 / MOVE is not judged on one jumpy fix
                            if (location.getAccuracy() <= EARLY_EXIT_ACCURACY_M) {
                                goodAccuracySampleCount++;
                                if (goodAccuracySampleCount >= EARLY_EXIT_MIN_GOOD_SAMPLES) {
                                    Log.d("GPS_SAMPLING", "Stable good signal acquired, completing sampling");
                                    samplingHandler.removeCallbacks(samplingTimeoutRunnable);
                                    cancelCountDown();
                                    processBestLocation();
                                    break;
                                }
                            }
                        }
                    }
                }
            };

            com.google.android.gms.location.LocationRequest request = new com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    800  // poll every 800 ms — faster samples on first two attempts
            ).setMinUpdateIntervalMillis(400)
                    .setMaxUpdateAgeMillis(0)
                    .setWaitForAccurateLocation(false)  // don't block; collect and pick best
                    .build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(request, locationCallback, android.os.Looper.getMainLooper());
            } else {
                fallbackToLocationManager();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GPS_SAMPLING", "Failed to start requestLocationUpdates, falling back", e);
            fallbackToLocationManager();
        }
    }

    private void processBestLocation() {
        if (!isSampling) return;
        isSampling = false;
        cancelCountDown();

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (locationSamples.isEmpty()) {
            Log.d("GPS_SAMPLING", "No samples collected, falling back to LocationManager");
            fallbackToLocationManager();
            return;
        }

        // Find the sample with the lowest accuracy number (the most confident signal)
        Location bestLocation = Collections.min(locationSamples, new java.util.Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {
                return Float.compare(l1.getAccuracy(), l2.getAccuracy());
            }
        });

        float accuracy = bestLocation.getAccuracy();
        Log.d("GPS_SAMPLING", "Best sample selected: Accuracy=" + accuracy + "m");

        if (accuracy > ACCURACY_GUARD) {
            showBlueprintFailure(AppMessages.gpsWeakWithAccuracy(accuracy), accuracy);
        } else {
            // Process the best acquired location
            processFetchedLocation(bestLocation);
        }
    }

    private void fallbackToLocationManager() {
        try {
            fallbackLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (fallbackLocationManager == null) {
                showBlueprintFailure(AppMessages.GPS_SERVICES_UNAVAILABLE, 0.0f);
                return;
            }

            boolean isGpsEnabled = fallbackLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = fallbackLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsEnabled && !isNetworkEnabled) {
                showBlueprintFailure(AppMessages.GPS_DISABLED, 0.0f);
                return;
            }

            Location lastKnown = null;
            if (isNetworkEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastKnown = fallbackLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (lastKnown == null && isGpsEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastKnown = fallbackLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

            // Never accept a stale lastKnown for worker #2 / MOVE — force a fresh lock instead
            boolean lastKnownFresh = lastKnown != null
                    && Math.abs(System.currentTimeMillis() - lastKnown.getTime()) <= 30000L;

            if (lastKnownFresh) {
                processFetchedLocation(lastKnown);
            } else {
                // Cancel any pending fallback timeout first
                cancelFallbackTimeout();
                cancelCountDown();

                // Request a single update with timeout failsafe
                fallbackListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@androidx.annotation.NonNull Location location) {
                        cancelFallbackTimeout();
                        cancelCountDown();
                        if (fallbackLocationManager != null) {
                            fallbackLocationManager.removeUpdates(this);
                        }
                        fallbackListener = null;
                        processFetchedLocation(location);
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(String provider) {}
                    @Override public void onProviderDisabled(String provider) {}
                };

                // Setup timeout runnable
                fallbackTimeoutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (fallbackListener != null && fallbackLocationManager != null) {
                            fallbackLocationManager.removeUpdates(fallbackListener);
                            fallbackListener = null;
                            cancelCountDown();
                            showBlueprintFailure(AppMessages.GPS_TIMEOUT, 0.0f);
                        }
                    }
                };

                if (samplingHandler == null) {
                    samplingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                }
                samplingHandler.postDelayed(fallbackTimeoutRunnable, 10000); // 10 seconds failsafe

                countDownTimer = new android.os.CountDownTimer(10000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long secondsRemaining = Math.max(1, Math.round(millisUntilFinished / 1000.0));
                        if (blueprintStatus != null && fallbackListener != null) {
                            blueprintStatus.setText("ACQUIRING GPS LOCK... (" + secondsRemaining + "s)");
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (blueprintStatus != null && fallbackListener != null) {
                            blueprintStatus.setText("PROCESSING LOCATION...");
                        }
                    }
                }.start();

                if (isGpsEnabled && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fallbackLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, fallbackListener);
                } else if (isNetworkEnabled && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fallbackLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, fallbackListener);
                } else {
                    cancelFallbackTimeout();
                    fallbackListener = null;
                    showBlueprintFailure("Location Permission Denied", 0.0f);
                }
            }
        } catch (Exception e) {
            cancelFallbackTimeout();
            fallbackListener = null;
            showBlueprintFailure("Failed to retrieve GPS location: " + e.getMessage(), 0.0f);
        }
    }

    private void cancelFallbackTimeout() {
        if (samplingHandler != null && fallbackTimeoutRunnable != null) {
            samplingHandler.removeCallbacks(fallbackTimeoutRunnable);
            fallbackTimeoutRunnable = null;
        }
    }

    private void processFetchedLocation(Location location) {
        if (location != null) {
            // Anti-cheat verification (Mock location detection)
            boolean isMock = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                isMock = location.isMock();
            } else {
                isMock = location.isFromMockProvider();
            }

            if (isMock) {
                showBlueprintFailure(AppMessages.GPS_MOCK, location.getAccuracy());
                return;
            }

            // Chronological Stale Location age check — relaxed to 90 s so valid fixes aren't rejected
            long locationTime = location.getTime();
            long systemTime = System.currentTimeMillis();
            long ageInSeconds = Math.abs(systemTime - locationTime) / 1000;
            if (ageInSeconds > 90) {
                showBlueprintFailure(AppMessages.GPS_STALE, location.getAccuracy());
                return;
            }

            // Accuracy Guard: Relaxed to 40m threshold to resolve indoor coordinates fetching
            if (location.getAccuracy() > ACCURACY_GUARD) {
                showBlueprintFailure(AppMessages.gpsWeakWithAccuracy(location.getAccuracy()), location.getAccuracy());
                return;
            }

            double liveLat = location.getLatitude();
            double liveLon = location.getLongitude();
            Log.d("GPS_FIX", "Fetched Lat: " + liveLat + " Lng: " + liveLon + " Accuracy: " + location.getAccuracy());

            if (locationData.isEmpty()) {
                showBlueprintFailure(AppMessages.NO_SITE_LOCATIONS, location.getAccuracy());
                return;
            }

            boolean verified = false;
            String matchedPointId = "";
            String matchType = "";
            String matchedPointName = "";

            for (JSONObject loc : locationData) {
                try {
                    double dbLat = loc.getDouble("latitude");
                    double dbLon = loc.getDouble("longitude");
                    double maxLat = loc.optDouble("max_latitude", 0.0);
                    double maxLon = loc.optDouble("max_longtitude", 0.0);
                    if (maxLon == 0.0) {
                        maxLon = loc.optDouble("max_longitude", 0.0);
                    }
                    String tempId = loc.optString("projectid", loc.optString("id", ""));

                    // Step 4 logic: If lockedLocationId is specified, ONLY verify against that location
                    if (lockedLocationId != null && !lockedLocationId.trim().isEmpty()) {
                        if (!tempId.equalsIgnoreCase(lockedLocationId.trim())) {
                            continue;
                        }
                    }

                    boolean hasBoundaryBox = (maxLat != 0.0) && (maxLon != 0.0);

                    // 1. Calculate true site boundaries from DB latitude/longitude & max_latitude/max_longtitude
                    double minLat = hasBoundaryBox ? Math.min(dbLat, maxLat) : dbLat;
                    double maxLatBound = hasBoundaryBox ? Math.max(dbLat, maxLat) : dbLat;
                    double minLon = hasBoundaryBox ? Math.min(dbLon, maxLon) : dbLon;
                    double maxLonBound = hasBoundaryBox ? Math.max(dbLon, maxLon) : dbLon;

                    double centerLat = hasBoundaryBox ? (minLat + maxLatBound) / 2.0 : dbLat;
                    double centerLon = hasBoundaryBox ? (minLon + maxLonBound) / 2.0 : dbLon;

                    // 2. Distance check from site center with 20m tolerance buffer
                    float[] centerDistResults = new float[1];
                    Location.distanceBetween(liveLat, liveLon, centerLat, centerLon, centerDistResults);
                    float distanceToCenter = centerDistResults[0];

                    float boxHalfDiagonal = 0f;
                    if (hasBoundaryBox) {
                        float[] diagResults = new float[1];
                        Location.distanceBetween(centerLat, centerLon, maxLatBound, maxLonBound, diagResults);
                        boxHalfDiagonal = diagResults[0];
                    }

                    // 20 meter tolerance buffer
                    float effectiveRadius = (hasBoundaryBox ? boxHalfDiagonal : 0f) + 20.0f;
                    boolean isCenterMatch = distanceToCenter <= effectiveRadius;

                    // 3. Bounding box range check with 20m tolerance buffer
                    double bufferMeters = 20.0; // 20m tolerance buffer
                    double latBuffer = bufferMeters / 111111.0;
                    double lngBuffer = bufferMeters / (111111.0 * Math.cos(Math.toRadians(liveLat)));

                    boolean isBoxMatch = hasBoundaryBox &&
                            (liveLat >= (minLat - latBuffer) && liveLat <= (maxLatBound + latBuffer) &&
                             liveLon >= (minLon - lngBuffer) && liveLon <= (maxLonBound + lngBuffer));

                    if (isCenterMatch || isBoxMatch) {
                        verified = true;
                        matchedPointId = tempId;
                        matchedPointName = loc.optString("locationname", loc.optString("location_name", loc.optString("name", "")));
                        if (matchedPointName.isEmpty()) {
                            matchedPointName = projectId != null ? projectId : "";
                        }
                        matchType = isBoxMatch ? "Authorized work area matched (boundary box)."
                                : "Site matched (" + Math.round(distanceToCenter) + "m from center, radius " + Math.round(effectiveRadius) + "m).";
                        Log.d("GPS", "Matched Site for ID " + tempId + ": dist=" + distanceToCenter + "m, boxMatch=" + isBoxMatch);
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String currentUserId = (empId != null && !empId.isEmpty()) ? empId : ((managerUid != null && !managerUid.isEmpty()) ? managerUid : "unknown");
            if (verified) {
                RuntimeLogger.log(this, currentUserId, "DEBUG", "Geofence", "MATCH: User successfully verified inside geofence at Point ID " + matchedPointId + " (" + matchType + ")");
                showBlueprintSuccess(matchType, matchedPointId, matchedPointName, liveLat, liveLon, location.getAccuracy());
            } else {
                RuntimeLogger.log(this, currentUserId, "DEBUG", "Geofence", "FAIL: User outside boundary (GPS: " + liveLat + ", " + liveLon + ")");
                if (lockedLocationId != null && !lockedLocationId.trim().isEmpty()) {
                    showBlueprintFailure("NOT AT LOCKED LOCATION\nYour location does not match the supervisor's verified site.", location.getAccuracy());
                } else {
                    showBlueprintFailure("OUTSIDE SITE BOUNDARY\nPlease move inside the authorized site and retry.", location.getAccuracy());
                }
            }
        } else {
            String currentUserId = (empId != null && !empId.isEmpty()) ? empId : ((managerUid != null && !managerUid.isEmpty()) ? managerUid : "unknown");
            RuntimeLogger.log(this, currentUserId, "ERROR", "Geofence", "FAIL: Unable to get current location");
            showBlueprintFailure("Unable to get current location. Ensure GPS is ON.", 0.0f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted, starting verification...", Toast.LENGTH_SHORT).show();
                performGpsCheck();
            } else {
                Toast.makeText(this, "Location permission is required for verification.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showRetryProjectsDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Connection Error")
            .setMessage("Failed to load projects from server. Please check your network connection.")
            .setPositiveButton("Retry", (dialog, which) -> {
                dialog.dismiss();
                loadAllProjectNames();
            })
            .setNegativeButton("Exit", (dialog, which) -> {
                dialog.dismiss();
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void showRetryLocationsDialog(String selectedProjId) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Connection Error")
            .setMessage("Failed to load geofenced boundaries for selected project.")
            .setPositiveButton("Retry", (dialog, which) -> {
                dialog.dismiss();
                fetchGeofenceBoundaries(selectedProjId);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setCancelable(false)
            .show();
    }

    private void submitAttendance(final String eid, final String action, final String uid, final String projectId,
                                  final String locationId, final double lat, final double lng, final String photoPath) {
        if (loadingManager != null) {
            loadingManager.showLoading(this);
        }
        new Thread(() -> {
            try {
                String boundary = "----AndroidBridgeBoundary";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                java.io.File file = new java.io.File(photoPath);
                if (!file.exists()) {
                    runOnUiThread(() -> {
                        if (loadingManager != null) loadingManager.hideLoading();
                        Toast.makeText(LocationVerifyActivity.this, "Photo file missing", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String urlString = "https://4scontracting.com/SMCS_APP/subcontractor/submit_attendance.php";
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(urlString).openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (java.io.DataOutputStream outputStream = new java.io.DataOutputStream(connection.getOutputStream())) {
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
                    try (java.io.FileInputStream fileInputStream = new java.io.FileInputStream(file)) {
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
                runOnUiThread(() -> {
                    if (loadingManager != null) {
                        loadingManager.hideLoading();
                    }
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        Toast.makeText(LocationVerifyActivity.this, "Attendance submitted", Toast.LENGTH_SHORT).show();
                        Intent intentHome = new Intent(LocationVerifyActivity.this, DashboardActivity.class);
                        intentHome.putExtra("attendance_submitted", true);
                        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intentHome);
                        finish();
                    } else {
                        Toast.makeText(LocationVerifyActivity.this, "Attendance submission failed", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (loadingManager != null) {
                        loadingManager.hideLoading();
                    }
                    Toast.makeText(LocationVerifyActivity.this, "Unable to submit attendance", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void writeFormField(java.io.DataOutputStream outputStream, String boundary, String fieldName, String value) throws Exception {
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n");
        outputStream.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        outputStream.writeBytes("\r\n");
    }

    /**
     * Starts a silent GPS pre-warm request from screen open.
     * Samples are stored in the shared {@code locationSamples} list so that
     * {@link #startHybridVerification()} can skip the 15 s wait if good fixes
     * are already available when the user taps Verify.
     */
    private void startGpsPreWarm() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return; // no permission yet — Verify button will handle it
        }
        try {
            locationSamples.clear();
            goodAccuracySampleCount = 0;

            warmUpCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@androidx.annotation.NonNull LocationResult result) {
                    if (isSampling) return; // actual verification already running — stop feeding
                    for (Location location : result.getLocations()) {
                        if (location == null) continue;
                        long ageMs = Math.abs(System.currentTimeMillis() - location.getTime());
                        if (ageMs > 60000L) continue; // skip stale fixes
                        locationSamples.add(location);
                        updateGpsDisplay(location);
                        Log.d("GPS_PREWARM", "Pre-warm sample: accuracy=" + location.getAccuracy() + "m age=" + ageMs + "ms");
                    }
                }
            };

            // Use BALANCED_POWER during pre-warm to save battery; HIGH_ACCURACY kicks in at verify time
            com.google.android.gms.location.LocationRequest preWarmRequest =
                    new com.google.android.gms.location.LocationRequest.Builder(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                            1500  // poll every 1.5 s — slower than verify, saves battery
                    ).setMinUpdateIntervalMillis(800)
                     .setMaxUpdateAgeMillis(0)
                     .setWaitForAccurateLocation(false)
                     .build();

            fusedLocationClient.requestLocationUpdates(
                    preWarmRequest, warmUpCallback, android.os.Looper.getMainLooper());
            Log.d("GPS_PREWARM", "Pre-warm started.");
        } catch (Exception e) {
            Log.e("GPS_PREWARM", "Failed to start pre-warm", e);
        }
    }

    /** Stops the silent pre-warm GPS request. Always call before starting actual verification. */
    private void stopGpsPreWarm() {
        if (warmUpCallback != null && fusedLocationClient != null) {
            try {
                fusedLocationClient.removeLocationUpdates(warmUpCallback);
                Log.d("GPS_PREWARM", "Pre-warm stopped. " + locationSamples.size() + " samples collected.");
            } catch (Exception e) {
                Log.e("GPS_PREWARM", "Error stopping pre-warm", e);
            }
            warmUpCallback = null;
        }
    }

    @Override
    protected void onDestroy() {
        cancelCountDown();
        stopGpsPreWarm(); // always release pre-warm on exit
        if (loadingManager != null) {
            loadingManager.hideLoading();
        }
        if (isSampling && fusedLocationClient != null && locationCallback != null) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (samplingHandler != null && samplingTimeoutRunnable != null) {
            samplingHandler.removeCallbacks(samplingTimeoutRunnable);
        }
        cancelFallbackTimeout();
        if (fallbackLocationManager != null && fallbackListener != null) {
            try {
                fallbackLocationManager.removeUpdates(fallbackListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            fallbackListener = null;
        }
        super.onDestroy();
    }
}
