package com.app.fourscontracting;



import android.content.Intent;

import android.os.Bundle;

import android.os.Handler;

import android.os.Looper;

import android.view.View;

import android.view.animation.AccelerateDecelerateInterpolator;



import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;



public class SplashActivity extends AppActivity {

    private UserLocalStore userLocalStore;

    private boolean navigated;

    private final Handler handler = new Handler(Looper.getMainLooper());



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);



        RuntimeLogger.log(this, "system", "INFO", "Lifecycle", "App Started");

        userLocalStore = new UserLocalStore(this);



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F8FAFC"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            }

        }



        View logoCard = findViewById(R.id.logo_card);

        View subtitle = findViewById(R.id.logo_subtitle);



        if (logoCard != null) {

            logoCard.setAlpha(0f);

            logoCard.setScaleX(0.85f);

            logoCard.setScaleY(0.85f);

            logoCard.animate()

                    .alpha(1f)

                    .scaleX(1f)

                    .scaleY(1f)

                    .setDuration(900)

                    .setInterpolator(new AccelerateDecelerateInterpolator())

                    .start();

        }



        if (subtitle != null) {

            subtitle.setAlpha(0f);

            subtitle.animate().alpha(1f).setDuration(1200).start();

        }



        // Branding delay, then one-time Camera + Location system prompt

        handler.postDelayed(this::askPermissionsThenContinue, 1400);

    }



    private void askPermissionsThenContinue() {

        boolean showingSystemDialog = AppPermissions.requestOnceIfNeeded(this);

        if (!showingSystemDialog) {

            proceedToNextScreen();

        }

        // else wait for onRequestPermissionsResult

    }



    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,

                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AppPermissions.REQUEST_CORE) {

            proceedToNextScreen();

        }

    }



    private void proceedToNextScreen() {

        if (navigated || isFinishing()) {

            return;

        }

        navigated = true;

        handler.removeCallbacksAndMessages(null);



        Intent intent;

        if (userLocalStore.getUserLoggedIn()) {

            User user = userLocalStore.getLoggedInUser();

            String val = user.username;

            if (val != null && !val.isEmpty() && UserLocalStore.isLoginResponseSuccess(val)) {

                intent = new Intent(SplashActivity.this, WebviewActivity.class);

                intent.putExtra("key", val);

            } else {

                userLocalStore.setUserLoggedIn(false);

                userLocalStore.clearUserData();

                intent = new Intent(SplashActivity.this, MainActivity.class);

            }

        } else {

            intent = new Intent(SplashActivity.this, MainActivity.class);

        }



        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        finish();

    }



    @Override

    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();

    }

}


