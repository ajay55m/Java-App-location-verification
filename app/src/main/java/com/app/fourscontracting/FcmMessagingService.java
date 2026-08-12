package com.app.fourscontracting;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FcmMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", token);
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.FCM_TOKEN), token);
        editor.apply();
    }

    public static void fetchAndSaveToken(Context context) {
        if (context == null) return;
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        SharedPreferences sharedPreferences = context.getApplicationContext()
                                .getSharedPreferences(context.getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                        sharedPreferences.edit().putString(context.getString(R.string.FCM_TOKEN), token).apply();
                        Log.d("FCM_TOKEN_FETCHED", token);
                    }
                });
        } catch (Exception e) {
            Log.e("FCM_TOKEN_ERROR", "Failed to fetch FCM token", e);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() == null) {
            return;
        }
        String title = remoteMessage.getNotification().getTitle();
        String message = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();

        Intent intent = click_action != null ? new Intent(click_action) : new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int pendingFlags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingFlags);
        String channelId = "Default";

        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(this, channelId);
        notificationBulider.setContentTitle(title);
        notificationBulider.setContentText(message);
        notificationBulider.setSmallIcon(R.mipmap.ic_launcher);
        notificationBulider.setColor(ContextCompat.getColor(this, R.color.colorAccent));
        notificationBulider.setAutoCancel(true);
        notificationBulider.setWhen(System.currentTimeMillis());
        notificationBulider.setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        notificationManager.notify(0,notificationBulider.build());
        super.onMessageReceived(remoteMessage);
    }
}
