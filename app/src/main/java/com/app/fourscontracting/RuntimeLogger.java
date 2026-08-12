package com.app.fourscontracting;

import android.content.Context;
import android.os.Build;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class RuntimeLogger {

    private static final String URL = "https://4scontracting.com/SMCS_APP/fcm_app/app_logger.php";

    public static void log(Context context, String userId, String level, String component, String message) {
        // Automatically get Thread and Device details
        final String threadName = Thread.currentThread().getName();
        final String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;

        StringRequest request = new StringRequest(Request.Method.POST, URL, null, null) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId != null ? userId : "guest");
                params.put("thread_name", threadName);
                params.put("component", component);
                params.put("log_type", level); // "INFO", "DEBUG", or "ERROR"
                params.put("message", message);
                params.put("device_info", deviceInfo);
                return params;
            }
        };
        Volley.newRequestQueue(context).add(request);
    }
}
