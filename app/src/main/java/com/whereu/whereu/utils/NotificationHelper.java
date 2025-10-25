package com.whereu.whereu.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.HomeActivity;

public class NotificationHelper {

    private static final String CHANNEL_ID = "whereu_notification_channel";
    private static final String CHANNEL_NAME = "WhereU Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for WhereU app";
    private static final String TAG = "NotificationHelper";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendNotificationForRequest(Context context, String senderId) {
        FirebaseFirestore.getInstance().collection("users").document(senderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String senderName = documentSnapshot.getString("displayName");
                        String title = (senderName != null ? senderName : "Someone") + " wants to view your location.";
                        String message = "Tap to respond.";

                        Intent intent = new Intent(context, HomeActivity.class);
                        intent.putExtra("open_fragment", "requests");

                        sendLocalNotification(context, title, message, intent);
                    } else {
                        Log.e(TAG, "Sender document not found for ID: " + senderId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch sender name for notification", e);
                });
    }

    public static void sendLocalNotification(Context context, String title, String message, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public static void sendLocalNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, HomeActivity.class);
        sendLocalNotification(context, title, message, intent);
    }
}
