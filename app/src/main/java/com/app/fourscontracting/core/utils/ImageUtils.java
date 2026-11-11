package com.app.fourscontracting.core.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class ImageUtils {
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        int w = 800;
        int h = (int) (bitmap.getHeight() * (800.0 / Math.max(1, bitmap.getWidth())));
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, Math.max(w, 1), Math.max(h, 1), true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }
}

