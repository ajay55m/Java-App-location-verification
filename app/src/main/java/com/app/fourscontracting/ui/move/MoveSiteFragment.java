package com.app.fourscontracting.ui.move;

import android.content.Context;
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
import com.app.fourscontracting.R;
import com.app.fourscontracting.SessionPrefs;
import com.app.fourscontracting.User;
import com.app.fourscontracting.UserLocalStore;
import com.app.fourscontracting.UserLocation;
import com.app.fourscontracting.UserProject;
import com.app.fourscontracting.WebviewActivity;
import com.app.fourscontracting.data.DepartmentModel;
import com.app.fourscontracting.data.MoveEmployeeModel;
import com.app.fourscontracting.data.MoveSiteApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully native Move Site (no WebView, no employee_details.php).
 * Lists only employees who already completed today's primary IN + OUT.
 */
public class MoveSiteFragment extends Fragment implements MoveEmployeeAdapter.Listener {

    private static final int REQUEST_CODE_SUPERVISOR_VERIFY = 1001;

    private UserLocalStore userLocalStore;
    private final MoveSiteApi api = new MoveSiteApi();
    private MoveEmployeeAdapter adapter;

    private TextView tvLocation;
    private TextView tvProject;
    private TextView tvReady;
    private TextView tvMoving;
    private TextView tvEmpty;
    private View emptyContainer;
    private ProgressBar progress;
    private Spinner spinnerDept;
    private RecyclerView recyclerView;

    private String managerUid = "";
    private String locationId = "";
    private final List<DepartmentModel> departments = new ArrayList<>();
    private boolean spinnerReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_move_site, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userLocalStore = new UserLocalStore(requireContext());

        tvLocation = view.findViewById(R.id.tv_move_location);
        tvProject = view.findViewById(R.id.tv_move_project);
        tvReady = view.findViewById(R.id.tv_ready_count);
        tvMoving = view.findViewById(R.id.tv_moving_count);
        tvEmpty = view.findViewById(R.id.tv_empty_move);
        emptyContainer = view.findViewById(R.id.empty_move_container);
        progress = view.findViewById(R.id.progress_move);
        spinnerDept = view.findViewById(R.id.spinner_department);
        recyclerView = view.findViewById(R.id.rv_move_employees);
        View btnChangeSite = view.findViewById(R.id.btn_change_site);

        adapter = new MoveEmployeeAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        bindSessionHeader();
        if (btnChangeSite != null) {
            btnChangeSite.setOnClickListener(v -> startChangeSite());
        }

        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerReady) return;
                if (position <= 0) {
                    adapter.submit(new ArrayList<>());
                    showEmpty("Select a department");
                    tvReady.setText("0");
                    tvMoving.setText("0");
                    return;
                }
                DepartmentModel dept = departments.get(position - 1);
                loadEmployees(dept.id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ensureLocationThenLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindSessionHeader();
        if (getActivity() instanceof WebviewActivity) {
            ((WebviewActivity) getActivity()).updateToolbarTitle("Move Site");
        }
        refreshList();
    }

    /** Reload current department list (used after punch / network restore). */
    public void refreshList() {
        if (!isAdded() || spinnerDept == null) return;
        int pos = spinnerDept.getSelectedItemPosition();
        if (pos > 0 && pos - 1 < departments.size()) {
            loadEmployees(departments.get(pos - 1).id);
        }
    }

    private void bindSessionHeader() {
        User user = userLocalStore.getLoggedInUser();
        String[] parts = UserLocalStore.parseUserInfo(user != null ? user.username : "");
        managerUid = parts.length > 0 ? parts[0] : "";

        SessionPrefs session = new SessionPrefs(requireContext());
        locationId = session.getLocationId();

        UserProject project = userLocalStore.getLoggedInUserProject();
        String projectName = project != null ? project.projectname : "";
        if (TextUtils.isEmpty(projectName)) {
            projectName = session.getProjectName();
        }
        if (tvProject != null) {
            tvProject.setText(TextUtils.isEmpty(projectName) ? "—" : projectName);
        }

        UserLocation location = userLocalStore.getLoggedInUserLocation();
        String locName = location != null ? location.locationname : "";
        if (tvLocation != null) {
            if (!TextUtils.isEmpty(projectName)) {
                tvLocation.setText(projectName);
            } else if (!TextUtils.isEmpty(locName)) {
                tvLocation.setText(locName);
            } else if (!TextUtils.isEmpty(locationId)) {
                tvLocation.setText("Site #" + locationId);
            } else {
                tvLocation.setText("No Site Selected");
            }
        }
    }

    private void ensureLocationThenLoad() {
        SessionPrefs session = new SessionPrefs(requireContext());
        if (!session.isLocationSessionValid()) {
            Intent intent = new Intent(requireActivity(), LocationVerifyActivity.class);
            intent.putExtra("IS_INITIAL_VERIFY", true);
            intent.putExtra("uid", managerUid);
            startActivityForResult(intent, REQUEST_CODE_SUPERVISOR_VERIFY);
        } else {
            locationId = session.getLocationId();
            loadDepartments();
        }
    }

    private void loadDepartments() {
        setLoading(true);
        api.fetchDepartments(requireContext(), new MoveSiteApi.DepartmentsCallback() {
            @Override
            public void onSuccess(List<DepartmentModel> list) {
                if (!isAdded()) return;
                setLoading(false);
                departments.clear();
                departments.addAll(list);

                List<String> labels = new ArrayList<>();
                labels.add("-- Choose Department --");
                for (DepartmentModel d : departments) labels.add(d.name);

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, labels);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerReady = false;
                spinnerDept.setAdapter(spinnerAdapter);
                spinnerReady = true;
                showEmpty("Select a department");
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                setLoading(false);
                if (message != null && !message.contains("Missing Dept")) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
                showEmpty("Select a department to view workers");
            }
        });
    }

    private void loadEmployees(String deptId) {
        setLoading(true);
        api.fetchMoveEmployees(requireContext(), deptId, locationId, new MoveSiteApi.EmployeesCallback() {
            @Override
            public void onSuccess(List<MoveEmployeeModel> employees, String locationName,
                                  int readyCount, int movingCount) {
                if (!isAdded()) return;
                setLoading(false);
                // Keep short project name in hero card; stats already show project
                tvReady.setText(String.valueOf(readyCount));
                tvMoving.setText(String.valueOf(movingCount));
                adapter.submit(employees);
                if (employees == null || employees.isEmpty()) {
                    showEmpty("No staff ready to move.\nFinish daily IN + OUT on Labour first.");
                } else {
                    hideEmpty();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                setLoading(false);
                adapter.submit(new ArrayList<>());
                String userFriendlyMsg = message;
                if (message != null && (message.contains("Missing Dept") || message.contains("Missing Dept ID") || message.toLowerCase().contains("missing"))) {
                    userFriendlyMsg = "Select a department to view workers";
                } else {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
                showEmpty(userFriendlyMsg);
            }
        });
    }

    private void showEmpty(String message) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
        }
        if (emptyContainer != null) {
            emptyContainer.setVisibility(View.VISIBLE);
        } else if (tvEmpty != null) {
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmpty() {
        if (emptyContainer != null) {
            emptyContainer.setVisibility(View.GONE);
        } else if (tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean loading) {
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            hideEmpty();
        }
    }

    private void startChangeSite() {
        if (TextUtils.isEmpty(managerUid) || "0".equals(managerUid)) {
            Toast.makeText(getContext(), AppMessages.USER_SESSION_MISSING, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(requireActivity(), LocationVerifyActivity.class);
        intent.putExtra("IS_INITIAL_VERIFY", true);
        intent.putExtra("uid", managerUid);
        startActivityForResult(intent, REQUEST_CODE_SUPERVISOR_VERIFY);
    }

    @Override
    public void onAction(MoveEmployeeModel employee) {
        if (employee == null || TextUtils.isEmpty(employee.id)) return;
        if (TextUtils.isEmpty(managerUid) || "0".equals(managerUid)) {
            Toast.makeText(getContext(), AppMessages.USER_SESSION_MISSING, Toast.LENGTH_LONG).show();
            return;
        }

        String projectId = userLocalStore.getLoggedInUserProjectID();
        UserProject up = userLocalStore.getLoggedInUserProject();
        String projName = up != null ? up.projectname : "";

        Intent intent = new Intent(requireActivity(), LocationVerifyActivity.class);
        intent.putExtra("empid", employee.id);
        intent.putExtra("emp_name", employee.firstName);
        intent.putExtra("photo_url", employee.photoUrl);
        intent.putExtra("uid", managerUid);
        intent.putExtra("projname", projName != null ? projName : "");
        intent.putExtra("selected_project_id", projectId != null ? projectId : "");
        intent.putExtra("FROM_MOVE_MENU", true);
        intent.putExtra("AUTO_START_VERIFY", false);
        intent.putExtra("IS_INITIAL_VERIFY", false);

        if (employee.onMove || "OUT".equalsIgnoreCase(employee.action)) {
            // Exit current movement at verified/next site
            intent.putExtra("type", "OUT");
            intent.putExtra("is_movement", true);
            intent.putExtra("AUTO_START_VERIFY", true);
            intent.putExtra("target_location_id", locationId);
            intent.putExtra("pending_eid", employee.id);
            intent.putExtra("pending_action", "OUT");
        } else {
            // Next-site MOVE (second site same day)
            intent.putExtra("type", "MOVE");
            intent.putExtra("is_movement", true);
            String sessionProjectId = new SessionPrefs(requireContext()).getProjectId();
            if (TextUtils.isEmpty(projectId) && !TextUtils.isEmpty(sessionProjectId)) {
                intent.putExtra("selected_project_id", sessionProjectId);
            }
            String sessionProjectName = new SessionPrefs(requireContext()).getProjectName();
            if (TextUtils.isEmpty(projName) && !TextUtils.isEmpty(sessionProjectName)) {
                intent.putExtra("projname", sessionProjectName);
            }
        }
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SUPERVISOR_VERIFY) {
            SessionPrefs session = new SessionPrefs(requireContext());
            if (session.isLocationSessionValid()) {
                locationId = session.getLocationId();
                bindSessionHeader();
                loadDepartments();
            } else {
                Toast.makeText(getContext(), AppMessages.SUPERVISOR_LOCATION_REQUIRED, Toast.LENGTH_LONG).show();
            }
        }
    }
}
