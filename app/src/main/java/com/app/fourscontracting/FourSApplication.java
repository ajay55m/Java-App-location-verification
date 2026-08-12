package com.app.fourscontracting;



import android.content.Context;



import androidx.multidex.MultiDexApplication;



import com.google.firebase.crashlytics.FirebaseCrashlytics;



/**

 * App entry — MultiDex + Crashlytics + larger global font scale.

 */

public class FourSApplication extends MultiDexApplication {

    @Override

    protected void attachBaseContext(Context base) {

        super.attachBaseContext(FontScaleHelper.wrap(base));

    }



    @Override

    public void onCreate() {

        super.onCreate();

        try {

            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

            crashlytics.setCrashlyticsCollectionEnabled(true);

        } catch (Exception ignored) {

            // FCM/Crashlytics may be unavailable on some devices; app must still run.

        }

    }

}


