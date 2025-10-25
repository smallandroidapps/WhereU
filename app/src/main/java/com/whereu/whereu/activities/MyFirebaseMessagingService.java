package com.whereu.whereu.activities;

import android.content.Intent;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.whereu.whereu.utils.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Log the token for now. In a real app, you'd send this to your server.
        Log.d("FCM_Token", "Refreshed token: " + token);
        // You would typically send this token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM_Token", "Token successfully updated for user: " + userId))
                    .addOnFailureListener(e -> Log.e("FCM_Token", "Error updating token for user: " + userId, e));
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationHelper.createNotificationChannel(this);

        if (remoteMessage.getData().size() > 0) {
            String senderName = remoteMessage.getData().get("sender_name");
            String requestId = remoteMessage.getData().get("request_id"); // Extract request_id
            String title = "Location Request";
            String message = "Someone wants to view your location.";
            String openTab = remoteMessage.getData().get("openTab");

            Intent intent = new Intent(this, HomeActivity.class);
            if (openTab != null) {
                intent.putExtra("openTab", openTab);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

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
            // Pass requestId to sendLocalNotification
            NotificationHelper.sendLocalNotification(this, title, message, intent, requestId, senderName, null);
        } else if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle() != null ? remoteMessage.getNotification().getTitle() : "Location Request";
            String message = remoteMessage.getNotification().getBody() != null ? remoteMessage.getNotification().getBody() : "Someone wants to view your location.";
            // For notifications without data payload, we don't have a requestId or senderId
            NotificationHelper.sendLocalNotification(this, title, message, new Intent(this, HomeActivity.class), null, null, null);
        }
    }

}
