package com.app.fourscontracting;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity so every screen inherits the larger app font scale.
 */
public abstract class AppActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(FontScaleHelper.wrap(newBase));
    }
}
