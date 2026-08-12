package com.app.fourscontracting;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.os.Handler;
import android.os.Looper;

public class LoadingManager {
    private Dialog dialog;
    private ObjectAnimator craneAnimator;
    private View associatedWebView;
    private final Handler failsafeHandler = new Handler(Looper.getMainLooper());
    private final Runnable failsafeRunnable = new Runnable() {
        @Override
        public void run() {
            hideLoading();
            if (associatedWebView != null) {
                associatedWebView.setVisibility(View.VISIBLE);
            }
        }
    };

    public void showLoading(Context context) {
        showLoading(context, null);
    }

    public void showLoading(Context context, final View webView) {
        if (dialog != null && dialog.isShowing()) return;

        this.associatedWebView = webView;

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.layout_loading_bricks);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false); // User must wait

        View load = dialog.findViewById(R.id.crane_load);
        if (load != null) {
            startCraneAnimation(load);
        }

        dialog.show();

        // FAILSAFE: If the page doesn't load in 5 seconds, hide it anyway
        failsafeHandler.removeCallbacks(failsafeRunnable);
        failsafeHandler.postDelayed(failsafeRunnable, 5000); // 5000ms = 5 seconds
    }

    private void startCraneAnimation(View load) {
        if (craneAnimator != null) {
            craneAnimator.cancel();
        }
        craneAnimator = ObjectAnimator.ofFloat(load, "translationY", 0f, 120f);
        craneAnimator.setDuration(1000);
        craneAnimator.setRepeatMode(ValueAnimator.REVERSE);
        craneAnimator.setRepeatCount(ValueAnimator.INFINITE);
        craneAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        craneAnimator.start();
    }

    public void hideLoading() {
        try {
            failsafeHandler.removeCallbacks(failsafeRunnable);
            if (dialog != null && dialog.isShowing()) {
                View cardView = dialog.findViewById(android.R.id.content);
                if (cardView != null) {
                    cardView.animate()
                        .alpha(0f)
                        .setDuration(200) // 0.2 seconds fade
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (dialog != null) {
                                        dialog.dismiss();
                                        dialog = null;
                                    }
                                } catch (Exception e) {
                                    // Safe dismiss
                                }
                            }
                        }).start();
                } else {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        } catch (Exception e) {
            // Prevent crashes during context changes
            dialog = null;
        }
        if (craneAnimator != null) {
            craneAnimator.cancel();
            craneAnimator = null;
        }
    }
}
