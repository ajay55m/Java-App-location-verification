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

    public static final int REQUEST_CHECK_LOCATION_SETTINGS = 9102;

    public interface LocationSettingsCallback {
        void onLocationSettingsEnabled();
        void onLocationSettingsFailed(String reason);
    }

    public static boolean isGpsEnabled(android.content.Context context) {
        if (context == null) return false;
        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    public static void checkAndPromptLocationSettings(Activity activity, LocationSettingsCallback callback) {
        if (activity == null) return;

        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();

        com.google.android.gms.location.LocationSettingsRequest.Builder builder = new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        com.google.android.gms.location.SettingsClient client = LocationServices.getSettingsClient(activity);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(activity, locationSettingsResponse -> {
                    if (callback != null) {
                        callback.onLocationSettingsEnabled();
                    }
                })
                .addOnFailureListener(activity, e -> {
                    if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                        try {
                            com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) e;
                            resolvable.startResolutionForResult(activity, REQUEST_CHECK_LOCATION_SETTINGS);
                        } catch (android.content.IntentSender.SendIntentException sendEx) {
                            if (callback != null) {
                                callback.onLocationSettingsFailed("Unable to prompt location settings");
                            }
                        }
                    } else {
                        if (callback != null) {
                            callback.onLocationSettingsFailed("Location settings not resolvable");
                        }
                    }
                });
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
