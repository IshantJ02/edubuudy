package com.edu.edubuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            // Display notification
            showNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

        // You can also handle data messages here if necessary
        if (remoteMessage.getData().size() > 0) {
            // Process data payload if necessary
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Log or send token to your server for future use if needed
    }

    private void showNotification(String title, String body) {
        // Create a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.noti)  // Update this line with your actual icon name
                .setAutoCancel(true);

        // Get the notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for devices running Android Oreo or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Channel for general notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        // Notify with a unique ID
        notificationManager.notify(0, builder.build());
    }
}
