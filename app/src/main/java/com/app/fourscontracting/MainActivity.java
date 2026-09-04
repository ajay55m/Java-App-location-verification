package com.app.fourscontracting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private String someVariable;

    LocationManager locationManager;
    private LinearLayout lv;
    private LinearLayout noInternetBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    UserLocalStore userLocalStore;
    String value = "";
    boolean doubleBackToExitPressedOnce = false;
    String app_server_url = "https://4scontracting.com/SMCS_APP/fcm_app/subadminlogin.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        userLocalStore = new UserLocalStore(this);
        FcmMessagingService.fetchAndSaveToken(this);

        checkAppVersion();

        noInternetBar = findViewById(R.id.noInternetBar);
        Button btnRetry = findViewById(R.id.btnRetry);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Define the callback that listens for network changes
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Back online - Hide the bar
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isNetworkAvailable()) {
                            noInternetBar.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // Connection lost - Show the bar
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isNetworkAvailable()) {
                            noInternetBar.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        };

        // Retry Button Logic
        if (btnRetry != null) {
            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNetworkAvailable()) {
                        noInternetBar.setVisibility(View.GONE);
                    }
                }
            });
        }

        // Initial Check when app starts
        if (!isNetworkAvailable()) {
            noInternetBar.setVisibility(View.VISIBLE);
        }



        final EditText mUsername = (EditText)findViewById(R.id.username);
        final EditText mPassword = (EditText)findViewById(R.id.password);

        Button mBtn = (Button)findViewById(R.id.loginbtn);
        mBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (authenticate() == false) {
                    if (!mUsername.getText().toString().isEmpty() && !mPassword.getText().toString().isEmpty()) {
                        if (haveNetwork()) {
                            final String rollNo = mUsername.getText().toString().trim();
                            final String pwd = mPassword.getText().toString().trim();

                            // Disable button while fetching token to prevent double-submit
                            mBtn.setEnabled(false);

                            try {
                                FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(task -> {
                                        mBtn.setEnabled(true);
                                        String freshToken = "";
                                        try {
                                            if (task.isSuccessful() && task.getResult() != null) {
                                                freshToken = task.getResult();
                                                SharedPreferences prefs = getApplicationContext()
                                                        .getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                                                prefs.edit().putString(getString(R.string.FCM_TOKEN), freshToken).apply();
                                            } else {
                                                SharedPreferences prefs = getApplicationContext()
                                                        .getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                                                freshToken = prefs.getString(getString(R.string.FCM_TOKEN), "");
                                            }
                                        } catch (Exception ignored) {
                                            SharedPreferences prefs = getApplicationContext()
                                                    .getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                                            freshToken = prefs.getString(getString(R.string.FCM_TOKEN), "");
                                        }

                                        performLogin(rollNo, pwd, freshToken);
                                    });
                            } catch (Exception e) {
                                mBtn.setEnabled(true);
                                SharedPreferences prefs = getApplicationContext()
                                        .getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                                String cachedToken = prefs.getString(getString(R.string.FCM_TOKEN), "");
                                performLogin(rollNo, pwd, cachedToken);
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Network connection is not available!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Please fill Username and Password", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });


        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        NavigationView navigationView = findViewById(R.id.nav_view);
//        // Passing each menu ID as a set of Ids because each
//        // menu should be considered as top level destinations.
//        mAppBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.settingFragment, R.id.imagesFragment)
//                .setDrawerLayout(drawer)
//                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
//        NavigationUI.setupWithNavController(navigationView, navController);
//
//        BottomNavigationView bottom_nav_view = findViewById(R.id.bottom_nav_view);
//        NavigationUI.setupWithNavController(bottom_nav_view, navController);
    }

    private void performLogin(final String rollNo, final String pwd, final String token) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, app_server_url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Button btn = findViewById(R.id.loginbtn);
                    if (btn != null) btn.setEnabled(true);
                    if (!UserLocalStore.isLoginResponseSuccess(response)) {
                        String errMsg = UserLocalStore.getLoginErrorMessage(response);
                        Toast.makeText(MainActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                    } else {
                        User user = new User(response);
                        someVariable = response;
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("username", response);
                        editor.apply();
                        userLocalStore.storeUserData(user);
                        userLocalStore.setUserLoggedIn(true);
                        Intent homepage = new Intent(MainActivity.this, DashboardActivity.class);
                        homepage.putExtra("key", user.username);
                        startActivity(homepage);
                        finish();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Button btn = findViewById(R.id.loginbtn);
                    if (btn != null) btn.setEnabled(true);
                    NetworkResponse response = error.networkResponse;
                    if (response != null && response.data != null) {
                        try {
                            String errorString = new String(response.data);
                            Toast.makeText(MainActivity.this, errorString, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Login error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Network error during login", Toast.LENGTH_LONG).show();
                    }
                }
            }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("fcm_token", token != null ? token : "");
                params.put("id", rollNo != null ? rollNo : "");
                params.put("pwd", pwd != null ? pwd : "");
                return params;
            }
        };
        MySingleton.getmInstance(MainActivity.this).addToRequestque(stringRequest);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(haveNetwork()) {
            if(userLocalStore.getUserLoggedIn()) {
                User user = userLocalStore.getLoggedInUser();
                String val = user.username;
                if (val != null && !val.isEmpty() && UserLocalStore.isLoginResponseSuccess(val)) {
                    String[] val_list = UserLocalStore.parseUserInfo(val);
                    if (authenticate() == true) {
                        lv = (LinearLayout) findViewById(R.id.loginView);
                        if (lv != null) lv.setVisibility(View.GONE);
                        Intent homepage = new Intent(MainActivity.this, DashboardActivity.class);
                        homepage.putExtra("key", user.username);
                        startActivity(homepage);
                        finish();
                        return;
                    } else if (val_list.length > 1 && val_list[1].equals("3")) {
                        lv = (LinearLayout) findViewById(R.id.loginView);
                        if (lv != null) lv.setVisibility(View.VISIBLE);
                    }
                } else {
                    userLocalStore.setUserLoggedIn(false);
                    userLocalStore.clearUserData();
                    lv = (LinearLayout) findViewById(R.id.loginView);
                    if (lv != null) lv.setVisibility(View.VISIBLE);
                }
            }
            else {
                lv = (LinearLayout)findViewById(R.id.loginView);
                if (lv != null) lv.setVisibility(View.VISIBLE);
            }
        } else if(!haveNetwork()) {
            User user = userLocalStore.getLoggedInUser();
            Toast.makeText(MainActivity.this, "Network connection is not available!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean authenticate() {
        return userLocalStore.getUserLoggedIn();
    }

    private boolean haveNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (connectivityManager != null && networkCallback != null) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void checkAppVersion() {
        if (!haveNetwork()) return;

        String versionUrl = "https://4scontracting.com/SMCS_APP/fcm_app/version_check.php";
        StringRequest versionRequest = new StringRequest(Request.Method.GET, versionUrl,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (response != null && !response.trim().isEmpty()) {
                        String serverVersion = response.trim();
                        String currentVersion = "1.2";
                        try {
                            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        try {
                            float serverVal = Float.parseFloat(serverVersion);
                            float currentVal = Float.parseFloat(currentVersion);
                            
                            if (serverVal > currentVal) {
                                showForceUpdateDialog(serverVersion);
                            }
                        } catch (NumberFormatException e) {
                            if (!serverVersion.equals(currentVersion)) {
                                showForceUpdateDialog(serverVersion);
                            }
                        }
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Fail silently to prevent lockout during backend maintenance
                }
            }
        );

        MySingleton.getmInstance(this).addToRequestque(versionRequest);
    }

    private void showForceUpdateDialog(String newVersion) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Update Required")
            .setMessage("A newer stable version (v" + newVersion + ") of 4S Contracting is available. Please update to continue.")
            .setPositiveButton("Update Now", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://4scontracting.com/SMCS_APP/fcm_app/4S_Contracting.apk"));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Browser Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setCancelable(false)
            .show();
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

}