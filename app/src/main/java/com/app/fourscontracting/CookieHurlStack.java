package com.app.fourscontracting;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.Request;
import com.app.fourscontracting.data.ApiConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1) First try normal request (no auth headers).
 * 2) If 401/403/409 → retry with x-api-key (+ Cookie humans_21909=1 if antibot).
 */
public class CookieHurlStack extends HurlStack {
    private static final String TAG = "CookieHurlStack";
    private static final String PREF_NAME = "CookiePrefs";
    private static final String KEY_BYPASS_COOKIE = "bypass_cookie";
    public static final String DEFAULT_BYPASS_COOKIE = "humans_21909=1";

    private final Context context;

    public CookieHurlStack(Context context) {
        super();
        this.context = context.getApplicationContext();
    }

    private static volatile String cachedWvCookie = null;

    public static void syncWebViewCookies(Context context) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                cachedWvCookie = android.webkit.CookieManager.getInstance().getCookie(ApiConfig.HOST);
            } catch (Exception ignored) {
            }
        }
    }

    public static String getBypassCookie(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_BYPASS_COOKIE, null);
        String bypass = (saved != null && !saved.isEmpty()) ? saved : DEFAULT_BYPASS_COOKIE;

        String wvCookie = cachedWvCookie;
        if (wvCookie == null) {
            try {
                wvCookie = android.webkit.CookieManager.getInstance().getCookie(ApiConfig.HOST);
                if (wvCookie != null && !wvCookie.isEmpty()) {
                    cachedWvCookie = wvCookie;
                }
            } catch (Exception ignored) {
            }
        }

        if (wvCookie != null && !wvCookie.isEmpty()) {
            if (!wvCookie.contains(bypass)) {
                return bypass + "; " + wvCookie;
            }
            return wvCookie;
        }
        return bypass;
    }

    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {

        // --- Attempt 1: Attach User-Agent, x-api-key & Cookie bypass ---
        Map<String, String> headers = new HashMap<>();
        if (additionalHeaders != null) {
            headers.putAll(additionalHeaders);
        }
        if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36");
        }
        if (!headers.containsKey(ApiConfig.HEADER_X_API_KEY) && !headers.containsKey("X-API-KEY")) {
            headers.put(ApiConfig.HEADER_X_API_KEY, ApiConfig.X_API_KEY);
        }
        if (!headers.containsKey("Cookie") && !headers.containsKey("cookie")) {
            String bypassCookie = getBypassCookie(context);
            if (bypassCookie != null && !bypassCookie.isEmpty()) {
                headers.put("Cookie", bypassCookie);
            }
        }

        Log.d(TAG, "Attempt 1 (normal) → " + request.getUrl());
        HttpResponse response = super.executeRequest(request, headers);
        int statusCode = response.getStatusCode();
        Log.d(TAG, "Attempt 1 status=" + statusCode);

        if (!needsAuthRetry(statusCode)) {
            return response;
        }

        // --- Attempt 2: pass x-api-key (and Cookie for antibot hosts) ---
        Log.w(TAG, "Attempt 1 failed (" + statusCode + "). Retrying with x-api-key");

        String body = readStream(response.getContent());
        String cookie = extractCookie(body);
        if (cookie == null || cookie.isEmpty()) {
            cookie = DEFAULT_BYPASS_COOKIE;
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BYPASS_COOKIE, cookie)
                .apply();

        Map<String, String> retryHeaders = new HashMap<>();
        if (additionalHeaders != null) {
            retryHeaders.putAll(additionalHeaders);
        }
        if (!retryHeaders.containsKey("User-Agent") && !retryHeaders.containsKey("user-agent")) {
            retryHeaders.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36");
        }
        retryHeaders.put(ApiConfig.HEADER_X_API_KEY, ApiConfig.X_API_KEY);
        // Also set common casing some servers expect
        retryHeaders.put("X-API-KEY", ApiConfig.X_API_KEY);
        if (statusCode == 409 || statusCode == 403 || statusCode == 406) {
            retryHeaders.put("Cookie", cookie);
        }

        Log.d(TAG, "Attempt 2 x-api-key + optional Cookie → " + request.getUrl());
        return super.executeRequest(request, retryHeaders);
    }

    private static boolean needsAuthRetry(int statusCode) {
        return statusCode == 401 || statusCode == 403 || statusCode == 406 || statusCode == 409;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private String extractCookie(String html) {
        if (html == null) return null;
        Pattern pattern = Pattern.compile("document\\.cookie\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
