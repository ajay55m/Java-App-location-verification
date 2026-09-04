package com.app.fourscontracting.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.app.fourscontracting.CookieHurlStack;
import com.app.fourscontracting.MySingleton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * High-performance image loader designed specifically for get_photo.php binary streams.
 * Powered by Glide with custom headers, Volley fallback, and magic-byte sanitization.
 */
public class ImageLoaderHelper {

    private static final String TAG = "ImageLoaderHelper";
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static Handler mainHandler;
    private static LruCache<String, Bitmap> memoryCache;

    private static synchronized Handler getMainHandler() {
        if (mainHandler == null) {
            try {
                mainHandler = new Handler(Looper.getMainLooper());
            } catch (Exception ignored) {}
        }
        return mainHandler;
    }

    private static synchronized LruCache<String, Bitmap> getMemoryCache() {
        if (memoryCache == null) {
            try {
                int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
                int cacheSize = Math.max(maxMemory / 8, 4 * 1024);
                memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                    @Override
                    protected int sizeOf(String key, Bitmap bitmap) {
                        return bitmap.getByteCount() / 1024;
                    }
                };
            } catch (Exception ignored) {}
        }
        return memoryCache;
    }

    public static void loadImage(Context context, String photoUrl, ImageView targetView) {
        loadImage(context, photoUrl, targetView, null);
    }

    public static void loadImage(Context context, String photoUrl, ImageView imgPhoto, View avatarView) {
        if (imgPhoto == null && avatarView == null) return;
        ImageView target = imgPhoto != null ? imgPhoto : (ImageView) avatarView;

        if (photoUrl == null || photoUrl.trim().isEmpty() || "null".equalsIgnoreCase(photoUrl.trim())) {
            if (target != null) {
                target.setTag(null);
                target.setImageResource(com.app.fourscontracting.R.drawable.ic_baseline_person_24);
            }
            if (imgPhoto != null && avatarView != null) {
                imgPhoto.setImageDrawable(null);
                imgPhoto.setVisibility(View.GONE);
                avatarView.setVisibility(View.VISIBLE);
            }
            return;
        }

        String url = sanitizeUrl(photoUrl);
        if (url == null || url.isEmpty()) {
            if (target != null) {
                target.setTag(null);
                target.setImageResource(com.app.fourscontracting.R.drawable.ic_baseline_person_24);
            }
            if (imgPhoto != null && avatarView != null) {
                imgPhoto.setImageDrawable(null);
                imgPhoto.setVisibility(View.GONE);
                avatarView.setVisibility(View.VISIBLE);
            }
            return;
        }

        target.setTag(url);

        // Check Memory Cache first
        LruCache<String, Bitmap> cache = getMemoryCache();
        Bitmap cached = cache != null ? cache.get(url) : null;
        if (cached != null) {
            if (imgPhoto != null && avatarView != null) {
                imgPhoto.setImageBitmap(cached);
                imgPhoto.setVisibility(View.VISIBLE);
                avatarView.setVisibility(View.GONE);
            } else {
                target.setImageBitmap(cached);
            }
            return;
        }

        // Show avatar placeholder while loading
        if (imgPhoto != null && avatarView != null) {
            imgPhoto.setImageDrawable(null);
            imgPhoto.setVisibility(View.GONE);
            if (avatarView instanceof ImageView) {
                ((ImageView) avatarView).setImageResource(com.app.fourscontracting.R.drawable.ic_baseline_person_24);
            }
            avatarView.setVisibility(View.VISIBLE);
        } else {
            target.setImageResource(com.app.fourscontracting.R.drawable.ic_baseline_person_24);
        }

        final String requestUrl = url;

        // Try Glide loading with authenticated headers
        if (context != null && !requestUrl.startsWith("data:image/")) {
            try {
                LazyHeaders.Builder builder = new LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        .addHeader("Referer", ApiConfig.SUBCONTRACTOR + "/");

                String cookie = CookieHurlStack.getBypassCookie(context);
                if (cookie != null && !cookie.isEmpty()) {
                    builder.addHeader("Cookie", cookie);
                }

                GlideUrl glideUrl = new GlideUrl(requestUrl, builder.build());

                Glide.with(context)
                        .asBitmap()
                        .load(glideUrl)
                        .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                                Object currentTag = target.getTag();
                                if (currentTag == null || requestUrl.equals(currentTag)) {
                                    LruCache<String, Bitmap> c = getMemoryCache();
                                    if (c != null) c.put(requestUrl, resource);

                                    if (imgPhoto != null && avatarView != null) {
                                        imgPhoto.setImageBitmap(resource);
                                        imgPhoto.setVisibility(View.VISIBLE);
                                        avatarView.setVisibility(View.GONE);
                                    } else {
                                        target.setImageBitmap(resource);
                                    }
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

                            @Override
                            public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                                loadWithSanitizerFallback(context, requestUrl, imgPhoto, avatarView, target);
                            }
                        });
                return;
            } catch (Exception e) {
                Log.w(TAG, "Glide load failed, using stream sanitizer fallback: " + e.getMessage());
            }
        }

        loadWithSanitizerFallback(context, requestUrl, imgPhoto, avatarView, target);
    }

    private static void loadWithSanitizerFallback(Context context, String requestUrl, ImageView imgPhoto, View avatarView, ImageView target) {
        final Context appContext = context != null ? context.getApplicationContext() : null;

        executor.execute(() -> {
            Bitmap bitmap = downloadAndSanitizeBitmap(appContext, requestUrl);
            if (bitmap != null) {
                LruCache<String, Bitmap> c = getMemoryCache();
                if (c != null) {
                    c.put(requestUrl, bitmap);
                }
            }

            Handler handler = getMainHandler();
            if (handler != null) {
                handler.post(() -> {
                    Object currentTag = target.getTag();
                    if (currentTag == null || requestUrl.equals(currentTag)) {
                        if (bitmap != null) {
                            if (imgPhoto != null && avatarView != null) {
                                imgPhoto.setImageBitmap(bitmap);
                                imgPhoto.setVisibility(View.VISIBLE);
                                avatarView.setVisibility(View.GONE);
                            } else {
                                target.setImageBitmap(bitmap);
                            }
                        } else {
                            if (imgPhoto != null && avatarView != null) {
                                imgPhoto.setVisibility(View.GONE);
                                avatarView.setVisibility(View.VISIBLE);
                            } else {
                                target.setImageResource(com.app.fourscontracting.R.drawable.ic_court_suit_avatar);
                            }
                        }
                    }
                });
            }
        });
    }

    public static String sanitizeUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty() || "null".equalsIgnoreCase(photoUrl.trim())) {
            return null;
        }
        String url = photoUrl.trim();
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        } else if (!url.startsWith("https://")) {
            if (url.startsWith("/")) {
                url = ApiConfig.HOST + url;
            } else {
                url = ApiConfig.SUBCONTRACTOR + "/" + url;
            }
        }
        return url.replace(" ", "%20");
    }

    public static Map<String, String> buildImageRequestHeaders(Context context) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Referer", ApiConfig.SUBCONTRACTOR + "/");

        String cookieVal;
        if (context != null) {
            try {
                cookieVal = CookieHurlStack.getBypassCookie(context);
            } catch (Exception ignored) {
                cookieVal = CookieHurlStack.DEFAULT_BYPASS_COOKIE;
            }
        } else {
            cookieVal = CookieHurlStack.DEFAULT_BYPASS_COOKIE;
        }
        if (cookieVal != null && !cookieVal.isEmpty()) {
            headers.put("Cookie", cookieVal);
        }

        return headers;
    }

    public static Map<String, String> buildImageAuthRetryHeaders(Context context) {
        Map<String, String> headers = buildImageRequestHeaders(context);
        headers.put(ApiConfig.HEADER_X_API_KEY, ApiConfig.X_API_KEY);
        return headers;
    }

    private static Bitmap downloadAndSanitizeBitmap(Context context, String initialUrl) {
        if (initialUrl != null && initialUrl.startsWith("data:image/") && initialUrl.contains(";base64,")) {
            try {
                String base64Data = initialUrl.substring(initialUrl.indexOf(";base64,") + 8);
                byte[] rawBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                Bitmap bmp = decodeJpegWithSanitization(rawBytes);
                if (bmp != null) return bmp;
            } catch (Exception e) {
                Log.e(TAG, "Error decoding base64 image: " + e.getMessage());
            }
        }

        String currentUrl = initialUrl;
        int redirects = 0;

        while (redirects < 5) {
            HttpURLConnection conn = null;
            InputStream is = null;
            try {
                URL url = new URL(currentUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");

                for (Map.Entry<String, String> entry : buildImageRequestHeaders(context).entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }

                int responseCode = conn.getResponseCode();

                // If Attempt 1 failed with 401/403/406/409, retry with x-api-key and same browser-like headers
                if (responseCode == 401 || responseCode == 403 || responseCode == 406 || responseCode == 409) {
                    conn.disconnect();
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    for (Map.Entry<String, String> entry : buildImageAuthRetryHeaders(context).entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                    responseCode = conn.getResponseCode();
                }

                // Handle Redirects (301, 302, 303, 307, 308)
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == 307 || responseCode == 308) {
                    String redirectUrl = conn.getHeaderField("Location");
                    if (redirectUrl != null && !redirectUrl.isEmpty()) {
                        if (!redirectUrl.startsWith("http://") && !redirectUrl.startsWith("https://")) {
                            redirectUrl = new URL(url, redirectUrl).toString();
                        }
                        currentUrl = redirectUrl.replace("http://", "https://");
                        redirects++;
                        conn.disconnect();
                        continue;
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    is = conn.getInputStream();
                    byte[] rawBytes = readStreamToByteArray(is);
                    return decodeJpegWithSanitization(rawBytes);
                } else {
                    Log.w(TAG, "Failed HTTP " + responseCode + " for photo: " + currentUrl);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading photo " + currentUrl + ": " + e.getMessage());
            } finally {
                if (is != null) {
                    try { is.close(); } catch (Exception ignored) {}
                }
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
            break;
        }
        return null;
    }

    private static byte[] readStreamToByteArray(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private static Bitmap decodeJpegWithSanitization(byte[] data) {
        if (data == null || data.length < 4) return null;

        int offset = 0;
        boolean foundMagic = false;
        int maxScan = Math.min(data.length - 3, 16384);

        for (int i = 0; i < maxScan; i++) {
            int b0 = data[i] & 0xFF;
            int b1 = data[i + 1] & 0xFF;

            // 1. JPEG (0xFF 0xD8)
            if (b0 == 0xFF && b1 == 0xD8) {
                offset = i;
                foundMagic = true;
                break;
            }
            // 2. PNG (\x89PNG = 0x89 0x50 0x4E 0x47)
            if (i <= data.length - 4 && b0 == 0x89 && b1 == 0x50 && (data[i + 2] & 0xFF) == 0x4E && (data[i + 3] & 0xFF) == 0x47) {
                offset = i;
                foundMagic = true;
                break;
            }
            // 3. GIF (GIF87a / GIF89a = 0x47 0x49 0x46)
            if (i <= data.length - 3 && b0 == 0x47 && b1 == 0x49 && (data[i + 2] & 0xFF) == 0x46) {
                offset = i;
                foundMagic = true;
                break;
            }
            // 4. WEBP (RIFF = 0x52 0x49 0x46 0x46)
            if (i <= data.length - 4 && b0 == 0x52 && b1 == 0x49 && (data[i + 2] & 0xFF) == 0x46 && (data[i + 3] & 0xFF) == 0x46) {
                offset = i;
                foundMagic = true;
                break;
            }
            // 5. BMP (BM = 0x42 0x4D)
            if (b0 == 0x42 && b1 == 0x4D) {
                offset = i;
                foundMagic = true;
                break;
            }
        }

        // Search for JFIF signature (0x4A, 0x46, 0x49, 0x46) if 0xFF 0xD8 was corrupted/overwritten by PHP output
        if (!foundMagic && !((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8)) {
            for (int i = 0; i < Math.min(data.length - 4, 4096); i++) {
                if ((data[i] & 0xFF) == 0x4A && (data[i + 1] & 0xFF) == 0x46
                        && (data[i + 2] & 0xFF) == 0x49 && (data[i + 3] & 0xFF) == 0x46) {
                    if (i >= 6) {
                        offset = i - 6;
                        data[offset] = (byte) 0xFF;
                        data[offset + 1] = (byte) 0xD8;
                        data[offset + 2] = (byte) 0xFF;
                        data[offset + 3] = (byte) 0xE0;
                        foundMagic = true;
                    }
                    break;
                }
            }
        }

        try {
            return BitmapFactory.decodeByteArray(data, offset, data.length - offset);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding sanitized image: " + e.getMessage());
            return null;
        }
    }
}
