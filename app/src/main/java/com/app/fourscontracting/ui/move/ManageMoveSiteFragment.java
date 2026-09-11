package com.app.fourscontracting.ui.move;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.fourscontracting.LocationActivity;
import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.ManageAttendanceApi;
import com.app.fourscontracting.data.MoveFeedAdapter;
import com.app.fourscontracting.data.MoveRecordModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageMoveSiteFragment extends Fragment
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
    private TextView tvStatTotal;
    private TextView tvStatActive;
    private TextView tvStatCompleted;
    private ProgressBar progressBar;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_move_site, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null) return;

        userLocalStore = new UserLocalStore(context);
        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        uid = (val_list != null && val_list.length > 0) ? val_list[0] : "";

        initViews(view);
        setupDateSpinners();

        moveAdapter = new MoveFeedAdapter(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(moveAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        loadData();
    }

    public void refreshList() {
        loadData();
    }

    private void initViews(View view) {
        if (view == null) return;
        ImageView imgBack = view.findViewById(R.id.img_back_button);
        TextView tvTitle = view.findViewById(R.id.tv_header_title);
        ImageView btnRefresh = view.findViewById(R.id.btn_refresh_feed);

        if (imgBack != null) {
            imgBack.setVisibility(View.GONE);
        }
        if (tvTitle != null) {
            tvTitle.setVisibility(View.GONE);
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (isAdded()) loadData();
            });
        }

        spinnerProjects = view.findViewById(R.id.spinner_projects);
        spinnerDay = view.findViewById(R.id.spinner_day);
        spinnerMonth = view.findViewById(R.id.spinner_month);
        spinnerYear = view.findViewById(R.id.spinner_year);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.rv_manage_move_site);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        tvStatTotal = view.findViewById(R.id.tv_stat_total);
        tvStatActive = view.findViewById(R.id.tv_stat_active);
        tvStatCompleted = view.findViewById(R.id.tv_stat_completed);
        progressBar = view.findViewById(R.id.progress_manage_move_site);
    }

    private void setupDateSpinners() {
        Context context = getContext();
        if (context == null || spinnerDay == null || spinnerMonth == null || spinnerYear == null) return;

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
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);
        spinnerDay.setSelection(dayIndexToSelect, false);

        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        List<String> monthsList = new ArrayList<>();
        for (String m : monthNames) {
            monthsList.add(m);
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, monthsList);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(currentMonth - 1, false);

        List<String> years = new ArrayList<>();
        years.add(String.valueOf(currentYear - 1));
        years.add(String.valueOf(currentYear));
        years.add(String.valueOf(currentYear + 1));
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, years);
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

        if (getView() != null) {
            getView().post(() -> isInitializingDateSpinners = false);
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
        Context context = getContext();
        if (context == null || !isAdded()) return;

        if (TextUtils.isEmpty(uid)) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (progressBar != null && swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        api.fetchFeed(context, uid, selectedProjectId, selDay, selMonth, selYear, new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName,
                                  List<AllocatedProjectModel> projects,
                                  List<com.app.fourscontracting.data.AttendanceRecordModel> attendanceRecords,
                                  List<MoveRecordModel> moveRecords) {
                if (!isAdded() || getContext() == null) return;

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

                updateSummaryMetrics();
                updateEmptyStateView();
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getContext() == null) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (message != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
                currentMoveList.clear();
                if (moveAdapter != null) {
                    moveAdapter.setItems(currentMoveList);
                }
                updateSummaryMetrics();
                updateEmptyStateView();
            }
        });
    }

    private void updateProjectSpinner(List<AllocatedProjectModel> projects) {
        Context context = getContext();
        if (context == null || spinnerProjects == null) return;

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

        ArrayAdapter<AllocatedProjectModel> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, allocatedProjects);
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

    private void updateSummaryMetrics() {
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
        Context context = getContext();
        if (context == null || record == null || !isAdded()) return;

        SessionPrefs session = new SessionPrefs(context);
        if (!session.isLocationSessionValid()) {
            Toast.makeText(context, "Location verification is required before marking move out.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(context, com.app.fourscontracting.LocationVerifyActivity.class);
            intent.putExtra("IS_INITIAL_VERIFY", false);
            intent.putExtra("IS_CHANGE_SITE", false);
            intent.putExtra("AUTO_START_VERIFY", true);
            intent.putExtra("uid", uid);
            intent.putExtra("empid", record.getEmpId());
            intent.putExtra("pending_eid", record.getEmpId());
            intent.putExtra("move_id", record.getMoveId());
            intent.putExtra("moveid", record.getMoveId());
            intent.putExtra("type", "OUT");
            intent.putExtra("pending_action", "OUT");
            intent.putExtra("is_movement", true);
            startActivityForResult(intent, REQUEST_CODE_MOVE_PUNCH);
            return;
        }

        String freshToken = session.issueFreshVerificationToken();
        String targetLocId = session.getLocationId();
        String pName = session.getProjectName();
        if (pName == null || pName.trim().isEmpty()) pName = record.getProjName();

        Intent intent = new Intent(context, LocationActivity.class);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MOVE_PUNCH) {
            if (isAdded()) {
                loadData();
            }
        }
    }

    private void showLightboxDialog(String photoUrl) {
        Context context = getContext();
        if (context == null) return;

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_lightbox);

        ImageView imgTarget = dialog.findViewById(R.id.img_lightbox_target);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_lightbox);
        ProgressBar progress = dialog.findViewById(R.id.progress_lightbox);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (imgTarget != null) {
            imgTarget.setOnClickListener(v -> dialog.dismiss());
            ImageLoaderHelper.loadImage(context, photoUrl, imgTarget, progress);
        }

        dialog.show();
    }
}
