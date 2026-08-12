package com.app.fourscontracting;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    public interface LocationCallback {
        void onSuccess(Location location);
        void onError(String message);
    }

    public void getCurrentLocation(final Activity activity, final long timeoutMs, final LocationCallback callback) {
        if (activity == null || callback == null) {
            return;
        }

        boolean hasFineLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasFineLocation && !hasCoarseLocation) {
            callback.onError("Location permission is required");
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(activity);
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] completed = {false};

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (completed[0]) return;
                    completed[0] = true;
                    if (location != null) {
                        callback.onSuccess(location);
                    } else {
                        callback.onError("GPS timeout, please retry");
                    }
                })
                .addOnFailureListener(e -> {
                    if (completed[0]) return;
                    completed[0] = true;
                    callback.onError("GPS unavailable, please retry");
                });

        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                callback.onError("GPS timeout, please retry");
            }
        }, timeoutMs);
    }

    public static double distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
