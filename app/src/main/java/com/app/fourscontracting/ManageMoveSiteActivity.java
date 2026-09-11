package com.app.fourscontracting;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.ManageAttendanceApi;
import com.app.fourscontracting.data.MoveFeedAdapter;
import com.app.fourscontracting.data.MoveRecordModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageMoveSiteActivity extends AppActivity
        implements MoveFeedAdapter.Listener {

    private static final int REQUEST_CODE_MOVE_PUNCH = 1006;

    private UserLocalStore userLocalStore;
    private final ManageAttendanceApi api = new ManageAttendanceApi();

    private Spinner spinnerProjects;
    private Spinner spinnerDay;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    private TextView tvStatTotal;
    private TextView tvStatActive;
    private TextView tvStatCompleted;

    private MoveFeedAdapter moveAdapter;

    private String uid = "";
    private String selectedProjectId = "all";
    private String selDay = "";
    private String selMonth = "";
    private String selYear = "";
    private boolean projectSpinnerInitialized = false;
    private boolean isInitializingDateSpinners = true;
    private boolean isUpdatingProjects = false;

    private final List<AllocatedProjectModel> allocatedProjects = new ArrayList<>();
    private final List<MoveRecordModel> currentMoveList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_move_site);

        userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        uid = (val_list != null && val_list.length > 0) ? val_list[0] : "";

        if (getIntent() != null && getIntent().hasExtra("uid")) {
            String passedUid = getIntent().getStringExtra("uid");
            if (!TextUtils.isEmpty(passedUid)) {
                uid = passedUid;
            }
        }

        initViews();
        setupDateSpinners();
        prepopulateProjectsFromCache();

        moveAdapter = new MoveFeedAdapter(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(moveAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        loadData();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.img_back_button);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ImageView btnRefresh = findViewById(R.id.btn_refresh_feed);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadData());
        }

        spinnerProjects = findViewById(R.id.spinner_projects);
        spinnerDay = findViewById(R.id.spinner_day);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerView = findViewById(R.id.rv_manage_move_site);
        llEmptyState = findViewById(R.id.ll_empty_state);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        progressBar = findViewById(R.id.progress_manage_move_site);

        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatActive = findViewById(R.id.tv_stat_active);
        tvStatCompleted = findViewById(R.id.tv_stat_completed);
    }

    private void setupDateSpinners() {
        isInitializingDateSpinners = true;

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int currentYear = cal.get(Calendar.YEAR);

        selDay = String.format(Locale.US, "%02d", currentDay);
        selMonth = String.format(Locale.US, "%02d", currentMonth);
        selYear = String.valueOf(currentYear);

        List<String> days = new ArrayList<>();
        int dayIndexToSelect = 0;
        for (int i = 1; i <= 31; i++) {
            String d = String.format(Locale.US, "%02d", i);
            days.add(d);
            if (i == currentDay) {
                dayIndexToSelect = i - 1;
            }
        }
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);
        spinnerDay.setSelection(dayIndexToSelect, false);

        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        List<String> monthsList = new ArrayList<>();
        for (String m : monthNames) {
            monthsList.add(m);
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthsList);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(currentMonth - 1, false);

        List<String> years = new ArrayList<>();
        years.add(String.valueOf(currentYear - 1));
        years.add(String.valueOf(currentYear));
        years.add(String.valueOf(currentYear + 1));
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(1, false);

        AdapterView.OnItemSelectedListener dateChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializingDateSpinners) return;

                if (spinnerDay != null && spinnerMonth != null && spinnerYear != null && spinnerYear.getSelectedItem() != null) {
                    selDay = String.format(Locale.US, "%02d", spinnerDay.getSelectedItemPosition() + 1);
                    selMonth = String.format(Locale.US, "%02d", spinnerMonth.getSelectedItemPosition() + 1);
                    selYear = spinnerYear.getSelectedItem().toString();
                    loadData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerDay.setOnItemSelectedListener(dateChangeListener);
        spinnerMonth.setOnItemSelectedListener(dateChangeListener);
        spinnerYear.setOnItemSelectedListener(dateChangeListener);

        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().post(() -> isInitializingDateSpinners = false);
        } else {
            isInitializingDateSpinners = false;
        }
    }

    private List<MoveRecordModel> filterRecordsBySelectedProject(List<MoveRecordModel> inputRecords) {
        if (inputRecords == null) return new ArrayList<>();
        if ("all".equalsIgnoreCase(selectedProjectId) || TextUtils.isEmpty(selectedProjectId)) {
            return inputRecords;
        }

        String targetName = "";
        for (AllocatedProjectModel p : allocatedProjects) {
            if (selectedProjectId.equalsIgnoreCase(p.getId())) {
                targetName = p.getProjname() != null ? p.getProjname().trim().toLowerCase() : "";
                break;
            }
        }
        if (targetName.isEmpty()) {
            targetName = selectedProjectId.trim().toLowerCase();
        }

        List<MoveRecordModel> filtered = new ArrayList<>();
        for (MoveRecordModel record : inputRecords) {
            if (record == null) continue;
            String recProjName = record.getProjName() != null ? record.getProjName().trim().toLowerCase() : "";
            if (recProjName.contains(targetName) || targetName.contains(recProjName) || selectedProjectId.equalsIgnoreCase(record.getProjName())) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private void loadData() {
        if (isFinishing() || isDestroyed()) return;

        if (progressBar != null && swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        api.fetchFeed(this, uid, selectedProjectId, selDay, selMonth, selYear, new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName,
                                  List<AllocatedProjectModel> projects,
                                  List<com.app.fourscontracting.data.AttendanceRecordModel> attendanceRecords,
                                  List<MoveRecordModel> moveRecords) {
                if (isFinishing() || isDestroyed()) return;

                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                if (!projectSpinnerInitialized || (projects != null && projects.size() != (allocatedProjects.size() - 1))) {
                    updateProjectSpinner(projects);
                }

                currentMoveList.clear();
                if (moveRecords != null) {
                    List<MoveRecordModel> filtered = filterRecordsBySelectedProject(moveRecords);
                    currentMoveList.addAll(filtered);
                }
                if (moveAdapter != null) {
                    moveAdapter.setItems(currentMoveList);
                }

                updateEmptyStateView();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ManageMoveSiteActivity.this, message, Toast.LENGTH_SHORT).show();
                updateEmptyStateView();
            }
        });
    }

    private void prepopulateProjectsFromCache() {
        if (spinnerProjects == null) return;
        SessionPrefs session = new SessionPrefs(this);
        List<Project> cached = session.getCachedProjectList(true);
        if (cached != null && !cached.isEmpty()) {
            List<AllocatedProjectModel> allocList = new ArrayList<>();
            for (Project p : cached) {
                if (p != null && p.name != null && !p.name.isEmpty()) {
                    allocList.add(new AllocatedProjectModel(p.id != null ? p.id : p.name, p.name));
                }
            }
            updateProjectSpinner(allocList);
        }
    }

    private void updateProjectSpinner(List<AllocatedProjectModel> projects) {
        if (spinnerProjects == null) return;

        isUpdatingProjects = true;
        allocatedProjects.clear();
        allocatedProjects.add(new AllocatedProjectModel("all", "All Locations (Allocated)"));
        if (projects != null) {
            allocatedProjects.addAll(projects);
        }

        int selectedIndex = 0;
        if (!TextUtils.isEmpty(selectedProjectId)) {
            for (int i = 0; i < allocatedProjects.size(); i++) {
                if (selectedProjectId.equalsIgnoreCase(allocatedProjects.get(i).getId())) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        spinnerProjects.setOnItemSelectedListener(null);

        ArrayAdapter<AllocatedProjectModel> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allocatedProjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProjects.setAdapter(adapter);
        spinnerProjects.setSelection(selectedIndex, false);

        spinnerProjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingProjects) return;
                if (position >= 0 && position < allocatedProjects.size()) {
                    AllocatedProjectModel selected = allocatedProjects.get(position);
                    if (selected != null && !selectedProjectId.equalsIgnoreCase(selected.getId())) {
                        selectedProjectId = selected.getId();
                        loadData();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerProjects.post(() -> {
            isUpdatingProjects = false;
            projectSpinnerInitialized = true;
        });
    }

    private void updateEmptyStateView() {
        updateMetricsStats();
        if (llEmptyState == null || tvEmptyMessage == null) return;

        if (allocatedProjects.size() <= 1 && projectSpinnerInitialized) {
            llEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No projects are currently allocated to you.");
            return;
        }

        if (currentMoveList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No move site records found for " + selDay + "/" + selMonth + "/" + selYear);
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateMetricsStats() {
        int total = currentMoveList.size();
        int active = 0;
        int completed = 0;

        for (MoveRecordModel record : currentMoveList) {
            if (record.isActive()) {
                active++;
            } else {
                completed++;
            }
        }

        if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(total));
        if (tvStatActive != null) tvStatActive.setText(String.valueOf(active));
        if (tvStatCompleted != null) tvStatCompleted.setText(String.valueOf(completed));
    }

    @Override
    public void onPhotoClicked(String photoUrl) {
        showLightboxDialog(photoUrl);
    }

    @Override
    public void onMoveOutClicked(MoveRecordModel record) {
        if (isFinishing() || isDestroyed() || record == null) return;

        SessionPrefs session = new SessionPrefs(this);
        if (!session.isLocationSessionValid()) {
            Toast.makeText(this, "Location verification is required before marking move out.", Toast.LENGTH_LONG).show();
            return;
        }

        String freshToken = session.issueFreshVerificationToken();
        String targetLocId = session.getLocationId();
        String pName = session.getProjectName();
        if (pName == null || pName.trim().isEmpty()) pName = record.getProjName();

        Intent intent = new Intent(this, LocationActivity.class);
        intent.putExtra("empid", record.getEmpId());
        intent.putExtra("move_id", record.getMoveId());
        intent.putExtra("emp_name", record.getFirstName());
        intent.putExtra("photo_url", record.getInPhotoUrl());
        intent.putExtra("type", "OUT");
        intent.putExtra("is_movement", true);
        intent.putExtra("uid", uid);
        intent.putExtra("project_id", targetLocId);
        intent.putExtra("departmentid", targetLocId);
        intent.putExtra("projname", pName);
        intent.putExtra("project_name", pName);
        intent.putExtra("locationid", targetLocId);
        intent.putExtra("loc_id", targetLocId);
        intent.putExtra("MATCHED_LOC_ID", targetLocId);
        intent.putExtra("VERIFIED", "true");
        intent.putExtra("VERIFICATION_TOKEN", freshToken);
        startActivityForResult(intent, REQUEST_CODE_MOVE_PUNCH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MOVE_PUNCH) {
            loadData();
        }
    }

    private void showLightboxDialog(String photoUrl) {
        if (isFinishing() || isDestroyed() || TextUtils.isEmpty(photoUrl)) return;

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_lightbox);

        ImageView imgTarget = dialog.findViewById(R.id.img_lightbox_target);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_lightbox);
        ProgressBar progress = dialog.findViewById(R.id.progress_lightbox);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (imgTarget != null) {
            imgTarget.setOnClickListener(v -> dialog.dismiss());
            ImageLoaderHelper.loadImage(this, photoUrl, imgTarget, progress);
        }

        dialog.show();
    }
}
