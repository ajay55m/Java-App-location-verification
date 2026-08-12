package com.app.fourscontracting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
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
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

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

public class WebviewActivity extends AppActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ValueAnimator glowAnimator;
    UserLocalStore userLocalStore;
    Spinner spinner;
    private LinearLayout noInternetBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // Camera/Location asked once on Splash — do not re-prompt here.

        boolean debuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(debuggable);
        }

        userLocalStore = new UserLocalStore(this);

        // Flush offline attendance punches when shell opens
        try {
            new com.app.fourscontracting.data.AttendanceRepository(this).flushQueue(null);
        } catch (Exception ignored) {
        }

        final MaterialCardView headerCard = findViewById(R.id.header_card);
        noInternetBar = findViewById(R.id.noInternetBar);
        Button btnRetry = findViewById(R.id.btnRetry);

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
                        if (isNetworkAvailable()) {
                            stopPulseGlow(headerCard);
                            hideNoInternetBar();
                            try {
                                new com.app.fourscontracting.data.AttendanceRepository(WebviewActivity.this).flushQueue(null);
                            } catch (Exception ignored) {
                            }
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
                        if (!isNetworkAvailable()) {
                            startPulseGlow(headerCard);
                            showNoInternetBar();
                            Toast.makeText(WebviewActivity.this, AppMessages.NO_INTERNET, Toast.LENGTH_SHORT).show();
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
                        stopPulseGlow(headerCard);
                        hideNoInternetBar();
                        reloadActiveWebView();
                    } else {
                        Toast.makeText(WebviewActivity.this, AppMessages.NO_INTERNET, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Initial Check when app starts
        if (!isNetworkAvailable()) {
            startPulseGlow(headerCard);
            showNoInternetBar();
        } else {
            stopPulseGlow(headerCard);
            hideNoInternetBar();
        }
        setupProjectSpinner();
        fetchAssignedProjectsFromServer();

        User user = userLocalStore.getLoggedInUser();
        final String val = user.username != null ? user.username : "";
        String[] val_list = UserLocalStore.parseUserInfo(val);

        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_calendar, R.id.nav_profile, R.id.nav_move_site, R.id.nav_manage_attendance)
                .setOpenableLayout(drawer)
                .build();
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment != null ? navHostFragment.getNavController() : Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        final ViewFlipper titleFlipper = findViewById(R.id.title_flipper);
        final TextView toolbarTitle = findViewById(R.id.toolbar_title);
        final View greetingContainer = findViewById(R.id.greetingContainer);
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
            // Clear footer highlight when on sidebar-only Move Site or Manage Attendance
            if ((destination.getId() == R.id.nav_move_site || destination.getId() == R.id.nav_manage_attendance) && bottom_nav_view != null) {
                bottom_nav_view.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < bottom_nav_view.getMenu().size(); i++) {
                    bottom_nav_view.getMenu().getItem(i).setChecked(false);
                }
                bottom_nav_view.getMenu().setGroupCheckable(0, true, true);
            }

            if (destination.getId() == R.id.nav_home) {
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
                    if (destination.getId() == R.id.nav_profile) {
                        toolbarTitle.setText("Labour Management");
                    } else if (destination.getId() == R.id.nav_move_site) {
                        toolbarTitle.setText("Move Site");
                    } else if (destination.getId() == R.id.nav_manage_attendance) {
                        toolbarTitle.setText("Manage Attendance");
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

        // Update Sidebar (keep XML gradient + splash logo card; do not overwrite background)
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
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            java.util.List<androidx.fragment.app.Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
            for (androidx.fragment.app.Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible()) {
                    if (fragment instanceof SettingFragment) {
                        // Native SettingFragment (no webview history)
                    } else if (fragment instanceof com.app.fourscontracting.ui.home.HomeFragment) {
                        com.app.fourscontracting.ui.home.HomeFragment homeFragment = (com.app.fourscontracting.ui.home.HomeFragment) fragment;
                        if (homeFragment.mWebView != null && homeFragment.mWebView.canGoBack()) {
                            homeFragment.mWebView.goBack();
                            return;
                        }
                    } else if (fragment instanceof com.app.fourscontracting.ui.slideshow.SlideshowFragment) {
                        com.app.fourscontracting.ui.slideshow.SlideshowFragment slideshowFragment = (com.app.fourscontracting.ui.slideshow.SlideshowFragment) fragment;
                        if (slideshowFragment.mWebView != null && slideshowFragment.mWebView.canGoBack()) {
                            slideshowFragment.mWebView.goBack();
                            return;
                        }
                    }
                }
            }
        }
        super.onBackPressed();
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("destination_id")) {
                int destId = intent.getIntExtra("destination_id", R.id.nav_home);
                BottomNavigationView bottom_nav_view = findViewById(R.id.bottom_nav_view);
                if (bottom_nav_view != null) {
                    bottom_nav_view.setSelectedItemId(destId);
                }
            }
            if (intent.getBooleanExtra("attendance_submitted", false)) {
                intent.putExtra("attendance_submitted", false);
                reloadActiveWebView();
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

    // Scroll collapse flipper controller
    public void setHeaderCollapsed(boolean collapsed) {
        ViewFlipper titleFlipper = findViewById(R.id.title_flipper);
        if (titleFlipper != null) {
            int targetChild = collapsed ? 1 : 0;
            if (titleFlipper.getDisplayedChild() != targetChild) {
                titleFlipper.setDisplayedChild(targetChild);
            }
        }
    }

    // Floating header card glow animations
    private void startPulseGlow(final MaterialCardView card) {
        if (card == null) return;
        if (glowAnimator != null) {
            glowAnimator.cancel();
        }
        card.setStrokeWidth((int) (2.5f * getResources().getDisplayMetrics().density));
        glowAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), 
                Color.parseColor("#E2E8F0"), Color.parseColor("#F59E0B"));
        glowAnimator.setDuration(1200);
        glowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                card.setStrokeColor((int) animation.getAnimatedValue());
            }
        });
        glowAnimator.start();
    }

    private void stopPulseGlow(final MaterialCardView card) {
        if (glowAnimator != null) {
            glowAnimator.cancel();
            glowAnimator = null;
        }
        if (card != null) {
            card.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));
            card.setStrokeColor(Color.parseColor("#0EA5E9")); // Brand Soft Blue Glow
        }
    }

    private void showNoInternetBar() {
        if (noInternetBar == null) return;
        if (noInternetBar.getVisibility() == View.VISIBLE) return;
        
        noInternetBar.setVisibility(View.VISIBLE);
        noInternetBar.post(new Runnable() {
            @Override
            public void run() {
                float height = noInternetBar.getHeight();
                if (height == 0) height = 150f;
                noInternetBar.setTranslationY(-height);
                noInternetBar.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(300)
                        .start();
            }
        });
    }

    private void hideNoInternetBar() {
        if (noInternetBar == null) return;
        if (noInternetBar.getVisibility() == View.GONE) return;
        
        float height = noInternetBar.getHeight();
        if (height == 0) height = 150f;
        
        noInternetBar.animate()
                .translationY(-height)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        noInternetBar.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void reloadActiveWebView() {
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
                        com.app.fourscontracting.ui.home.HomeFragment f = (com.app.fourscontracting.ui.home.HomeFragment) fragment;
                        if (f.mWebView != null) f.mWebView.reload();
                    } else if (fragment instanceof com.app.fourscontracting.ui.slideshow.SlideshowFragment) {
                        com.app.fourscontracting.ui.slideshow.SlideshowFragment f = (com.app.fourscontracting.ui.slideshow.SlideshowFragment) fragment;
                        if (f.mWebView != null) f.mWebView.reload();
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

        if (val_list.length >= 4) {
            String[] project_list = val_list[3].split(",");
            spinner = findViewById(R.id.spinner);
            if (spinner != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(WebviewActivity.this, android.R.layout.simple_spinner_item, project_list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (view != null) {
                            ((TextView) view).setTextColor(Color.WHITE);
                        }
                        String value = parent.getItemAtPosition(position).toString();
                        Toast.makeText(WebviewActivity.this, value, Toast.LENGTH_SHORT).show();
                        UserProject user = new UserProject(value);
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("projectname", value);
                        userLocalStore.storeUserProjectData(user);
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

        JsonObjectRequest getProjectsRequest = new JsonObjectRequest(Request.Method.POST, getProjectsUrl, null,
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
                        
                        // Update both database and shared pref cache
                        userLocalStore.updateAssignedProjects(newProjectsList, WebviewActivity.this);
                        
                        // Refresh spinner with new data
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