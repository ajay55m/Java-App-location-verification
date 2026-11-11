package com.app.fourscontracting.presentation.project;

import android.content.Context;
import android.widget.ArrayAdapter;
import com.app.fourscontracting.data.model.project.ProjectModel;
import java.util.List;

public class ProjectAdapter extends ArrayAdapter<ProjectModel> {
    public ProjectAdapter(Context context, int resource, List<ProjectModel> objects) {
        super(context, resource, objects);
    }
}
