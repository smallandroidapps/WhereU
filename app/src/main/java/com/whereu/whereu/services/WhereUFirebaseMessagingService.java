package com.whereu.whereu.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.whereu.whereu.activities.HomeActivity;
import com.whereu.whereu.utils.NotificationHelper;

/**
 * Handles FCM messages and token updates. Builds app notifications that deep-link
 * into the Requests page and selects the appropriate tab (To Me / From Me).
 */
public class WhereUFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "WhereUFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .update("fcmToken", token)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved"))
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to save FCM token", e));
            } else {
                Log.d(TAG, "No logged-in user; token will be saved after sign-in");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error handling new token", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Determine which tab to open from data payload
        String openTab = null;
        if (remoteMessage.getData() != null) {
            openTab = remoteMessage.getData().get("openTab");
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("open_fragment", "requests");
        if (openTab != null) {
            // Map values to pager index: to_me -> 0, from_me -> 1
            int tabIndex = "from_me".equalsIgnoreCase(openTab) ? 1 : 0;
            intent.putExtra("requests_tab", tabIndex);
        }

        if (title == null) title = "WhereU";
        if (body == null) body = "You have a new update.";

        NotificationHelper.sendLocalNotification(this, title, body, intent, null, null, null);
    }
}
