package com.app.fourscontracting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.widget.ViewFlipper;
import com.google.android.material.card.MaterialCardView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 100% Native Java Main Application Dashboard Activity.
 * Replaces legacy WebviewActivity with native fragment container & Volley REST API integration.
 */
public class DashboardActivity extends AppActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ValueAnimator glowAnimator;
    UserLocalStore userLocalStore;
    Spinner spinner;
    private LinearLayout noInternetBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private android.content.BroadcastReceiver connectivityReceiver;
    private MaterialCardView networkAlertCard;
    private ImageView ivNetworkAlertIcon;
    private TextView tvNetworkAlertMessage;
    private TextView tvNetworkAlertClose;
    private android.os.Handler networkAlertHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable hideNetworkAlertRunnable;
    private boolean wasOffline = false;

    private void updateConnectivityUI() {
        boolean available = isNetworkAvailable();
        if (!available) {
            wasOffline = true;
            showNetworkAlert(false);
        } else {
            if (wasOffline) {
                wasOffline = false;
                showNetworkAlert(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        userLocalStore = new UserLocalStore(this);

        // Flush offline attendance punches when shell opens
        try {
            new com.app.fourscontracting.data.AttendanceRepository(this).flushQueue(null);
        } catch (Exception ignored) {
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Define the callback that listens for network changes
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Back online - Hide the pulse alert
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateConnectivityUI();
                        try {
                            new com.app.fourscontracting.data.AttendanceRepository(DashboardActivity.this).flushQueue(null);
                        } catch (Exception ignored) {
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // Connection lost - Trigger pulsing alert
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateConnectivityUI();
                            }
                        }, 100);
                    }
                });
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateConnectivityUI();
                    }
                });
            }
        };

        setupNetworkAlertView();
        checkInitialNetworkState();
        setupProjectSpinner();
        fetchAssignedProjectsFromServer();

        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_profile, R.id.nav_supervisor_attendance, R.id.nav_manage_attendance, R.id.nav_move_site)
                .setOpenableLayout(drawer)
                .build();
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment != null ? navHostFragment.getNavController() : Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        final ViewFlipper titleFlipper = findViewById(R.id.title_flipper);
        final TextView toolbarTitle = findViewById(R.id.toolbar_title);
        final TextView headerGreeting = findViewById(R.id.headerGreeting);
        final TextView headerUserName = findViewById(R.id.headerUserName);

        final String rawName = (val_list.length > 2 && !val_list[2].isEmpty()) ? val_list[2] : "User";
        String capitalized = rawName;
        if (capitalized != null && !capitalized.isEmpty()) {
            capitalized = capitalized.substring(0, 1).toUpperCase() + capitalized.substring(1);
        }
        final String displayName = capitalized;
        final String displayUid = val_list.length > 0 ? val_list[0] : "";
        final String displayIdText = "Emp ID: #" + displayUid;

        final BottomNavigationView bottom_nav_view = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottom_nav_view, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (bottom_nav_view != null) {
                bottom_nav_view.getMenu().setGroupCheckable(0, true, true);
            }

            if (destination.getId() == R.id.nav_profile) {
                if (titleFlipper != null) {
                    titleFlipper.setDisplayedChild(0); // Show greeting
                }
                if (headerGreeting != null) {
                    headerGreeting.setText(getGreetingWish() + ",");
                }
                if (headerUserName != null) {
                    headerUserName.setText(displayName + " 👋");
                }
            } else {
                if (titleFlipper != null) {
                    titleFlipper.setDisplayedChild(1); // Show title
                }
                if (toolbarTitle != null) {
                    if (destination.getId() == R.id.nav_move_site) {
                        toolbarTitle.setText("Move Site");
                    } else if (destination.getId() == R.id.nav_manage_attendance) {
                        toolbarTitle.setText("Manage Attendance");
                    } else if (destination.getId() == R.id.nav_supervisor_attendance) {
                        toolbarTitle.setText("Self Attendance");
                    } else {
                        toolbarTitle.setText(destination.getLabel());
                    }
                }
            }
        });

        handleIntent(getIntent());

        // Apply faint 3% engineering blueprint grid pattern globally
        View contentMainRoot = findViewById(R.id.content_main_root);
        if (contentMainRoot != null) {
            contentMainRoot.setBackground(new BlueprintGridDrawable(8));
        }

        // Update Sidebar
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView navName = headerView.findViewById(R.id.sidebar_user_name);
            TextView navId = headerView.findViewById(R.id.sidebar_user_id);
            if (navName != null) navName.setText(displayName);
            if (navId != null) navId.setText(displayIdText);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onSupportNavigateUp() {
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment != null ? navHostFragment.getNavController() : Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("destination_id")) {
                int destId = intent.getIntExtra("destination_id", R.id.nav_profile);
                BottomNavigationView bottom_nav_view = findViewById(R.id.bottom_nav_view);
                if (bottom_nav_view != null) {
                    bottom_nav_view.setSelectedItemId(destId);
                }
            }
            if (intent.getBooleanExtra("attendance_submitted", false)) {
                intent.putExtra("attendance_submitted", false);
                refreshActiveFragment();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (connectivityManager != null && networkCallback != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback);
                } else {
                    NetworkRequest networkRequest = new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build();
                    connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (connectivityReceiver == null) {
            connectivityReceiver = new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateConnectivityUI();
                        }
                    });
                }
            };
            try {
                registerReceiver(connectivityReceiver, new android.content.IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            } catch (Exception ignored) {}
        }

        updateConnectivityUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
        if (connectivityReceiver != null) {
            try {
                unregisterReceiver(connectivityReceiver);
            } catch (Exception ignored) {}
            connectivityReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hideNetworkAlertRunnable != null) {
            networkAlertHandler.removeCallbacks(hideNetworkAlertRunnable);
        }
    }

    private void checkInitialNetworkState() {
        boolean isOnline = isNetworkAvailable();
        if (!isOnline) {
            wasOffline = true;
            showNetworkAlert(false);
        } else {
            wasOffline = false;
        }
    }

    private void setupNetworkAlertView() {
        networkAlertCard = findViewById(R.id.networkAlertCard);
        ivNetworkAlertIcon = findViewById(R.id.ivNetworkAlertIcon);
        tvNetworkAlertMessage = findViewById(R.id.tvNetworkAlertMessage);
        tvNetworkAlertClose = findViewById(R.id.tvNetworkAlertClose);

        if (tvNetworkAlertClose != null) {
            tvNetworkAlertClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNetworkAvailable()) {
                        wasOffline = false;
                        showNetworkAlert(true);
                        try {
                            new com.app.fourscontracting.data.AttendanceRepository(DashboardActivity.this).flushQueue(null);
                        } catch (Exception ignored) {}
                    } else {
                        retriggerNetworkAlertIn();
                    }
                }
            });
        }
    }

    private void retriggerNetworkAlertIn() {
        if (networkAlertCard == null) return;
        final float screenWidth = getResources().getDisplayMetrics().widthPixels;
        networkAlertCard.animate()
                .translationX(-screenWidth)
                .setDuration(150)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (networkAlertCard != null) {
                            networkAlertCard.setTranslationX(-screenWidth);
                            networkAlertCard.animate()
                                    .translationX(0f)
                                    .alpha(1f)
                                    .setDuration(320)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                                    .start();
                        }
                    }
                })
                .start();
    }

    private void showNetworkAlert(boolean isOnline) {
        if (networkAlertCard == null) {
            setupNetworkAlertView();
        }
        if (networkAlertCard == null) return;

        if (hideNetworkAlertRunnable != null) {
            networkAlertHandler.removeCallbacks(hideNetworkAlertRunnable);
        }

        if (isOnline) {
            // --- ONLINE STATE: "We are back..." (Dark Emerald Green #0B381D) ---
            networkAlertCard.setCardBackgroundColor(Color.parseColor("#0B381D"));
            if (ivNetworkAlertIcon != null) {
                ivNetworkAlertIcon.setImageResource(R.drawable.ic_bhim_online_check);
            }
            if (tvNetworkAlertMessage != null) {
                tvNetworkAlertMessage.setText("We are back...");
            }

            animateNetworkAlertIn();

            hideNetworkAlertRunnable = new Runnable() {
                @Override
                public void run() {
                    animateNetworkAlertOut();
                }
            };
            networkAlertHandler.postDelayed(hideNetworkAlertRunnable, 3000);
        } else {
            // --- OFFLINE STATE: "You're offline right now" (Dark Crimson Red #450A0A) ---
            networkAlertCard.setCardBackgroundColor(Color.parseColor("#450A0A"));
            if (ivNetworkAlertIcon != null) {
                ivNetworkAlertIcon.setImageResource(R.drawable.ic_bhim_offline_alert);
            }
            if (tvNetworkAlertMessage != null) {
                tvNetworkAlertMessage.setText("You're offline right now");
            }

            animateNetworkAlertIn();
        }
    }

    private void animateNetworkAlertIn() {
        if (networkAlertCard == null) return;
        if (networkAlertCard.getVisibility() != View.VISIBLE) {
            networkAlertCard.setVisibility(View.VISIBLE);
            networkAlertCard.setAlpha(0f);

            // Sudden Left-to-Right entrance animation with Overshoot
            float screenWidth = getResources().getDisplayMetrics().widthPixels;
            networkAlertCard.setTranslationX(-screenWidth);
            networkAlertCard.setTranslationY(0f);

            networkAlertCard.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                    .start();
        }
    }

    private void animateNetworkAlertOut() {
        if (networkAlertCard == null) return;
        if (networkAlertCard.getVisibility() == View.VISIBLE) {
            float screenWidth = getResources().getDisplayMetrics().widthPixels;

            networkAlertCard.animate()
                    .alpha(0f)
                    .translationX(screenWidth)
                    .setDuration(280)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (networkAlertCard != null) {
                                networkAlertCard.setVisibility(View.GONE);
                                networkAlertCard.setTranslationX(0f);
                            }
                        }
                    })
                    .start();
        }
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network activeNet = connectivityManager.getActiveNetwork();
                if (activeNet == null) return false;
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNet);
                return capabilities != null &&
                        (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String getGreetingWish() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good Morning";
        } else if (hour < 16) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }



    private void refreshActiveFragment() {
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            java.util.List<androidx.fragment.app.Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
            for (androidx.fragment.app.Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    if (fragment instanceof SettingFragment) {
                        ((SettingFragment) fragment).refreshList();
                    } else if (fragment instanceof com.app.fourscontracting.ui.move.MoveSiteFragment) {
                        ((com.app.fourscontracting.ui.move.MoveSiteFragment) fragment).refreshList();
                    } else if (fragment instanceof com.app.fourscontracting.ui.attendance.ManageAttendanceFragment) {
                        ((com.app.fourscontracting.ui.attendance.ManageAttendanceFragment) fragment).refreshList();
                    } else if (fragment instanceof com.app.fourscontracting.ui.home.HomeFragment) {
                        ((com.app.fourscontracting.ui.home.HomeFragment) fragment).refreshData();
                    } else if (fragment instanceof com.app.fourscontracting.ui.slideshow.SlideshowFragment) {
                        ((com.app.fourscontracting.ui.slideshow.SlideshowFragment) fragment).refreshData();
                    } else if (fragment instanceof com.app.fourscontracting.ui.supervisor.SupervisorAttendanceFragment) {
                        ((com.app.fourscontracting.ui.supervisor.SupervisorAttendanceFragment) fragment).refreshData();
                    }
                }
            }
        }
    }

    public void updateToolbarTitle(String newTitle) {
        final TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(newTitle);
            toolbarTitle.setAlpha(0f);
            toolbarTitle.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void setupProjectSpinner() {
        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);

        if (val_list.length >= 4 && val_list[3] != null && !val_list[3].isEmpty()) {
            String[] project_list = val_list[3].split(",");
            spinner = findViewById(R.id.spinner);
            if (spinner != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(DashboardActivity.this, android.R.layout.simple_spinner_item, project_list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (view != null && view instanceof TextView) {
                            ((TextView) view).setTextColor(Color.WHITE);
                        }
                        if (parent != null && parent.getItemAtPosition(position) != null) {
                            String value = parent.getItemAtPosition(position).toString();
                            UserProject user = new UserProject(value);
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString("projectname", value);
                            userLocalStore.storeUserProjectData(user);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        }
    }

    private void fetchAssignedProjectsFromServer() {
        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);
        String uid = val_list.length > 0 ? val_list[0] : "";

        if (uid.isEmpty()) {
            return;
        }

        String getProjectsUrl = "https://4scontracting.com/SMCS_APP/fcm_app/getprojects.php?uid=" + uid;

        JsonObjectRequest getProjectsRequest = new JsonObjectRequest(Request.Method.GET, getProjectsUrl, null,
            response -> {
                try {
                    String status = response.optString("status", "");
                    if ("success".equalsIgnoreCase(status)) {
                        JSONArray projectsArray = response.getJSONArray("projects");
                        List<String> newProjectsList = new ArrayList<>();
                        for (int i = 0; i < projectsArray.length(); i++) {
                            JSONObject projObj = projectsArray.getJSONObject(i);
                            newProjectsList.add(projObj.optString("projname", ""));
                        }
                        
                        userLocalStore.updateAssignedProjects(newProjectsList, DashboardActivity.this);
                        setupProjectSpinner();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            error -> {
                error.printStackTrace();
            }
        );

        MySingleton.getmInstance(this).addToRequestque(getProjectsRequest);
    }
}
