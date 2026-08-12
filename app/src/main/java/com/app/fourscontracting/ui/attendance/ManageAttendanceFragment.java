package com.app.fourscontracting.ui.attendance;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.AttendanceFeedAdapter;
import com.app.fourscontracting.data.AttendanceRecordModel;
import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.ManageAttendanceApi;
import com.app.fourscontracting.data.MoveFeedAdapter;
import com.app.fourscontracting.data.MoveRecordModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageAttendanceFragment extends Fragment
        implements AttendanceFeedAdapter.Listener, MoveFeedAdapter.Listener {

    private UserLocalStore userLocalStore;
    private final ManageAttendanceApi api = new ManageAttendanceApi();

    private Spinner spinnerProjects;
    private Spinner spinnerDay;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;
    private Button btnTabAttendance;
    private Button btnTabMove;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    private AttendanceFeedAdapter attendanceAdapter;
    private MoveFeedAdapter moveAdapter;

    private String uid = "";
    private String selectedProjectId = "all";
    private String selDay = "";
    private String selMonth = "";
    private String selYear = "";
    private boolean isAttendanceTab = true;
    private boolean projectSpinnerInitialized = false;
    private boolean isInitializingDateSpinners = true;
    private boolean isUpdatingProjects = false;

    private final List<AllocatedProjectModel> allocatedProjects = new ArrayList<>();
    private final List<AttendanceRecordModel> currentAttendanceList = new ArrayList<>();
    private final List<MoveRecordModel> currentMoveList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_attendance, container, false);
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
        setupTabButtons();

        attendanceAdapter = new AttendanceFeedAdapter(this);
        moveAdapter = new MoveFeedAdapter(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(attendanceAdapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        loadData();
    }

    public void refreshList() {
        loadData();
    }

    private void initViews(View view) {
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
            btnRefresh.setOnClickListener(v -> loadData());
        }

        spinnerProjects = view.findViewById(R.id.spinner_projects);
        spinnerDay = view.findViewById(R.id.spinner_day);
        spinnerMonth = view.findViewById(R.id.spinner_month);
        spinnerYear = view.findViewById(R.id.spinner_year);
        btnTabAttendance = view.findViewById(R.id.btn_tab_attendance);
        btnTabMove = view.findViewById(R.id.btn_tab_move);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerView = view.findViewById(R.id.rv_manage_attendance);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        progressBar = view.findViewById(R.id.progress_manage_attendance);
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

        isInitializingDateSpinners = false;
    }

    private void setupTabButtons() {
        if (btnTabAttendance != null) {
            btnTabAttendance.setOnClickListener(v -> switchTab(true));
        }
        if (btnTabMove != null) {
            btnTabMove.setOnClickListener(v -> switchTab(false));
        }
    }

    private void switchTab(boolean isAttendance) {
        this.isAttendanceTab = isAttendance;
        if (btnTabAttendance != null && btnTabMove != null) {
            if (isAttendance) {
                btnTabAttendance.setBackgroundResource(R.drawable.bg_tab_btn_selected);
                btnTabAttendance.setTextColor(0xFF0095FF);
                btnTabMove.setBackgroundResource(R.drawable.bg_tab_btn_unselected);
                btnTabMove.setTextColor(0xFF8A8F9E);

                if (recyclerView != null) {
                    recyclerView.setAdapter(attendanceAdapter);
                }
            } else {
                btnTabMove.setBackgroundResource(R.drawable.bg_tab_btn_selected);
                btnTabMove.setTextColor(0xFF0095FF);
                btnTabAttendance.setBackgroundResource(R.drawable.bg_tab_btn_unselected);
                btnTabAttendance.setTextColor(0xFF8A8F9E);

                if (recyclerView != null) {
                    recyclerView.setAdapter(moveAdapter);
                }
            }
        }
        updateEmptyStateView();
    }

    private void loadData() {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        if (progressBar != null && swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        api.fetchFeed(context, uid, selectedProjectId, selDay, selMonth, selYear, new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName,
                                  List<AllocatedProjectModel> projects,
                                  List<AttendanceRecordModel> attendanceRecords,
                                  List<MoveRecordModel> moveRecords) {
                if (!isAdded() || getContext() == null) return;

                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                if (!projectSpinnerInitialized || (projects != null && projects.size() != (allocatedProjects.size() - 1))) {
                    updateProjectSpinner(projects);
                }

                currentAttendanceList.clear();
                if (attendanceRecords != null) {
                    currentAttendanceList.addAll(attendanceRecords);
                }
                if (attendanceAdapter != null) {
                    attendanceAdapter.setItems(currentAttendanceList);
                }

                currentMoveList.clear();
                if (moveRecords != null) {
                    currentMoveList.addAll(moveRecords);
                }
                if (moveAdapter != null) {
                    moveAdapter.setItems(currentMoveList);
                }

                updateEmptyStateView();
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getContext() == null) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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

        ArrayAdapter<AllocatedProjectModel> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, allocatedProjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProjects.setAdapter(adapter);

        spinnerProjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingProjects) return;
                if (position >= 0 && position < allocatedProjects.size()) {
                    AllocatedProjectModel selected = allocatedProjects.get(position);
                    selectedProjectId = selected.getId();
                    loadData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        isUpdatingProjects = false;
        projectSpinnerInitialized = true;
    }

    private void updateEmptyStateView() {
        if (llEmptyState == null || tvEmptyMessage == null) return;

        if (allocatedProjects.size() <= 1 && projectSpinnerInitialized) {
            llEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No projects are currently allocated to you.");
            return;
        }

        if (isAttendanceTab) {
            if (currentAttendanceList.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("No attendance found for " + selDay + "/" + selMonth + "/" + selYear);
            } else {
                llEmptyState.setVisibility(View.GONE);
            }
        } else {
            if (currentMoveList.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("No site movements found for " + selDay + "/" + selMonth + "/" + selYear);
            } else {
                llEmptyState.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onBreakClicked(AttendanceRecordModel record) {
        showBreakModal(record);
    }

    @Override
    public void onPhotoClicked(String photoUrl) {
        showLightboxDialog(photoUrl);
    }

    private void showBreakModal(AttendanceRecordModel record) {
        Context context = getContext();
        if (context == null || record == null) return;

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_break_status);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        CheckBox cbBreak = dialog.findViewById(R.id.cb_took_break);
        Button btnSave = dialog.findViewById(R.id.btn_save_break);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_break);

        if (cbBreak != null) {
            cbBreak.setChecked(record.getBreakHours() > 0);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSave != null && cbBreak != null) {
            btnSave.setOnClickListener(v -> {
                boolean isChecked = cbBreak.isChecked();
                btnSave.setEnabled(false);
                api.updateBreakStatus(context, record.getAttendId(), isChecked, new ManageAttendanceApi.BreakUpdateCallback() {
                    @Override
                    public void onSuccess(String attendId, double updatedBreakHours) {
                        if (!isAdded() || getContext() == null) return;
                        btnSave.setEnabled(true);
                        dialog.dismiss();
                        record.setBreakHours(updatedBreakHours);
                        if (attendanceAdapter != null) {
                            attendanceAdapter.notifyDataSetChanged();
                        }
                        Toast.makeText(getContext(), "Break status updated successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || getContext() == null) return;
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
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
