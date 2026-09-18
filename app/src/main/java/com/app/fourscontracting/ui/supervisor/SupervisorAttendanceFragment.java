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
import com.app.fourscontracting.LocationActivity;
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
import com.app.fourscontracting.data.MoveEmployeeModel;
import com.app.fourscontracting.data.MoveRecordModel;
import com.app.fourscontracting.data.MoveSiteApi;

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
    private final MoveSiteApi moveSiteApi = new MoveSiteApi();

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
        btnSelfTimeOut.setOnClickListener(v -> {
    if (currentActiveMoveRecord != null && currentActiveMoveRecord.isActive()) {
        performSupervisorMoveOut(currentActiveMoveRecord);
    } else {
        performSelfPunch("OUT");
    }
});
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

    private AttendanceRecordModel currentSelfRecord = null;
    private MoveRecordModel currentActiveMoveRecord = null;

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
                processSupervisorAttendanceFeed(userName, attendanceRecords, moveRecords);
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
                processSupervisorAttendanceFeed(!TextUtils.isEmpty(userName) ? userName : currentUserName, attendanceRecords, moveRecords);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                updateSelfUiWithMovement(null, null);
            }
        });
    }

    private boolean isSupervisorSelfRecord(AttendanceRecordModel r) {
        if (r == null) return false;

        String sUid = supervisorUid != null ? supervisorUid.trim() : "";
        if (sUid.isEmpty()) return false;

        String eId = r.getEmpId() != null ? r.getEmpId().trim() : "";
        if (!eId.isEmpty() && !"--".equals(eId) && !"null".equalsIgnoreCase(eId)) {
            boolean isSame = eId.equalsIgnoreCase(sUid);
            if (!isSame) {
                try {
                    isSame = Integer.parseInt(eId) == Integer.parseInt(sUid);
                } catch (Exception ignored) {}
            }
            return isSame;
        }

        String uId = r.getUserId() != null ? r.getUserId().trim() : "";
        if (!uId.isEmpty() && !"--".equals(uId) && !"null".equalsIgnoreCase(uId)) {
            boolean isSameUser = uId.equalsIgnoreCase(sUid);
            if (!isSameUser) {
                try {
                    isSameUser = Integer.parseInt(uId) == Integer.parseInt(sUid);
                } catch (Exception ignored) {}
            }
            if (isSameUser) {
                String workerName = r.getFirstName() != null ? r.getFirstName().trim() : "";
                if (workerName.isEmpty() || "User".equalsIgnoreCase(workerName) || "Supervisor".equalsIgnoreCase(workerName)) {
                    return true;
                }
            }
        }

        if (!TextUtils.isEmpty(r.getFirstName()) && !TextUtils.isEmpty(supervisorName)) {
            String name = r.getFirstName().trim();
            String supName = supervisorName.trim();
            if (supName.equalsIgnoreCase(name)) {
                if (!eId.isEmpty() && !sUid.isEmpty() && !eId.equalsIgnoreCase(sUid)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isSupervisorSelfMoveRecord(MoveRecordModel r) {
        if (r == null || supervisorUid == null || supervisorUid.trim().isEmpty()) return false;
        String sUid = supervisorUid.trim();
        String eId = r.getEmpId() != null ? r.getEmpId().trim() : "";
        if (!eId.isEmpty()) {
            if (eId.equalsIgnoreCase(sUid)) return true;
            try {
                if (Integer.parseInt(eId) == Integer.parseInt(sUid)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void processSupervisorAttendanceFeed(String userName, 
                                              List<AttendanceRecordModel> attendanceRecords, 
                                              List<MoveRecordModel> moveRecords) {
        if (progress != null) progress.setVisibility(View.GONE);
        // 1. Process main site attendance record
        AttendanceRecordModel selfRecord = null;
        if (attendanceRecords != null && !attendanceRecords.isEmpty()) {
            List<AttendanceRecordModel> supervisorRecords = new ArrayList<>();
            for (AttendanceRecordModel r : attendanceRecords) {
                if (isSupervisorSelfRecord(r)) {
                    supervisorRecords.add(r);
                }
            }
            if (!supervisorRecords.isEmpty()) {
                selfRecord = supervisorRecords.get(0);
                for (int i = 1; i < supervisorRecords.size(); i++) {
                    AttendanceRecordModel cand = supervisorRecords.get(i);
                    if (shouldPreferRecord(cand, selfRecord)) {
                        selfRecord = cand;
                    }
                }
            }
        }
        // 2. Process active supervisor move site record from feed
        MoveRecordModel feedActiveMove = null;
        if (moveRecords != null && !moveRecords.isEmpty()) {
            for (MoveRecordModel m : moveRecords) {
                if (m != null && isSupervisorSelfMoveRecord(m) && m.isActive()) {
                    feedActiveMove = m;
                    break;
                }
            }
        }
        currentSelfRecord = selfRecord;

        // Verify move status directly with api_get_move_employees.php
        verifyMoveStatusWithApi(selfRecord, feedActiveMove);
    }

    private void verifyMoveStatusWithApi(AttendanceRecordModel selfRecord, MoveRecordModel feedActiveMove) {
        Context context = getContext();
        if (context == null || TextUtils.isEmpty(supervisorUid)) {
            currentActiveMoveRecord = feedActiveMove;
            updateSelfUiWithMovement(selfRecord, feedActiveMove);
            return;
        }

        String locId = sessionPrefs != null ? sessionPrefs.getLocationId() : "";
        moveSiteApi.fetchMoveEmployees(context, "", locId, new MoveSiteApi.EmployeesCallback() {
            @Override
            public void onSuccess(List<MoveEmployeeModel> employees, String locationName, int readyCount, int movingCount) {
                if (!isAdded()) return;
                MoveRecordModel verifiedActiveMove = null;
                if (employees != null) {
                    for (MoveEmployeeModel emp : employees) {
                        if (emp != null && isSupervisorEmployeeMatch(emp.id) && emp.onMove) {
                            String mSiteName = !TextUtils.isEmpty(locationName) ? locationName : (feedActiveMove != null ? feedActiveMove.getProjName() : "Moved Site");
                            verifiedActiveMove = new MoveRecordModel(
                                    !TextUtils.isEmpty(emp.moveId) ? emp.moveId : (feedActiveMove != null ? feedActiveMove.getMoveId() : "1"),
                                    emp.id,
                                    !TextUtils.isEmpty(emp.firstName) ? emp.firstName : supervisorName,
                                    mSiteName,
                                    emp.timeInDisplay,
                                    emp.timeOutDisplay,
                                    true,
                                    false,
                                    emp.photoUrl,
                                    ""
                            );
                            break;
                        }
                    }
                }

                if (verifiedActiveMove != null) {
                    currentActiveMoveRecord = verifiedActiveMove;
                    updateSelfUiWithMovement(selfRecord, verifiedActiveMove);
                } else if (employees != null) {
                    currentActiveMoveRecord = null;
                    updateSelfUiWithMovement(selfRecord, null);
                } else {
                    currentActiveMoveRecord = feedActiveMove;
                    updateSelfUiWithMovement(selfRecord, feedActiveMove);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                currentActiveMoveRecord = feedActiveMove;
                updateSelfUiWithMovement(selfRecord, feedActiveMove);
            }
        });
    }

    private boolean isSupervisorEmployeeMatch(String empId) {
        if (empId == null || supervisorUid == null) return false;
        String eId = empId.trim();
        String sUid = supervisorUid.trim();
        if (eId.isEmpty() || sUid.isEmpty()) return false;
        if (eId.equalsIgnoreCase(sUid)) return true;
        try {
            return Integer.parseInt(eId) == Integer.parseInt(sUid);
        } catch (Exception ignored) {}
        return false;
    }


    private boolean shouldPreferRecord(AttendanceRecordModel cand, AttendanceRecordModel current) {
        if (current == null) return true;
        if (cand == null) return false;

        boolean candActive = cand.isActive();
        boolean currActive = current.isActive();

        // If current is completed (has both IN and OUT), check if cand is an orphaned active record created at checkout time
        if (!currActive && candActive) {
            String currOut = current.getTimeOut() != null ? current.getTimeOut().trim() : "";
            String candIn = cand.getTimeIn() != null ? cand.getTimeIn().trim() : "";
            if (!currOut.isEmpty() && !candIn.isEmpty() && isTimeEqualOrClose(candIn, currOut)) {
                // cand is an orphaned active record created by checkout; prefer the true completed record
                return false;
            }
            return true;
        }

        if (!candActive && currActive) {
            String candOut = cand.getTimeOut() != null ? cand.getTimeOut().trim() : "";
            String currIn = current.getTimeIn() != null ? current.getTimeIn().trim() : "";
            if (!candOut.isEmpty() && !currIn.isEmpty() && isTimeEqualOrClose(currIn, candOut)) {
                return true;
            }
            return false;
        }

        // Both active or both completed: prefer higher attendId (latest record)
        try {
            long candId = Long.parseLong(cand.getAttendId());
            long currId = Long.parseLong(current.getAttendId());
            if (candId > currId) return true;
            if (candId < currId) return false;
        } catch (Exception ignored) {}

        // Fallback: prefer completed record over active record if cand is completed
        if (!candActive && currActive) return true;

        return false;
    }

    private boolean isTimeEqualOrClose(String time1, String time2) {
        if (time1 == null || time2 == null) return false;
        String t1 = time1.trim();
        String t2 = time2.trim();
        if (t1.isEmpty() || t2.isEmpty()) return false;
        if (t1.equalsIgnoreCase(t2)) return true;

        try {
            String formatted1 = formatTime(t1);
            String formatted2 = formatTime(t2);
            return formatted1.equalsIgnoreCase(formatted2);
        } catch (Exception ignored) {}

        return false;
    }

   private void updateSelfUiWithMovement(AttendanceRecordModel attendanceRecord, MoveRecordModel activeMove) {
    if (activeMove != null && activeMove.isActive()) {
        if (tvSelfTimeIn != null) tvSelfTimeIn.setText(formatTime(activeMove.getInTime()));
        if (tvSelfTimeOut != null) tvSelfTimeOut.setText(formatTime(activeMove.getOutTime()));

        String siteName = activeMove.getProjName();
        if (TextUtils.isEmpty(siteName)) siteName = "Moved Site";
        if (tvSelfProject != null) {
            tvSelfProject.setText("Project: " + siteName + " (Moved Site)");
        }

        if (tvSelfStatusBadge != null) {
            tvSelfStatusBadge.setText("ACTIVE (MOVED SITE)");
            tvSelfStatusBadge.setBackgroundResource(R.drawable.bg_luxury_status_active);
            tvSelfStatusBadge.setTextColor(0xFFFFFFFF);
        }

        if (btnSelfTimeIn != null) btnSelfTimeIn.setVisibility(View.GONE);
        if (btnSelfTimeOut != null) {
            btnSelfTimeOut.setVisibility(View.VISIBLE);
            btnSelfTimeOut.setText("SELF MOVE OUT");
        }
        if (btnSelfMoveSite != null) btnSelfMoveSite.setVisibility(View.GONE);
        return;
    }

    if (btnSelfTimeOut != null) {
        btnSelfTimeOut.setText("SELF TIME OUT");
    }
    updateSelfUi(attendanceRecord);
}



    private void performSupervisorMoveOut(MoveRecordModel record) {
    Context context = getContext();
    if (context == null || record == null || !isAdded()) return;

    String targetEmpId = !TextUtils.isEmpty(record.getEmpId()) ? record.getEmpId() : supervisorUid;

    SessionPrefs session = new SessionPrefs(context);
    if (!session.isLocationSessionValid()) {
        Toast.makeText(context, "Location verification is required before marking move out.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", false);
        intent.putExtra("IS_CHANGE_SITE", false);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("uid", supervisorUid);
        intent.putExtra("empid", targetEmpId);
        intent.putExtra("pending_eid", targetEmpId);
        intent.putExtra("move_id", record.getMoveId());
        intent.putExtra("moveid", record.getMoveId());
        intent.putExtra("type", "OUT");
        intent.putExtra("pending_action", "OUT");
        intent.putExtra("is_movement", true);
        startActivityForResult(intent, 1005);
        return;
    }

    String freshToken = session.issueFreshVerificationToken();
    String targetLocId = session.getLocationId();
    String pName = session.getProjectName();
    if (pName == null || pName.trim().isEmpty()) pName = record.getProjName();

    Intent intent = new Intent(context, LocationActivity.class);
    intent.putExtra("empid", targetEmpId);
    intent.putExtra("move_id", record.getMoveId());
    intent.putExtra("emp_name", !TextUtils.isEmpty(supervisorName) ? supervisorName : record.getFirstName());
    intent.putExtra("photo_url", record.getInPhotoUrl());
    intent.putExtra("type", "OUT");
    intent.putExtra("is_movement", true);
    intent.putExtra("uid", supervisorUid);
    intent.putExtra("project_id", targetLocId);
    intent.putExtra("departmentid", targetLocId);
    intent.putExtra("projname", pName);
    intent.putExtra("project_name", pName);
    intent.putExtra("locationid", targetLocId);
    intent.putExtra("loc_id", targetLocId);
    intent.putExtra("MATCHED_LOC_ID", targetLocId);
    intent.putExtra("VERIFIED", "true");
    intent.putExtra("VERIFICATION_TOKEN", freshToken);
    startActivityForResult(intent, 1005);
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
                                displayName = !TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor #" + supervisorUid;
                            }
                            historyList.add(new AttendanceRecordModel(
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
                            ));
                        }
                    }
                }

                if (moveRecords != null) {
    for (MoveRecordModel m : moveRecords) {
        if (m != null && isSupervisorSelfMoveRecord(m)) {
            String displayName = !TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor #" + supervisorUid;
            String siteName = m.getProjName();
            if (TextUtils.isEmpty(siteName)) siteName = "Moved Site";
            if (!siteName.toLowerCase().contains("moved")) {
                siteName = siteName + " (Moved Site)";
            }

            historyList.add(new AttendanceRecordModel(
                    "MOVE_" + m.getMoveId(),
                    supervisorUid,
                    m.getEmpId(),
                    displayName,
                    siteName,
                    m.getInTime(),
                    m.getOutTime(),
                    0.0,
                    m.isHasIn(),
                    m.isHasOut(),
                    m.getInPhotoUrl(),
                    m.getOutPhotoUrl()
            ));
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
        if (rawTime == null || rawTime.trim().isEmpty() || "--".equals(rawTime.trim()) || "--:--".equals(rawTime.trim())
                || "00:00:00".equals(rawTime.trim()) || "00.00.00".equals(rawTime.trim()) || "00:00".equals(rawTime.trim()) || "null".equalsIgnoreCase(rawTime.trim())) {
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
        String selfAttendId = (currentSelfRecord != null && !TextUtils.isEmpty(currentSelfRecord.getAttendId()))
                ? currentSelfRecord.getAttendId() : "";

        if (!sessionPrefs.isLocationSessionValid()) {
            Toast.makeText(context, AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            launchLocationVerifyForSessionWithAction(punchType, sUid, selfAttendId);
            return;
        }

        // Location session is verified — issue a fresh token & open LocationActivity (camera & photo capture submit screen)
        String freshToken = sessionPrefs.issueFreshVerificationToken();

        Intent intent = new Intent(context, com.app.fourscontracting.LocationActivity.class);
        intent.putExtra("empid", sUid);
        intent.putExtra("emp_name", !TextUtils.isEmpty(supervisorName) ? supervisorName : "Supervisor");
        intent.putExtra("type", punchType);
        intent.putExtra("uid", sUid);
        if (!TextUtils.isEmpty(selfAttendId)) {
            intent.putExtra("attend_id", selfAttendId);
            intent.putExtra("attendance_id", selfAttendId);
            intent.putExtra("id", selfAttendId);
        }
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

    private void launchLocationVerifyForSessionWithAction(String punchType, String targetEmpId) {
        launchLocationVerifyForSessionWithAction(punchType, targetEmpId, "");
    }

    private void launchLocationVerifyForSessionWithAction(String punchType, String targetEmpId, String attendId) {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", false);
        intent.putExtra("IS_CHANGE_SITE", false);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("uid", getSupervisorUid());
        intent.putExtra("empid", targetEmpId);
        intent.putExtra("pending_eid", targetEmpId);
        intent.putExtra("type", punchType);
        intent.putExtra("pending_action", punchType);
        if (attendId != null && !attendId.isEmpty()) {
            intent.putExtra("attend_id", attendId);
            intent.putExtra("attendance_id", attendId);
        }
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

        String sUid = getSupervisorUid();
        String targetWorkerId = "";
        if (isSupervisorSelfRecord(record)) {
            targetWorkerId = sUid;
        } else {
            targetWorkerId = com.app.fourscontracting.ui.attendance.ManageAttendanceFragment.resolveTargetWorkerId(record, sUid);
        }

        if (TextUtils.isEmpty(targetWorkerId) || "--".equals(targetWorkerId.trim())) {
            Toast.makeText(context, "Employee ID is missing for this attendance record.", Toast.LENGTH_LONG).show();
            return;
        }

        if (sessionPrefs == null) sessionPrefs = new SessionPrefs(context);
        if (!sessionPrefs.isLocationSessionValid()) {
            Toast.makeText(context, AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            launchLocationVerifyForSessionWithAction("OUT", targetWorkerId, record.getAttendId());
            return;
        }

        String freshToken = sessionPrefs.issueFreshVerificationToken();
        String targetLocId = sessionPrefs.getLocationId();
        String targetProjName = sessionPrefs.getProjectName();
        if (TextUtils.isEmpty(targetProjName)) targetProjName = record.getProjName();

        Intent intent = new Intent(context, com.app.fourscontracting.LocationActivity.class);
        intent.putExtra("empid", targetWorkerId);
        intent.putExtra("pending_eid", targetWorkerId);
        if (record.getAttendId() != null && !record.getAttendId().isEmpty()) {
            intent.putExtra("attend_id", record.getAttendId());
            intent.putExtra("attendance_id", record.getAttendId());
            intent.putExtra("id", record.getAttendId());
        }
        intent.putExtra("emp_name", record.getFirstName());
        intent.putExtra("photo_url", record.getInPhotoUrl());
        intent.putExtra("type", "OUT");
        intent.putExtra("uid", getSupervisorUid());
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
