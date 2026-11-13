package com.app.fourscontracting.domain;

import android.location.Location;
import android.os.Build;

public class LocationVerifier {
    public static boolean isMockLocation(Location location) {
        if (location == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return location.isMock();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return location.isFromMockProvider();
        }
        return false;
    }

    public static float distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        try {
            float[] results = new float[1];
            Location.distanceBetween(lat1, lng1, lat2, lng2, results);
            return results[0];
        } catch (Throwable t) {
            // Pure Java Haversine fallback (e.g. JVM unit tests where android.jar is not mocked)
            return (float) computeHaversineMeters(lat1, lng1, lat2, lng2);
        }
    }

    public static double computeHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    /**
     * Checks if a GPS coordinate is inside the bounding box defined by [lat1, lng1] and [lat2, lng2].
     * Includes a GPS accuracy tolerance buffer in meters so slight satellite drift does not falsely fail.
     */
    public static boolean isWithinBoundingBox(double userLat, double userLng,
                                             double lat1, double lng1,
                                             double lat2, double lng2,
                                             float accuracyMeters) {
        double minLat = Math.min(lat1, lat2);
        double maxLat = Math.max(lat1, lat2);
        double minLng = Math.min(lng1, lng2);
        double maxLng = Math.max(lng1, lng2);

        // Immediate check: strictly inside coordinates
        if (userLat >= minLat && userLat <= maxLat && userLng >= minLng && userLng <= maxLng) {
            return true;
        }

        // Add GPS accuracy buffer (at least 25 meters, up to 100 meters)
        float bufferMeters = Math.max(25.0f, Math.min(accuracyMeters * 0.75f, 100.0f));
        double latBuffer = bufferMeters / 111139.0;
        double cosLat = Math.cos(Math.toRadians(userLat));
        double lngBuffer = bufferMeters / (111139.0 * Math.max(0.1, cosLat));

        return (userLat >= (minLat - latBuffer) && userLat <= (maxLat + latBuffer)
                && userLng >= (minLng - lngBuffer) && userLng <= (maxLng + lngBuffer));
    }

    /**
     * Calculates the shortest distance in meters from a user GPS point to the bounding box.
     * Returns 0.0f if the user point is inside the bounding box.
     */
    public static float distanceToBoundingBox(double userLat, double userLng,
                                             double lat1, double lng1,
                                             double lat2, double lng2) {
        double minLat = Math.min(lat1, lat2);
        double maxLat = Math.max(lat1, lat2);
        double minLng = Math.min(lng1, lng2);
        double maxLng = Math.max(lng1, lng2);

        if (userLat >= minLat && userLat <= maxLat && userLng >= minLng && userLng <= maxLng) {
            return 0.0f;
        }

        double clampedLat = Math.max(minLat, Math.min(userLat, maxLat));
        double clampedLng = Math.max(minLng, Math.min(userLng, maxLng));

        return distanceBetween(userLat, userLng, clampedLat, clampedLng);
    }
}

