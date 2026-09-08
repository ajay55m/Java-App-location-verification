package com.app.fourscontracting.ui.move;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.app.fourscontracting.LocationVerifyActivity;
import com.app.fourscontracting.Project;
import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.UserLocation;
import com.app.fourscontracting.UserProject;
import com.app.fourscontracting.data.DepartmentModel;
import com.app.fourscontracting.data.MoveEmployeeModel;
import com.app.fourscontracting.data.MoveSiteApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 100% Native Move Site Fragment UI and Controller.
 * Displays site location status, ready/moving summary statistics,
 * department filter dropdown, and employee move cards.
 */
public class MoveSiteFragment extends Fragment implements MoveEmployeeAdapter.Listener {

    private static final int REQUEST_CODE_SUPERVISOR_VERIFY = 1001;
    private static final int REQUEST_CODE_MOVE_PUNCH = 1002;

    private UserLocalStore userLocalStore;
    private final MoveSiteApi api = new MoveSiteApi();
    private MoveEmployeeAdapter adapter;

    private TextView tvLocation;
    private TextView tvReadyCount;
    private TextView tvMovingCount;
    private TextView tvProjectName;
    private TextView tvEmpty;
    private View emptyContainer;
    private ProgressBar progress;
    private Spinner spinnerDept;
    private RecyclerView recyclerView;

    private String uid = "";
    private String projId = "";
    private String selectedDeptId = "";

    private final List<MoveEmployeeModel> moveEmployeeList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_move_site, container, false);
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

        tvLocation = view.findViewById(R.id.tv_move_location);
        tvReadyCount = view.findViewById(R.id.tv_ready_count);
        tvMovingCount = view.findViewById(R.id.tv_moving_count);
        tvProjectName = view.findViewById(R.id.tv_move_project);
        tvEmpty = view.findViewById(R.id.tv_empty_move);
        emptyContainer = view.findViewById(R.id.empty_move_container);
        progress = view.findViewById(R.id.progress_move);
        spinnerDept = view.findViewById(R.id.spinner_department);
        recyclerView = view.findViewById(R.id.rv_move_employees);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new MoveEmployeeAdapter(this);
            recyclerView.setAdapter(adapter);
        }

        View btnChangeSite = view.findViewById(R.id.btn_change_site);
        if (btnChangeSite != null) {
            btnChangeSite.setOnClickListener(v -> launchLocationVerifyManualChangeSite());
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

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getResolvedSiteName() {
        if (!isAdded()) return "Verified Site";
        SessionPrefs session = new SessionPrefs(requireContext());
        String pName = session.getProjectName();
        if (pName != null && !pName.trim().isEmpty() && !isNumeric(pName.trim())) {
            return pName.trim();
        }

        UserLocation userLoc = userLocalStore.getLoggedInUserLocation();
        if (userLoc != null && userLoc.locationname != null && !userLoc.locationname.trim().isEmpty() && !isNumeric(userLoc.locationname.trim())) {
            return userLoc.locationname.trim();
        }

        UserProject userProj = userLocalStore.getLoggedInUserProject();
        if (userProj != null && userProj.projectname != null && !userProj.projectname.trim().isEmpty() && !isNumeric(userProj.projectname.trim())) {
            return userProj.projectname.trim();
        }

        String targetId = (projId != null && !projId.trim().isEmpty()) ? projId.trim() : session.getProjectId();
        List<Project> cachedProjects = session.getCachedProjectList(true);
        if (cachedProjects != null) {
            for (Project p : cachedProjects) {
                if (p != null && p.name != null && !isNumeric(p.name.trim())) {
                    if ((p.id != null && p.id.trim().equalsIgnoreCase(targetId)) ||
                        (p.name != null && p.name.trim().equalsIgnoreCase(targetId))) {
                        return p.name.trim();
                    }
                }
            }
        }

        return "Verified Site";
    }

    private void checkLocationSessionAndInit() {
        if (!isAdded()) return;
        SessionPrefs session = new SessionPrefs(requireContext());
        String resolvedSite = getResolvedSiteName();
        if (tvLocation != null) {
            tvLocation.setText(resolvedSite);
        }
        if (tvProjectName != null) {
            tvProjectName.setText(resolvedSite);
        }

        if (!session.isLocationSessionValid()) {
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("Location verification is required to access Move Site.");
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            launchLocationVerifyInitialAuto();
        } else {
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            loadDepartments();
        }
    }

    private void launchLocationVerifyInitialAuto() {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", true);
        intent.putExtra("IS_CHANGE_SITE", false);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("uid", uid);
        startActivityForResult(intent, REQUEST_CODE_SUPERVISOR_VERIFY);
    }

    private void launchLocationVerifyManualChangeSite() {
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", false);
        intent.putExtra("IS_CHANGE_SITE", true);
        intent.putExtra("AUTO_START_VERIFY", false);
        intent.putExtra("uid", uid);
        startActivityForResult(intent, REQUEST_CODE_SUPERVISOR_VERIFY);
    }

    private void loadDepartments() {
        if (progress != null) progress.setVisibility(View.VISIBLE);
        api.fetchDepartments(requireContext(), new MoveSiteApi.DepartmentsCallback() {
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
                if (spinnerDept != null) {
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
            if (tvReadyCount != null) tvReadyCount.setText("0");
            if (tvMovingCount != null) tvMovingCount.setText("0");
            moveEmployeeList.clear();
            if (adapter != null) adapter.submit(new ArrayList<>());
            if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
            if (tvEmpty != null) tvEmpty.setText("Select a department to view move site workers");
            return;
        }

        SessionPrefs session = new SessionPrefs(requireContext());
        String locationId = session.getLocationId();

        api.fetchMoveEmployees(requireContext(), selectedDeptId, locationId, new MoveSiteApi.EmployeesCallback() {
            @Override
            public void onSuccess(List<MoveEmployeeModel> employees, String locationName, int readyCount, int movingCount) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);

                if (tvReadyCount != null) tvReadyCount.setText(String.valueOf(readyCount));
                if (tvMovingCount != null) tvMovingCount.setText(String.valueOf(movingCount));
                if (!TextUtils.isEmpty(locationName) && !isNumeric(locationName)) {
                    if (tvLocation != null) tvLocation.setText(locationName);
                }

                moveEmployeeList.clear();
                if (employees != null) moveEmployeeList.addAll(employees);

                if (adapter != null) adapter.submit(moveEmployeeList);

                if (moveEmployeeList.isEmpty()) {
                    if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
                    if (tvEmpty != null) tvEmpty.setText("No workers available for move site in this department");
                } else {
                    if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);

                String userFriendlyMsg = message;
                if (message != null && (message.contains("Missing Dept") || message.toLowerCase().contains("missing"))) {
                    userFriendlyMsg = "Select a department to view move site workers";
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }

                if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
                if (tvEmpty != null) tvEmpty.setText(userFriendlyMsg);
            }
        });
    }

    @Override
    public void onAction(MoveEmployeeModel employee) {
        if (employee == null) return;
        SessionPrefs session = new SessionPrefs(requireContext());
        if (!session.isLocationSessionValid()) {
            Toast.makeText(requireContext(), AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            launchLocationVerifyInitialAuto();
            return;
        }

        String actionType = employee.onMove ? "OUT" : "MOVE";
        Intent intent = new Intent(requireContext(), LocationVerifyActivity.class);
        intent.putExtra("empid", employee.id);
        intent.putExtra("emp_name", employee.firstName);
        intent.putExtra("photo_url", employee.photoUrl);
        intent.putExtra("type", actionType);
        intent.putExtra("is_movement", true);
        intent.putExtra("uid", uid);
        intent.putExtra("selected_project_id", projId);
        intent.putExtra("AUTO_START_VERIFY", true);
        intent.putExtra("target_location_id", session.getLocationId());
        intent.putExtra("pending_eid", employee.id);
        intent.putExtra("pending_action", actionType);
        startActivityForResult(intent, REQUEST_CODE_MOVE_PUNCH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SUPERVISOR_VERIFY || requestCode == REQUEST_CODE_MOVE_PUNCH) {
            SessionPrefs session = new SessionPrefs(requireContext());
            if (session.isLocationSessionValid()) {
                String pName = getResolvedSiteName();
                if (tvLocation != null) tvLocation.setText(pName);
                if (tvProjectName != null) tvProjectName.setText(pName);
                loadDepartments();
            } else {
                Toast.makeText(requireContext(), AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            }
        }
    }
}
