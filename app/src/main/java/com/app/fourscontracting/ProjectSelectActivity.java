package com.app.fourscontracting;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectSelectActivity extends AppActivity {
    private final List<ProjectModel> projects = new ArrayList<>();
    private ProjectAdapter adapter;
    private String currentUid = "";
    private ProjectModel pendingProject;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_select);

        // Location permission is requested once on Splash — do not re-prompt here.

        RecyclerView recyclerView = findViewById(R.id.projectRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectAdapter();
        recyclerView.setAdapter(adapter);

        UserLocalStore localStore = new UserLocalStore(this);
        User user = localStore.getLoggedInUser();
        String val = user != null ? user.username : "";
        String[] valList = UserLocalStore.parseUserInfo(val);
        currentUid = valList.length > 0 ? valList[0] : "";

        if (currentUid.isEmpty()) {
            Toast.makeText(this, "User session is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        new ProjectApiHelper().fetchProjects(this, currentUid, new ProjectApiHelper.Callback() {
            @Override
            public void onSuccess(List<ProjectModel> loadedProjects, String username) {
                projects.clear();
                projects.addAll(loadedProjects);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProjectSelectActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyProjectSelection(final ProjectModel project) {
        if (!AppPermissions.hasLocation(this)) {
            pendingProject = project;
            AppPermissions.ensureOrGuide(this, false, true);
            return;
        }

        new LocationHelper().getCurrentLocation(this, 10000, new LocationHelper.LocationCallback() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) {
                    Toast.makeText(ProjectSelectActivity.this, "GPS timeout, please retry", Toast.LENGTH_SHORT).show();
                    return;
                }

                verifyLocationWithServer(project, location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProjectSelectActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted, verifying location...", Toast.LENGTH_SHORT).show();
                if (pendingProject != null) {
                    verifyProjectSelection(pendingProject);
                    pendingProject = null;
                }
            } else {
                Toast.makeText(this, "Location permission is required for verification.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void verifyLocationWithServer(final ProjectModel project, final double lat, final double lng) {
        String url = "https://4scontracting.com/SMCS_APP/subcontractor/verify_location.php";
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String status = json.optString("status", "");
                        if ("success".equalsIgnoreCase(status)) {
                            SharedPreferences prefs = getSharedPreferences("AppPrefs", Activity.MODE_PRIVATE);
                            prefs.edit()
                                    .putString("last_verified_location_id", json.optString("location_id", ""))
                                    .putLong("last_verification_timestamp", System.currentTimeMillis())
                                    .apply();

                            UserLocalStore store = new UserLocalStore(this);
                            store.storeUserProjectData(new UserProject(project.projname));
                            store.storeUserProjectID(project.id);

                            Intent intent = new Intent(ProjectSelectActivity.this, DashboardActivity.class);
                            intent.putExtra("destination_id", R.id.nav_profile);
                            intent.putExtra("project_id", project.id);
                            intent.putExtra("project_name", project.projname);
                            intent.putExtra("uid", currentUid);
                            intent.putExtra("location_id", json.optString("location_id", ""));
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ProjectSelectActivity.this, json.optString("message", "Verification failed"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ProjectSelectActivity.this, "Unable to verify location", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(ProjectSelectActivity.this, "Verification request failed", Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("uid", currentUid);
                params.put("projid", project.id);
                params.put("lat", String.valueOf(lat));
                params.put("lng", String.valueOf(lng));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private class ProjectAdapter extends RecyclerView.Adapter<ProjectViewHolder> {
        @NonNull
        @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ProjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            ProjectModel project = projects.get(position);
            holder.title.setText(project.projname);
            holder.subtitle.setText(project.id);
            holder.itemView.setOnClickListener(v -> verifyProjectSelection(project));
        }

        @Override
        public int getItemCount() {
            return projects.size();
        }
    }

    private static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
