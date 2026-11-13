package com.app.fourscontracting.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.app.fourscontracting.MySingleton;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Attendance network + offline queue (repository layer for MVVM).
 */
public class AttendanceRepository {

    public interface Callback {
        void onResult(AttendanceResult result);
    }

    private final Context appContext;
    private final OfflineAttendanceQueue queue;

    public AttendanceRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.queue = new OfflineAttendanceQueue(appContext);
    }

    public void submit(AttendancePayload payload, Callback callback) {
        if (payload == null) {
            callback.onResult(AttendanceResult.fail(AttendanceCodes.INVALID_PARAMS, "Invalid attendance payload"));
            return;
        }

        if (!hasGps(payload)) {
            callback.onResult(AttendanceResult.fail(AttendanceCodes.GPS_REQUIRED,
                    "GPS location is required to save attendance."));
            return;
        }

        if (!isNetworkAvailable()) {
            queue.enqueue(payload);
            callback.onResult(AttendanceResult.queued());
            return;
        }

        post(payload, result -> {
            if (!result.success
                    && !payload.isMovement
                    && !"OUT".equalsIgnoreCase(payload.type)
                    && result.message != null
                    && result.message.toLowerCase().contains("no active movement")) {
                // If site movement table has no record, retry once as standard attendance check-out
                payload.isMovement = false;
                post(payload, fallbackResult -> {
                    if (!fallbackResult.success && AttendanceCodes.NETWORK_ERROR.equals(fallbackResult.code)) {
                        queue.enqueue(payload);
                        callback.onResult(AttendanceResult.queued());
                    } else {
                        callback.onResult(fallbackResult);
                    }
                });
            } else if (!result.success && AttendanceCodes.NETWORK_ERROR.equals(result.code)) {
                queue.enqueue(payload);
                callback.onResult(AttendanceResult.queued());
            } else {
                callback.onResult(result);
            }
        });
    }

    public void flushQueue(Callback progressOrNull) {
        if (!isNetworkAvailable()) {
            return;
        }
        List<AttendancePayload> items = queue.snapshot();
        for (AttendancePayload payload : items) {
            post(payload, result -> {
                if (result.success) {
                    queue.remove(payload.clientRequestId);
                }
                if (progressOrNull != null) {
                    progressOrNull.onResult(result);
                }
            });
        }
    }

    public int pendingCount() {
        return queue.size();
    }

    private void post(AttendancePayload payload, Callback callback) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.MULTI_SAVE_TIME_IN,
                response -> callback.onResult(AttendanceResult.parse(response)),
                error -> {
                    boolean networkish = error == null || error.networkResponse == null;
                    if (networkish) {
                        callback.onResult(AttendanceResult.fail(AttendanceCodes.NETWORK_ERROR,
                                "Network error while saving attendance"));
                    } else {
                        int statusCode = error.networkResponse.statusCode;
                        String body = error.networkResponse.data != null
                                ? new String(error.networkResponse.data)
                                : "Server error";

                        // If 406 Not Acceptable (Mod_Security WAF filter triggered), attempt automatic retry with downscaled image
                        if ((statusCode == 406 || (body != null && (body.contains("406") || body.contains("Mod_Security"))))
                                && payload.imagepath != null && payload.imagepath.length() > 500 && !payload.isRetryAttempt) {
                            payload.isRetryAttempt = true;
                            payload.downscaleImageForWafBypass();
                            post(payload, callback);
                            return;
                        }

                        AttendanceResult parsed = AttendanceResult.parse(body);
                        if (parsed.success) {
                            callback.onResult(parsed);
                        } else {
                            callback.onResult(AttendanceResult.fail(
                                    parsed.code.equals(AttendanceCodes.ERROR) ? AttendanceCodes.ERROR : parsed.code,
                                    parsed.message.isEmpty() ? body : parsed.message));
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", com.app.fourscontracting.core.utils.Constants.USER_AGENT);
                headers.put("Accept", "*/*");
                headers.put("Cookie", com.app.fourscontracting.CookieHurlStack.getBypassCookie(appContext));
                headers.put("x-api-key", ApiConfig.X_API_KEY);
                headers.put("X-API-KEY", ApiConfig.X_API_KEY);
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return payload.toFormParams();
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        MySingleton.getmInstance(appContext).addToRequestque(request);
    }

    private static boolean hasGps(AttendancePayload payload) {
        return payload != null
                && !Double.isNaN(payload.lat)
                && !Double.isNaN(payload.lng)
                && !(payload.lat == 0.0 && payload.lng == 0.0);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
