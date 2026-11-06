package com.app.fourscontracting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProjectActivity extends AppActivity {
    private Spinner spinner;
    private final List<ProjectModel> projectList = new ArrayList<>();
    private boolean isInitial = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        spinner = findViewById(R.id.spinner);

        loadProjects();
    }

    private void loadProjects() {
        ApiClient.getService().getProjects().enqueue(new Callback<List<ProjectModel>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProjectModel>> call, @NonNull Response<List<ProjectModel>> response) {
                if (response.body() != null && !response.body().isEmpty()) {
                    projectList.clear();
                    projectList.addAll(response.body());

                    ArrayAdapter<ProjectModel> adapter = new ArrayAdapter<>(
                            ProjectActivity.this,
                            android.R.layout.simple_spinner_item,
                            projectList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (isInitial) {
                                isInitial = false;
                                return;
                            }
                            ProjectModel selected = projectList.get(position);
                            String code = selected.getProjectCode();
                            Toast.makeText(ProjectActivity.this, "Selected: " + code, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(ProjectActivity.this, LocationVerifyActivity.class);
                            intent.putExtra("project_code", code);
                            intent.putExtra("location_id", selected.id);
                            startActivity(intent);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                } else {
                    Toast.makeText(ProjectActivity.this, "No projects returned from server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProjectModel>> call, @NonNull Throwable t) {
                Toast.makeText(ProjectActivity.this, "Failed to load projects: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
