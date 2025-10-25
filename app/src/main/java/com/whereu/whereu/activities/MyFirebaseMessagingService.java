package com.whereu.whereu.activities;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.whereu.whereu.utils.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationHelper.createNotificationChannel(this);

        if (remoteMessage.getData().size() > 0) {
            String senderName = remoteMessage.getData().get("sender_name");
            String title = "Location Request";
            String message = "Someone wants to view your location.";

            if (senderName != null && !senderName.isEmpty()) {
                title = senderName + " wants to view your location";
                message = "From: " + senderName;
            } else if (remoteMessage.getNotification() != null) {
                if (remoteMessage.getNotification().getTitle() != null) {
                    title = remoteMessage.getNotification().getTitle();
                }
                if (remoteMessage.getNotification().getBody() != null) {
                    message = remoteMessage.getNotification().getBody();
                }
            }
            NotificationHelper.sendLocalNotification(this, title, message);
        } else if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle() != null ? remoteMessage.getNotification().getTitle() : "Location Request";
            String message = remoteMessage.getNotification().getBody() != null ? remoteMessage.getNotification().getBody() : "Someone wants to view your location.";
            NotificationHelper.sendLocalNotification(this, title, message);
        }
    }
}
