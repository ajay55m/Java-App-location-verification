package com.app.fourscontracting;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Forces larger typography app-wide (all sp-based text).
 */
public final class FontScaleHelper {
    /** Near system default — readable without oversized UI. */
    public static final float APP_FONT_SCALE = 1.05f;

    private FontScaleHelper() {}

    public static Context wrap(Context context) {
        if (context == null) {
            return null;
        }
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Math.abs(config.fontScale - APP_FONT_SCALE) < 0.01f) {
            return context;
        }
        config.fontScale = APP_FONT_SCALE;
        return context.createConfigurationContext(config);
    }
}
