package com.app.fourscontracting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class ProjectActivity extends AppActivity {
    UserLocalStore userLocalStore;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);


        //Toast.makeText(ProjectActivity.this, val_list[3], Toast.LENGTH_SHORT).show();

        String[] project_list = val_list.length > 3 ? val_list[3].split(",") : new String[]{""};

        spinner = findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProjectActivity.this, android.R.layout.simple_spinner_item, project_list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = parent.getItemAtPosition(position).toString();
                Toast.makeText(ProjectActivity.this, value, Toast.LENGTH_SHORT).show();
                Intent homepage = new Intent(ProjectActivity.this, WebviewActivity.class);
                homepage.putExtra("key", val);
                homepage.putExtra("projectname", value);
                startActivity(homepage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        /*LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < project_list.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            //for (int j = 0; j < 1; j++) {
                Button btnTag = new Button(this);
                btnTag.setLayoutParams(new LinearLayout.LayoutParams
                        (LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT));
                btnTag.setText(project_list[i]);
                btnTag.setId((i + 1));
                //btnTag.setBackgroundColor(getResources().getColor(R.color.colorRed));
                row.addView(btnTag);
            //}
            layout.addView(row);
        }
        setContentView(layout);*/
    }
}