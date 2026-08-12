package com.app.fourscontracting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Asks Camera + Location once on first app open.
 * Later screens only check grants — they do not spam system dialogs.
 */
public final class AppPermissions {
    private AppPermissions() {}

    public static final int REQUEST_CORE = 9101;
    private static final String PREFS = "AppPrefs";
    private static final String KEY_PROMPTED = "core_permissions_prompted";

    public static String[] required() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    public static boolean hasCamera(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasLocation(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasAll(Context context) {
        return hasCamera(context) && hasLocation(context);
    }

    public static boolean wasPrompted(Context context) {
        return prefs(context).getBoolean(KEY_PROMPTED, false);
    }

    public static void markPrompted(Context context) {
        prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply();
    }

    /**
     * Call from Splash / first screen only.
     * @return true if a system permission dialog was shown
     */
    public static boolean requestOnceIfNeeded(@NonNull Activity activity) {
        if (hasAll(activity)) {
            markPrompted(activity);
            return false;
        }
        if (wasPrompted(activity)) {
            return false;
        }
        markPrompted(activity);
        ActivityCompat.requestPermissions(activity, required(), REQUEST_CORE);
        return true;
    }

    /** Soft check used by attendance / verification screens — no repeated popups. */
    public static boolean ensureOrGuide(@NonNull Activity activity, boolean needCamera, boolean needLocation) {
        boolean ok = true;
        if (needCamera && !hasCamera(activity)) {
            ok = false;
        }
        if (needLocation && !hasLocation(activity)) {
            ok = false;
        }
        if (ok) {
            return true;
        }

        // First install edge case: never prompted yet → ask once
        if (!wasPrompted(activity)) {
            requestOnceIfNeeded(activity);
            return false;
        }

        Toast.makeText(activity,
                "Camera and Location were already requested. Enable them in App Settings to continue.",
                Toast.LENGTH_LONG).show();
        openAppSettings(activity);
        return false;
    }

    public static void openAppSettings(@NonNull Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
            activity.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public static void handleResult(int requestCode, @NonNull int[] grantResults) {
        // Prompt flag is already stored when the dialog is shown.
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
