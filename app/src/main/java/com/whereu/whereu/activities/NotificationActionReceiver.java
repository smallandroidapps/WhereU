package com.whereu.whereu.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.utils.NotificationHelper;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";
    public static final String ACTION_APPROVE = "com.whereu.whereu.APPROVE";
    public static final String ACTION_REJECT = "com.whereu.whereu.REJECT";
    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_SENDER_ID = "sender_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String senderId = intent.getStringExtra(EXTRA_SENDER_ID);

        if (requestId == null || senderId == null) {
            Log.e(TAG, "Missing request ID or sender ID");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (ACTION_APPROVE.equals(action)) {
            db.collection("requests").document(requestId)
                    .update("status", "Approved")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Request approved");
                        // Trigger location sharing logic here
                        NotificationHelper.cancelNotification(context, requestId.hashCode());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to approve request", e));
        } else if (ACTION_REJECT.equals(action)) {
            db.collection("requests").document(requestId)
                    .update("status", "Rejected")
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Request rejected");
                        NotificationHelper.cancelNotification(context, requestId.hashCode());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to reject request", e));
        }
    }
}