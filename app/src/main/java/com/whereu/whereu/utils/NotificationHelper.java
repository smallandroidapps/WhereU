package com.whereu.whereu.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.HomeActivity;
import com.whereu.whereu.activities.NotificationActionReceiver;

public class NotificationHelper {

    private static final String CHANNEL_ID = "whereu_notification_channel";
    private static final String CHANNEL_NAME = "WhereU Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for location requests";
    private static final String TAG = "NotificationHelper";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 100});
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendNotificationForRequest(Context context, String senderId, String requestId, String senderPhotoUrl) {
        FirebaseFirestore.getInstance().collection("users").document(senderId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String senderName = documentSnapshot.getString("displayName"); // Corrected field name
                if (senderName == null || senderName.isEmpty()) {
                    senderName = "Unknown User";
                }
                String title = "Location Request from " + senderName;
                String message = senderName + " wants to know your location.";
                sendLocalNotification(context, title, message, new Intent(context, HomeActivity.class), requestId, senderId, senderPhotoUrl);
            } else {
                sendLocalNotification(context, "Location Request", "Someone wants to know your location.", new Intent(context, HomeActivity.class), requestId, senderId, null);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching sender details: " + e.getMessage());
            sendLocalNotification(context, "Location Request", "Someone wants to know your location.", new Intent(context, HomeActivity.class), requestId, senderId, null);
        });
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManagerCompat.from(context).cancel(notificationId);
    }

    public static void sendLocalNotification(Context context, String title, String message, Intent contentIntent, String requestId, String senderId, String senderPhotoUrl) {
        // Ensure channel exists
        createNotificationChannel(context);

        contentIntent.putExtra("open_fragment", "requests");
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent approveIntent = new Intent(context, NotificationActionReceiver.class).setAction("com.whereu.whereu.APPROVE").putExtra("request_id", requestId).putExtra("sender_id", senderId);
        PendingIntent approvePendingIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis() + 1, approveIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(context, NotificationActionReceiver.class).setAction("com.whereu.whereu.REJECT").putExtra("request_id", requestId).putExtra("sender_id", senderId);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis() + 2, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .addAction(android.R.drawable.ic_menu_save, "Approve", approvePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        final int notificationId = senderId != null ? senderId.hashCode() : (int) System.currentTimeMillis();

        // On Android 13+, require POST_NOTIFICATIONS runtime permission; below that, proceed
        final boolean canNotify;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canNotify = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            canNotify = true;
        }

        if (senderPhotoUrl != null && !senderPhotoUrl.isEmpty()) {
            Glide.with(context)
                    .asBitmap()
                    .load(senderPhotoUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            if (canNotify) {
                                notificationManager.notify(notificationId, builder.build());
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (canNotify) {
                                notificationManager.notify(notificationId, builder.build());
                            }
                        }
                    });
        } else {
            if (canNotify) {
                notificationManager.notify(notificationId, builder.build());
            }
        }
    }

    public static void sendLocalNotification(Context context, String title, String message) {
        sendLocalNotification(context, title, message, new Intent(context, HomeActivity.class), null, null, null);
    }
}
