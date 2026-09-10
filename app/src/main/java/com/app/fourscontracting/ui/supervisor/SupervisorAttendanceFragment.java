package com.app.fourscontracting.ui.supervisor;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.fourscontracting.AppMessages;
import com.app.fourscontracting.DashboardActivity;
import com.app.fourscontracting.LocationVerifyActivity;
import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.UserLocation;
import com.app.fourscontracting.UserProject;
import com.app.fourscontracting.data.AllocatedProjectModel;
import com.app.fourscontracting.data.AttendanceFeedAdapter;
import com.app.fourscontracting.data.AttendanceRecordModel;
import com.app.fourscontracting.data.ImageLoaderHelper;
import com.app.fourscontracting.data.ManageAttendanceApi;
import com.app.fourscontracting.data.MoveRecordModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Native Supervisor Self Attendance & Past Punch History Fragment.
 * Allows logged-in supervisors to perform Self TIME IN, Self TIME OUT, and Self MOVE SITE,
 * while viewing past attendance history records with date filtering.
 */
public class SupervisorAttendanceFragment extends Fragment
        implements AttendanceFeedAdapter.Listener {

    private static final int REQUEST_CODE_SELF_PUNCH = 2001;

    private UserLocalStore userLocalStore;
    private SessionPrefs sessionPrefs;
    private final ManageAttendanceApi api = new ManageAttendanceApi();

    private TextView tvSupervisorName;
    private TextView tvSupervisorEmpId;
    private TextView tvSupervisorSite;
    private TextView tvSelfStatusBadge;
    private TextView tvSelfTimeIn;
    private TextView tvSelfTimeOut;
    private TextView tvSelfProject;
    private Button btnSelfTimeIn;
    private Button btnSelfTimeOut;
    private Button btnSelfMoveSite;
    private ProgressBar progress;

    // History Manage Section Views
    private Spinner spinnerHistoryDay;
    private Spinner spinnerHistoryMonth;
    private Spinner spinnerHistoryYear;
    private TextView tvHistoryStatTotal;
    private TextView tvHistoryStatActive;
    private TextView tvHistoryStatCompleted;
    private ProgressBar progressHistory;
    private View llHistoryEmpty;
    private TextView tvHistoryEmptyMsg;
    private RecyclerView rvPastHistory;
    private AttendanceFeedAdapter historyAdapter;

    private String supervisorUid = "";
    private String supervisorName = "";
    private String projId = "";
    private String projName = "";
    private String locationId = "";

    private String selHistoryDay = "";
    private String selHistoryMonth = "";
    private String selHistoryYear = "";
    private boolean isInitializingHistoryDateSpinners = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_supervisor_attendance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null) return;

        userLocalStore = new UserLocalStore(context);
        sessionPrefs = new SessionPrefs(context);

        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        supervisorUid = val_list.length > 0 ? val_list[0] : "";
        supervisorName = (val_list.length > 2 && !val_list[2].isEmpty()) ? val_list[2] : "Supervisor";
        projId = userLocalStore.getLoggedInUserProjectID();

        UserProject up = userLocalStore.getLoggedInUserProject();
        projName = up != null ? up.projectname : "";

        initViews(view);
        bindHeaderData();

        btnSelfTimeIn.setOnClickListener(v -> performSelfPunch("IN"));
        btnSelfTimeOut.setOnClickListener(v -> performSelfPunch("OUT"));
        btnSelfMoveSite.setOnClickListener(v -> performSelfMove());

        historyAdapter = new AttendanceFeedAdapter(this);
        rvPastHistory.setLayoutManager(new LinearLayoutManager(context));
        rvPastHistory.setAdapter(historyAdapter);

        setupHistoryDateSpinners();

        loadSupervisorAttendanceData();
        loadSupervisorHistoryData();
    }

    private void initViews(View view) {
        tvSupervisorName = view.findViewById(R.id.tv_supervisor_name);
        tvSupervisorEmpId = view.findViewById(R.id.tv_supervisor_emp_id);
        tvSupervisorSite = view.findViewById(R.id.tv_supervisor_site);
        tvSelfStatusBadge = view.findViewById(R.id.tv_self_status_badge);
        tvSelfTimeIn = view.findViewById(R.id.tv_self_time_in);
        tvSelfTimeOut = view.findViewById(R.id.tv_self_time_out);
        tvSelfProject = view.findViewById(R.id.tv_self_project);
        btnSelfTimeIn = view.findViewById(R.id.btn_self_time_in);
        btnSelfTimeOut = view.findViewById(R.id.btn_self_time_out);
        btnSelfMoveSite = view.findViewById(R.id.btn_self_move_site);
        progress = view.findViewById(R.id.progress_supervisor_self);

        View btnChangeSite = view.findViewById(R.id.btn_supervisor_change_site);
        if (btnChangeSite != null) {
            btnChangeSite.setOnClickListener(v -> launchLocationVerifyForSession());
        }

        // History Views Initialization
        spinnerHistoryDay = view.findViewById(R.id.spinner_supervisor_history_day);
        spinnerHistoryMonth = view.findViewById(R.id.spinner_supervisor_history_month);
        spinnerHistoryYear = view.findViewById(R.id.spinner_supervisor_history_year);
        tvHistoryStatTotal = view.findViewById(R.id.tv_supervisor_history_stat_total);
        tvHistoryStatActive = view.findViewById(R.id.tv_supervisor_history_stat_active);
        tvHistoryStatCompleted = view.findViewById(R.id.tv_supervisor_history_stat_completed);
        progressHistory = view.findViewById(R.id.progress_supervisor_history);
        llHistoryEmpty = view.findViewById(R.id.ll_supervisor_history_empty);
        tvHistoryEmptyMsg = view.findViewById(R.id.tv_supervisor_history_empty_msg);
        rvPastHistory = view.findViewById(R.id.rv_supervisor_past_history);

        View btnHistoryRefresh = view.findViewById(R.id.btn_supervisor_history_refresh);
        if (btnHistoryRefresh != null) {
            btnHistoryRefresh.setOnClickListener(v -> {
                if (isAdded()) loadSupervisorHistoryData();
            });
        }
    }

    private void bindHeaderData() {
        if (userLocalStore != null) {
            User user = userLocalStore.getLoggedInUser();
            String val = user != null ? user.username : "";
            String[] val_list = UserLocalStore.parseUserInfo(val);
            if (val_list.length > 0 && !val_list[0].isEmpty()) {
                supervisorUid = val_list[0];
            }
            if (val_list.length > 2 && !val_list[2].isEmpty()) {
                supervisorName = val_list[2];
            }
        }

        if (tvSupervisorName != null) {
            tvSupervisorName.setText(!TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor");
        }
        if (tvSupervisorEmpId != null) {
            tvSupervisorEmpId.setText("Supervisor ID: #" + (!TextUtils.isEmpty(supervisorUid) ? supervisorUid : "--"));
        }

        locationId = sessionPrefs != null ? sessionPrefs.getLocationId() : "";
        String siteName = sessionPrefs != null ? sessionPrefs.getProjectName() : "";
        if (TextUtils.isEmpty(siteName)) {
            siteName = projName;
        }

        if (tvSupervisorSite != null) {
            tvSupervisorSite.setText("Verified Site: " + (TextUtils.isEmpty(siteName) ? "Main Location" : siteName));
        }
    }

    private void setupHistoryDateSpinners() {
        Context context = getContext();
        if (context == null || spinnerHistoryDay == null || spinnerHistoryMonth == null || spinnerHistoryYear == null) return;

        isInitializingHistoryDateSpinners = true;

        Calendar cal = Calendar.getInstance();
        int curDay = cal.get(Calendar.DAY_OF_MONTH);
        int curMonth = cal.get(Calendar.MONTH);
        int curYear = cal.get(Calendar.YEAR);

        List<String> days = new ArrayList<>();
        days.add("Day");
        for (int i = 1; i <= 31; i++) {
            days.add(String.format(Locale.US, "%02d", i));
        }

        List<String> months = new ArrayList<>();
        months.add("Month");
        String[] monthNames = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String m : monthNames) {
            months.add(m);
        }

        List<String> years = new ArrayList<>();
        years.add("Year");
        for (int y = curYear - 2; y <= curYear + 1; y++) {
            years.add(String.valueOf(y));
        }

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, days);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistoryDay.setAdapter(dayAdapter);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistoryMonth.setAdapter(monthAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistoryYear.setAdapter(yearAdapter);

        spinnerHistoryDay.setSelection(curDay);
        spinnerHistoryMonth.setSelection(curMonth + 1);
        int yearIndex = years.indexOf(String.valueOf(curYear));
        if (yearIndex >= 0) spinnerHistoryYear.setSelection(yearIndex);

        selHistoryDay = String.format(Locale.US, "%02d", curDay);
        selHistoryMonth = String.format(Locale.US, "%02d", curMonth + 1);
        selHistoryYear = String.valueOf(curYear);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializingHistoryDateSpinners) return;
                updateSelectedHistoryDateValues();
                loadSupervisorHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerHistoryDay.setOnItemSelectedListener(listener);
        spinnerHistoryMonth.setOnItemSelectedListener(listener);
        spinnerHistoryYear.setOnItemSelectedListener(listener);

        isInitializingHistoryDateSpinners = false;
    }

    private void updateSelectedHistoryDateValues() {
        if (spinnerHistoryDay != null && spinnerHistoryDay.getSelectedItemPosition() > 0) {
            selHistoryDay = (String) spinnerHistoryDay.getSelectedItem();
        } else {
            selHistoryDay = "";
        }

        if (spinnerHistoryMonth != null && spinnerHistoryMonth.getSelectedItemPosition() > 0) {
            selHistoryMonth = String.format(Locale.US, "%02d", spinnerHistoryMonth.getSelectedItemPosition());
        } else {
            selHistoryMonth = "";
        }

        if (spinnerHistoryYear != null && spinnerHistoryYear.getSelectedItemPosition() > 0) {
            selHistoryYear = (String) spinnerHistoryYear.getSelectedItem();
        } else {
            selHistoryYear = "";
        }
    }

    public void refreshData() {
        if (!isAdded()) return;
        bindHeaderData();
        loadSupervisorAttendanceData();
        loadSupervisorHistoryData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).updateToolbarTitle("Self Attendance");
        }
        refreshData();
    }

    private void loadSupervisorAttendanceData() {
        Context context = getContext();
        if (context == null || TextUtils.isEmpty(supervisorUid)) return;

        if (progress != null) progress.setVisibility(View.VISIBLE);

        Calendar cal = Calendar.getInstance();
        String selDay = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH));
        String selMonth = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1);
        String selYear = String.valueOf(cal.get(Calendar.YEAR));

        api.fetchFeed(context, supervisorUid, "all", selDay, selMonth, selYear, new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName, List<AllocatedProjectModel> projects, List<AttendanceRecordModel> attendanceRecords, List<MoveRecordModel> moveRecords) {
                if (!isAdded()) return;
                if (attendanceRecords == null || attendanceRecords.isEmpty()) {
                    fetchSupervisorAttendanceFallback(context, userName);
                    return;
                }
                processSupervisorAttendanceFeed(userName, attendanceRecords);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                fetchSupervisorAttendanceFallback(context, "");
            }
        });
    }

    private void fetchSupervisorAttendanceFallback(Context context, String currentUserName) {
        api.fetchFeed(context, supervisorUid, "all", "", "", "", new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName, List<AllocatedProjectModel> projects, List<AttendanceRecordModel> attendanceRecords, List<MoveRecordModel> moveRecords) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                processSupervisorAttendanceFeed(!TextUtils.isEmpty(userName) ? userName : currentUserName, attendanceRecords);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                updateSelfUi(null);
            }
        });
    }

    private boolean isSupervisorSelfRecord(AttendanceRecordModel r) {
        if (r == null) return false;

        String uId = r.getUserId() != null ? r.getUserId().trim() : "";
        String eId = r.getEmpId() != null ? r.getEmpId().trim() : "";
        String sUid = supervisorUid != null ? supervisorUid.trim() : "";

        // 1. If empId is provided and belongs to a DIFFERENT worker (e.g. 37 != 85), reject immediately
        if (!eId.isEmpty() && !sUid.isEmpty()) {
            boolean isSame = eId.equalsIgnoreCase(sUid);
            if (!isSame) {
                try {
                    isSame = Integer.parseInt(eId) == Integer.parseInt(sUid);
                } catch (Exception ignored) {}
            }
            if (!isSame) {
                return false;
            }
        }

        // 2. If empId or userId matches supervisorUid, or userId == empId, accept!
        if (!sUid.isEmpty()) {
            if (!eId.isEmpty() && eId.equalsIgnoreCase(sUid)) return true;
            if (!uId.isEmpty() && uId.equalsIgnoreCase(sUid)) return true;
        }

        if (!uId.isEmpty() && !eId.isEmpty() && uId.equalsIgnoreCase(eId)) {
            return true;
        }

        // 3. Name match fallback
        if (!TextUtils.isEmpty(r.getFirstName()) && !TextUtils.isEmpty(supervisorName)) {
            String name = r.getFirstName().trim();
            String supName = supervisorName.trim();
            if (supName.equalsIgnoreCase(name) || supName.toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(supName.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private void processSupervisorAttendanceFeed(String userName, List<AttendanceRecordModel> attendanceRecords) {
        if (progress != null) progress.setVisibility(View.GONE);

        AttendanceRecordModel selfRecord = null;
        if (attendanceRecords != null && !attendanceRecords.isEmpty()) {
            List<AttendanceRecordModel> supervisorRecords = new ArrayList<>();
            for (AttendanceRecordModel r : attendanceRecords) {
                if (isSupervisorSelfRecord(r)) {
                    supervisorRecords.add(r);
                }
            }

            if (!supervisorRecords.isEmpty()) {
                // 1. First priority: look for an active (open) punch
                for (AttendanceRecordModel r : supervisorRecords) {
                    if (r.isActive() && !TextUtils.isEmpty(r.getTimeIn()) && !"--".equals(r.getTimeIn())) {
                        selfRecord = r;
                        break;
                    }
                }
                // 2. Second priority: pick the latest record with valid timeIn
                if (selfRecord == null) {
                    for (int i = supervisorRecords.size() - 1; i >= 0; i--) {
                        AttendanceRecordModel r = supervisorRecords.get(i);
                        if (!TextUtils.isEmpty(r.getTimeIn()) && !"--".equals(r.getTimeIn())) {
                            selfRecord = r;
                            break;
                        }
                    }
                }
                // 3. Fallback: pick the last item in supervisorRecords
                if (selfRecord == null) {
                    selfRecord = supervisorRecords.get(supervisorRecords.size() - 1);
                }
            }
        }

        updateSelfUi(selfRecord);
    }

    private void loadSupervisorHistoryData() {
        Context context = getContext();
        if (context == null || TextUtils.isEmpty(supervisorUid)) return;

        if (progressHistory != null) progressHistory.setVisibility(View.VISIBLE);

        api.fetchFeed(context, supervisorUid, "all", selHistoryDay, selHistoryMonth, selHistoryYear, new ManageAttendanceApi.FeedCallback() {
            @Override
            public void onSuccess(String userName, List<AllocatedProjectModel> projects, List<AttendanceRecordModel> attendanceRecords, List<MoveRecordModel> moveRecords) {
                if (!isAdded()) return;
                if (progressHistory != null) progressHistory.setVisibility(View.GONE);

                List<AttendanceRecordModel> historyList = new ArrayList<>();
                if (attendanceRecords != null) {
                    for (AttendanceRecordModel r : attendanceRecords) {
                        if (isSupervisorSelfRecord(r)) {
                            String displayName = r.getFirstName();
                            if (TextUtils.isEmpty(displayName) || "User".equalsIgnoreCase(displayName.trim())) {
                                displayName = (!TextUtils.isEmpty(supervisorName) && !"User".equalsIgnoreCase(supervisorName.trim())) ? supervisorName : (!TextUtils.isEmpty(userName) && !"User".equalsIgnoreCase(userName.trim())) ? userName : "Supervisor #" + supervisorUid;
                            }

                            AttendanceRecordModel displayRecord = new AttendanceRecordModel(
                                    r.getAttendId(),
                                    r.getUserId(),
                                    r.getEmpId(),
                                    displayName,
                                    r.getProjName(),
                                    r.getTimeIn(),
                                    r.getTimeOut(),
                                    r.getBreakHours(),
                                    r.isHasIn(),
                                    r.isHasOut(),
                                    r.getInPhotoUrl(),
                                    r.getOutPhotoUrl()
                            );
                            historyList.add(displayRecord);
                        }
                    }
                }

                if (historyAdapter != null) {
                    historyAdapter.setItems(historyList);
                }

                updateHistoryUiStats(historyList);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progressHistory != null) progressHistory.setVisibility(View.GONE);
                if (historyAdapter != null) historyAdapter.setItems(new ArrayList<>());
                updateHistoryUiStats(new ArrayList<>());
            }
        });
    }

    private void updateHistoryUiStats(List<AttendanceRecordModel> records) {
        int total = records != null ? records.size() : 0;
        int activeCount = 0;
        int completedCount = 0;

        if (records != null) {
            for (AttendanceRecordModel r : records) {
                if (r.isActive()) {
                    activeCount++;
                } else {
                    completedCount++;
                }
            }
        }

        if (tvHistoryStatTotal != null) tvHistoryStatTotal.setText(String.valueOf(total));
        if (tvHistoryStatActive != null) tvHistoryStatActive.setText(String.valueOf(activeCount));
        if (tvHistoryStatCompleted != null) tvHistoryStatCompleted.setText(String.valueOf(completedCount));

        if (llHistoryEmpty != null) {
            if (total == 0) {
                llHistoryEmpty.setVisibility(View.VISIBLE);
                if (tvHistoryEmptyMsg != null) {
                    tvHistoryEmptyMsg.setText("No past attendance history found for the selected date.");
                }
            } else {
                llHistoryEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void updateSelfUi(AttendanceRecordModel record) {
        if (record != null) {
            String formattedIn = formatTime(record.getTimeIn());
            String formattedOut = formatTime(record.getTimeOut());

            if (tvSelfTimeIn != null) tvSelfTimeIn.setText(formattedIn);
            if (tvSelfTimeOut != null) tvSelfTimeOut.setText(formattedOut);

            String currentSite = record.getProjName();
            if (TextUtils.isEmpty(currentSite)) {
                currentSite = projName;
            }
            if (TextUtils.isEmpty(currentSite) && sessionPrefs != null) {
                currentSite = sessionPrefs.getProjectName();
            }
            if (tvSelfProject != null) {
                tvSelfProject.setText("Project: " + (TextUtils.isEmpty(currentSite) ? "Main Location" : currentSite));
            }

            if (tvSelfStatusBadge != null) {
                if (record.isActive()) {
                    tvSelfStatusBadge.setText("ACTIVE");
                    tvSelfStatusBadge.setBackgroundResource(R.drawable.bg_luxury_status_active);
                    tvSelfStatusBadge.setTextColor(0xFFFFFFFF);
                } else {
                    tvSelfStatusBadge.setText("COMPLETED");
                    tvSelfStatusBadge.setBackgroundResource(R.drawable.bg_luxury_status_completed);
                    tvSelfStatusBadge.setTextColor(0xFFFFFFFF);
                }
            }

            if (btnSelfTimeIn != null) btnSelfTimeIn.setVisibility(View.GONE);
            if (btnSelfTimeOut != null) btnSelfTimeOut.setVisibility(record.isActive() ? View.VISIBLE : View.GONE);
            if (btnSelfMoveSite != null) btnSelfMoveSite.setVisibility(record.isActive() ? View.GONE : View.VISIBLE);
        } else {
            if (tvSelfTimeIn != null) tvSelfTimeIn.setText("--:--");
            if (tvSelfTimeOut != null) tvSelfTimeOut.setText("--:--");
            String currentSite = projName;
            if (TextUtils.isEmpty(currentSite) && sessionPrefs != null) {
                currentSite = sessionPrefs.getProjectName();
            }
            if (tvSelfProject != null) {
                tvSelfProject.setText("Project: " + (TextUtils.isEmpty(currentSite) ? "Main Location" : currentSite));
            }
            if (tvSelfStatusBadge != null) {
                tvSelfStatusBadge.setText("NOT PUNCHED");
                tvSelfStatusBadge.setBackgroundResource(R.drawable.bg_top_header_pill);
                tvSelfStatusBadge.setTextColor(0xFF0284C7);
            }

            if (btnSelfTimeIn != null) btnSelfTimeIn.setVisibility(View.VISIBLE);
            if (btnSelfTimeOut != null) btnSelfTimeOut.setVisibility(View.GONE);
            if (btnSelfMoveSite != null) btnSelfMoveSite.setVisibility(View.GONE);
        }
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty() || "--".equals(rawTime) || "--:--".equals(rawTime)) {
            return "--:--";
        }
        if (rawTime.toLowerCase().contains("am") || rawTime.toLowerCase().contains("pm")) {
            return rawTime;
        }
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US);
            java.util.Date date = inFormat.parse(rawTime);
            if (date != null) {
                java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("hh:mm a", Locale.US);
                return outFormat.format(date);
            }
        } catch (Exception ignored) {
            try {
                java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("HH:mm", Locale.US);
                java.util.Date date = inFormat.parse(rawTime);
                if (date != null) {
                    java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("hh:mm a", Locale.US);
                    return outFormat.format(date);
                }
            } catch (Exception ignored2) {}
        }
        return rawTime;
    }

    private String getSupervisorUid() {
        if (!TextUtils.isEmpty(supervisorUid) && !"--".equals(supervisorUid.trim())) {
            return supervisorUid;
        }
        if (userLocalStore != null) {
            User user = userLocalStore.getLoggedInUser();
            String val = user != null ? user.username : "";
            String[] val_list = UserLocalStore.parseUserInfo(val);
            if (val_list.length > 0 && !TextUtils.isEmpty(val_list[0])) {
                supervisorUid = val_list[0];
                return supervisorUid;
            }
        }
        return supervisorUid != null ? supervisorUid : "";
    }

    private void performSelfPunch(String punchType) {
        Context context = getContext();
        if (context == null) return;

        String sUid = getSupervisorUid();

        if (!sessionPrefs.isLocationSessionValid()) {
            Toast.makeText(context, AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            launchLocationVerifyForSessionWithAction(punchType, sUid);
            return;
        }

        // Location session is verified — issue a fresh token & open LocationActivity (camera & photo capture submit screen)
        String freshToken = sessionPrefs.issueFreshVerificationToken();

        Intent intent = new Intent(context, com.app.fourscontracting.LocationActivity.class);
        intent.putExtra("empid", sUid);
        intent.putExtra("emp_name", !TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor");
        intent.putExtra("type", punchType);
        intent.putExtra("uid", sUid);
        String pId = !TextUtils.isEmpty(projId) ? projId : sessionPrefs.getProjectId();
        String pName = !TextUtils.isEmpty(projName) ? projName : sessionPrefs.getProjectName();
        intent.putExtra("project_id", pId);
        intent.putExtra("departmentid", pId);
        intent.putExtra("projname", pName);
        intent.putExtra("project_name", pName);
        intent.putExtra("locationid", locationId);
        intent.putExtra("loc_id", locationId);
        intent.putExtra("MATCHED_LOC_ID", locationId);
        intent.putExtra("VERIFIED", "true");
        intent.putExtra("VERIFICATION_TOKEN", freshToken);
        startActivityForResult(intent, REQUEST_CODE_SELF_PUNCH);
    }

    private void launchLocationVerifyForSessionWithAction(String punchType, String sUid) {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", false);
        intent.putExtra("IS_CHANGE_SITE", false);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("uid", sUid);
        intent.putExtra("empid", sUid);
        intent.putExtra("pending_eid", sUid);
        intent.putExtra("type", punchType);
        intent.putExtra("pending_action", punchType);
        startActivityForResult(intent, REQUEST_CODE_SELF_PUNCH);
    }

    private void performSelfMove() {
        Context context = getContext();
        if (context == null) return;

        String sUid = getSupervisorUid();

        Intent intent = new Intent(context, LocationVerifyActivity.class);
        intent.putExtra("empid", sUid);
        intent.putExtra("emp_name", !TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor");
        intent.putExtra("type", "MOVE");
        intent.putExtra("uid", sUid);
        intent.putExtra("is_movement", true);
        intent.putExtra("selected_project_id", projId);
        intent.putExtra("projname", projName);
        intent.putExtra("FROM_MOVE_MENU", true);
        intent.putExtra("AUTO_START_VERIFY", false);
        intent.putExtra("IS_INITIAL_VERIFY", false);
        startActivityForResult(intent, REQUEST_CODE_SELF_PUNCH);
    }

    private void launchLocationVerifyForSession() {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", true);
        intent.putExtra("IS_CHANGE_SITE", true);
        intent.putExtra("AUTO_START_VERIFY", false);
        intent.putExtra("uid", getSupervisorUid());
        startActivityForResult(intent, REQUEST_CODE_SELF_PUNCH);
    }

    @Override
    public void onBreakClicked(AttendanceRecordModel record) {
        showBreakStatusDialog(record);
    }

    @Override
    public void onPhotoClicked(String photoUrl) {
        showLightboxDialog(photoUrl);
    }

    @Override
    public void onTimeOutClicked(AttendanceRecordModel record) {
        Context context = getContext();
        if (context == null || record == null || !isAdded()) return;

        if (sessionPrefs == null) sessionPrefs = new SessionPrefs(context);
        if (!sessionPrefs.isLocationSessionValid()) {
            Toast.makeText(context, AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            return;
        }

        String freshToken = sessionPrefs.issueFreshVerificationToken();
        String targetLocId = sessionPrefs.getLocationId();
        String targetProjName = sessionPrefs.getProjectName();
        if (TextUtils.isEmpty(targetProjName)) targetProjName = record.getProjName();

        Intent intent = new Intent(context, com.app.fourscontracting.LocationActivity.class);
        intent.putExtra("empid", record.getEmpId());
        intent.putExtra("emp_name", record.getFirstName());
        intent.putExtra("photo_url", record.getInPhotoUrl());
        intent.putExtra("type", "OUT");
        intent.putExtra("uid", supervisorUid);
        intent.putExtra("project_id", targetLocId);
        intent.putExtra("departmentid", targetLocId);
        intent.putExtra("projname", targetProjName);
        intent.putExtra("project_name", targetProjName);
        intent.putExtra("locationid", targetLocId);
        intent.putExtra("loc_id", targetLocId);
        intent.putExtra("MATCHED_LOC_ID", targetLocId);
        intent.putExtra("VERIFIED", "true");
        intent.putExtra("VERIFICATION_TOKEN", freshToken);
        startActivityForResult(intent, REQUEST_CODE_SELF_PUNCH);
    }

    private void showBreakStatusDialog(AttendanceRecordModel record) {
        Context context = getContext();
        if (context == null || record == null) return;

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_break_status);

        CheckBox cbBreak = dialog.findViewById(R.id.cb_took_break);
        View btnCancel = dialog.findViewById(R.id.btn_cancel_break);
        View btnSave = dialog.findViewById(R.id.btn_save_break);

        boolean currentBreak = record.getBreakHours() > 0;
        if (cbBreak != null) {
            cbBreak.setChecked(currentBreak);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSave != null && cbBreak != null) {
            btnSave.setOnClickListener(v -> {
                if (!isAdded() || getContext() == null) {
                    dialog.dismiss();
                    return;
                }
                boolean isChecked = cbBreak.isChecked();
                btnSave.setEnabled(false);
                api.updateBreakStatus(getContext(), record.getAttendId(), isChecked, new ManageAttendanceApi.BreakUpdateCallback() {
                    @Override
                    public void onSuccess(String attendId, double updatedBreakHours) {
                        if (!isAdded() || getContext() == null) return;
                        btnSave.setEnabled(true);
                        dialog.dismiss();
                        record.setBreakHours(updatedBreakHours);
                        if (historyAdapter != null) {
                            historyAdapter.notifyDataSetChanged();
                        }
                        Toast.makeText(getContext(), "Break status updated successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || getContext() == null) return;
                        btnSave.setEnabled(true);
                        if (message != null) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }

        dialog.show();
    }

    private void showLightboxDialog(String photoUrl) {
        Context context = getContext();
        if (context == null || TextUtils.isEmpty(photoUrl)) return;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELF_PUNCH) {
            bindHeaderData();
            loadSupervisorAttendanceData();
            loadSupervisorHistoryData();
        }
    }
}
