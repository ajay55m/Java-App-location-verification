package com.app.fourscontracting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ProjectActivity extends AppActivity {
    UserLocalStore userLocalStore;
    Spinner spinner;
    View layoutProjectAllocation;
    TextView tvNoAllocation;
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        layoutProjectAllocation = findViewById(R.id.layoutProjectAllocation);
        tvNoAllocation = findViewById(R.id.tvNoAllocation);
        btnNext = findViewById(R.id.btnNext);
        spinner = findViewById(R.id.spinnerProject);
        if (spinner == null) {
            spinner = findViewById(R.id.spinner);
        }

        // STEP 4: Hide logic - check allocation flag
        String hasAllocation = getSharedPreferences("app", MODE_PRIVATE)
                .getString("has_project_allocation", "0");

        if ("0".equals(hasAllocation)) {
            // HIDE PROJECT LOCATION ALLOCATION
            if (layoutProjectAllocation != null) layoutProjectAllocation.setVisibility(View.GONE);
            if (tvNoAllocation != null) {
                tvNoAllocation.setVisibility(View.VISIBLE);
                tvNoAllocation.setText("No Project Allocated For You.\nPlease Contact Admin.");
            }
            return; // CRITICAL: Don't call get_locations.php, don't setup spinner
        }

        userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);

        String rawProjects = val_list.length > 3 ? val_list[3] : "";
        String[] project_list = rawProjects.split(",");
        List<String> validProjects = new ArrayList<>();
        for (String p : project_list) {
            if (p != null && !p.trim().isEmpty()) {
                validProjects.add(p.trim());
            }
        }

        // If list is empty, hide automatically and show contact admin message
        if (validProjects.isEmpty()) {
            if (layoutProjectAllocation != null) layoutProjectAllocation.setVisibility(View.GONE);
            if (tvNoAllocation != null) {
                tvNoAllocation.setVisibility(View.VISIBLE);
                tvNoAllocation.setText("No Project Allocated For You.\nPlease Contact Admin.");
            }
            return; // Prevent NullPointer / IndexOutOfBounds crash
        }

        final String[] projectArr = validProjects.toArray(new String[0]);
        if (spinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ProjectActivity.this, android.R.layout.simple_spinner_item, projectArr);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String value = parent.getItemAtPosition(position).toString();
                    userLocalStore.storeUserProjectData(new UserProject(value));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (spinner != null && spinner.getSelectedItem() != null) {
                    String selectedProject = spinner.getSelectedItem().toString();
                    Intent verifyIntent = new Intent(ProjectActivity.this, LocationVerifyActivity.class);
                    verifyIntent.putExtra("project_code", selectedProject);
                    verifyIntent.putExtra("projname", selectedProject);
                    startActivity(verifyIntent);
                } else {
                    Toast.makeText(ProjectActivity.this, "Please select a project", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
