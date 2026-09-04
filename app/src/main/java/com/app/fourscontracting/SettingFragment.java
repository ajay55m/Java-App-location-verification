package com.app.fourscontracting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.fourscontracting.data.DepartmentModel;
import com.app.fourscontracting.data.LabourEmployeeAdapter;
import com.app.fourscontracting.data.LabourEmployeeModel;
import com.app.fourscontracting.data.LabourManagementApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully native Labour Management screen (no WebView, no employee_details.php).
 * Displays site status, stats strip, department filter, and worker list with IN/OUT buttons.
 */
public class SettingFragment extends Fragment implements LabourEmployeeAdapter.Listener {

    private static final int REQUEST_CODE_SUPERVISOR_VERIFY = 999;
    private static final int REQUEST_CODE_ATTENDANCE_PUNCH = 1005;

    private UserLocalStore userLocalStore;
    private final LabourManagementApi api = new LabourManagementApi();
    private LabourEmployeeAdapter adapter;

    private TextView tvLocation;
    private TextView tvTotalCount;
    private TextView tvInCount;
    private TextView tvOutCount;
    private TextView tvEmpty;
    private View emptyContainer;
    private ProgressBar progress;
    private Spinner spinnerDept;
    private EditText etSearch;
    private RecyclerView recyclerView;

    private String uid = "";
    private String projId = "";
    private String selectedDeptId = "";

    private final List<LabourEmployeeModel> fullEmployeeList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userLocalStore = new UserLocalStore(requireContext());
        User user = userLocalStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        uid = val_list.length > 0 ? val_list[0] : "";
        projId = userLocalStore.getLoggedInUserProjectID();

        tvLocation = view.findViewById(R.id.tv_labour_location);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvInCount = view.findViewById(R.id.tv_in_count);
        tvOutCount = view.findViewById(R.id.tv_out_count);
        tvEmpty = view.findViewById(R.id.tv_empty_labour);
        emptyContainer = view.findViewById(R.id.empty_labour_container);
        progress = view.findViewById(R.id.progress_labour);
        spinnerDept = view.findViewById(R.id.spinner_department);
        etSearch = view.findViewById(R.id.et_search_worker);
        recyclerView = view.findViewById(R.id.rv_labour_employees);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LabourEmployeeAdapter(this);
        recyclerView.setAdapter(adapter);

        View btnChangeSite = view.findViewById(R.id.btn_change_site);
        if (btnChangeSite != null) {
            btnChangeSite.setOnClickListener(v -> launchLocationVerify());
        }

        // Live Search Filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterEmployees(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        checkLocationSessionAndInit();
    }

    public void refreshList() {
        loadEmployees();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLocationSessionAndInit();
    }

    private void checkLocationSessionAndInit() {
        if (!isAdded()) return;
        SessionPrefs session = new SessionPrefs(requireContext());
        String pName = session.getProjectName();
        if (pName != null && !pName.isEmpty()) {
            tvLocation.setText(pName);
        } else {
            tvLocation.setText(projId.isEmpty() ? "Verified Site" : projId);
        }

        if (!session.isLocationSessionValid()) {
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("Location verification is required to access Labour Management.");
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            launchLocationVerify();
        } else {
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            loadDepartments();
        }
    }

    private void launchLocationVerify() {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", true);
        intent.putExtra("uid", uid);
        startActivityForResult(intent, REQUEST_CODE_SUPERVISOR_VERIFY);
    }

    private void loadDepartments() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        api.fetchDepartments(requireContext(), new LabourManagementApi.DepartmentsCallback() {
            @Override
            public void onSuccess(List<DepartmentModel> departments) {
                if (!isAdded()) return;
                List<DepartmentModel> fullList = new ArrayList<>();
                fullList.add(new DepartmentModel("", "-- Select Department --"));
                if (departments != null) fullList.addAll(departments);

                ArrayAdapter<DepartmentModel> deptAdapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        fullList
                );
                deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDept.setAdapter(deptAdapter);

                spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                        DepartmentModel sel = fullList.get(position);
                        selectedDeptId = sel.id;
                        loadEmployees();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                // Auto-select department 5 (4s Contracting Employees) on initial load matching web model
                if (fullList.size() > 1) {
                    int defaultPos = 0;
                    for (int i = 0; i < fullList.size(); i++) {
                        if ("5".equals(fullList.get(i).id)) {
                            defaultPos = i;
                            break;
                        }
                    }
                    if (defaultPos == 0 && fullList.size() > 1) {
                        defaultPos = 1;
                    }
                    if (defaultPos > 0) {
                        spinnerDept.setSelection(defaultPos);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (message != null && !message.contains("Missing Dept")) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadEmployees() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);

        if (TextUtils.isEmpty(selectedDeptId)) {
            if (progress != null) progress.setVisibility(View.GONE);
            tvTotalCount.setText("0");
            tvInCount.setText("0");
            tvOutCount.setText("0");
            fullEmployeeList.clear();
            adapter.setItems(new ArrayList<>());
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("Select a department to view workers");
            return;
        }

        SessionPrefs session = new SessionPrefs(requireContext());
        String locationId = session.getLocationId();

        api.fetchEmployees(requireContext(), uid, locationId, selectedDeptId, new LabourManagementApi.EmployeesCallback() {
            @Override
            public void onSuccess(List<LabourEmployeeModel> employees, int totalCount, int inCount, int outCount) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);

                tvTotalCount.setText(String.valueOf(totalCount));
                tvInCount.setText(String.valueOf(inCount));
                tvOutCount.setText(String.valueOf(outCount));

                fullEmployeeList.clear();
                if (employees != null) fullEmployeeList.addAll(employees);

                String currentQuery = etSearch != null ? etSearch.getText().toString() : "";
                filterEmployees(currentQuery);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);

                String userFriendlyMsg = message;
                if (message != null && (message.contains("Missing Dept") || message.contains("Missing Dept ID") || message.toLowerCase().contains("missing"))) {
                    userFriendlyMsg = "Select a department to view workers";
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }

                if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
                if (tvEmpty != null) tvEmpty.setText(userFriendlyMsg);
            }
        });
    }

    private void filterEmployees(String query) {
        if (fullEmployeeList.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("No workers found");
            return;
        }

        if (TextUtils.isEmpty(query)) {
            adapter.setItems(fullEmployeeList);
            if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
            return;
        }

        String q = query.trim().toLowerCase();
        List<LabourEmployeeModel> filtered = new ArrayList<>();
        for (LabourEmployeeModel item : fullEmployeeList) {
            if (item.getName().toLowerCase().contains(q) || item.getId().toLowerCase().contains(q)) {
                filtered.add(item);
            }
        }

        adapter.setItems(filtered);
        if (filtered.isEmpty()) {
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("No matching workers found");
        } else {
            if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTimeInClicked(LabourEmployeeModel item) {
        markAttendanceNative(item, "IN");
    }

    @Override
    public void onTimeOutClicked(LabourEmployeeModel item) {
        markAttendanceNative(item, "OUT");
    }

    private void markAttendanceNative(LabourEmployeeModel item, String actionType) {
        SessionPrefs session = new SessionPrefs(requireContext());
        if (!session.isLocationSessionValid()) {
            Toast.makeText(requireContext(), AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            launchLocationVerify();
            return;
        }

        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("empid", item.getId());
        intent.putExtra("emp_name", item.getName());
        intent.putExtra("photo_url", item.getPhotoUrl());
        intent.putExtra("type", actionType);
        intent.putExtra("uid", uid);
        intent.putExtra("selected_project_id", projId);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("target_location_id", session.getLocationId());
        intent.putExtra("pending_eid", item.getId());
        intent.putExtra("pending_action", actionType);
        startActivityForResult(intent, REQUEST_CODE_ATTENDANCE_PUNCH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SUPERVISOR_VERIFY || requestCode == REQUEST_CODE_ATTENDANCE_PUNCH) {
            SessionPrefs session = new SessionPrefs(requireContext());
            if (session.isLocationSessionValid()) {
                String pName = session.getProjectName();
                if (pName != null && !pName.isEmpty()) tvLocation.setText(pName);
                loadDepartments();
            } else {
                Toast.makeText(requireContext(), AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            }
        }
    }
}