package com.whereu.whereu.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.utils.NotificationHelper;
import com.whereu.whereu.activities.HomeActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class NewRequestsWorker extends Worker {

    private static final String TAG = "NewRequestsWorker";
    private static final String PREFS_NAME = "whereu_worker_prefs";
    private static final String KEY_LAST_CHECK_TS = "last_check_ts";
    private static final String KEY_NOTIFIED_IDS = "notified_request_ids";

    public NewRequestsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.d(TAG, "No authenticated user; skipping background poll.");
                return Result.success();
            }

            Context ctx = getApplicationContext();
            // Ensure notification channel exists before posting
            try {
                NotificationHelper.createNotificationChannel(ctx);
            } catch (Exception channelEx) {
                Log.w(TAG, "Failed to create notification channel in worker", channelEx);
            }
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long lastCheckTs = prefs.getLong(KEY_LAST_CHECK_TS, 0L);
            Set<String> notifiedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, new HashSet<>());
            if (notifiedIds == null) notifiedIds = new HashSet<>();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Fetch recipient's trusted contacts first
            Set<String> trustedContactPhoneNumbers = new HashSet<>();
            try {
                DocumentSnapshot userDoc = Tasks.await(
                        db.collection("users").document(currentUser.getUid()).get()
                );
                if (userDoc.exists() && userDoc.contains("trustedContacts")) {
                    List<String> trustedIds = (List<String>) userDoc.get("trustedContacts");
                    if (trustedIds != null && !trustedIds.isEmpty()) {
                        // Fetch the phone numbers of trusted contacts
                        QuerySnapshot trustedDocs = Tasks.await(
                                db.collection("users").whereIn("userId", trustedIds).get()
                        );
                        for (DocumentSnapshot trustedDoc : trustedDocs) {
                            if (trustedDoc.contains("mobileNumber")) {
                                trustedContactPhoneNumbers.add(trustedDoc.getString("mobileNumber"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch trusted contacts in worker", e);
            }

            List<LocationRequest> newPending = new ArrayList<>();
            try {
                // Fetch pending requests addressed to current user
                QuerySnapshot snapshot = Tasks.await(
                        db.collection("locationRequests")
                                .whereEqualTo("toUserId", currentUser.getUid())
                                .whereEqualTo("status", "pending")
                                .get()
                );

                List<DocumentSnapshot> docs = snapshot.getDocuments();
                for (DocumentSnapshot doc : docs) {
                    LocationRequest req = doc.toObject(LocationRequest.class);
                    if (req == null) continue;
                    req.setRequestId(doc.getId());

                    // 2. Check if requester is a trusted contact by phone number
                    try {
                        DocumentSnapshot fromUserDoc = Tasks.await(
                                db.collection("users").document(req.getFromUserId()).get()
                        );
                        if (fromUserDoc.exists() && fromUserDoc.contains("mobileNumber")) {
                            String fromPhoneNumber = fromUserDoc.getString("mobileNumber");
                            if (fromPhoneNumber != null && trustedContactPhoneNumbers.contains(fromPhoneNumber)) {
                                // 3. Auto-approve the request
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("status", "approved");
                                updates.put("approvedTimestamp", System.currentTimeMillis());
                                try {
                                    Tasks.await(db.collection("locationRequests").document(req.getRequestId()).update(updates));
                                    Log.d(TAG, "Auto-approved request from trusted contact: " + req.getFromUserId());
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to auto-approve request: " + req.getRequestId(), e);
                                }
                            } else if (req.getTimestamp() > lastCheckTs && !notifiedIds.contains(req.getRequestId())) {
                                newPending.add(req);
                            }
                        } else if (req.getTimestamp() > lastCheckTs && !notifiedIds.contains(req.getRequestId())) {
                            newPending.add(req);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to check if requester is trusted: " + req.getFromUserId(), e);
                        if (req.getTimestamp() > lastCheckTs && !notifiedIds.contains(req.getRequestId())) {
                            newPending.add(req);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch pending requests in worker", e);
                // Try again later
                return Result.retry();
            }

            for (LocationRequest req : newPending) {
                try {
                    // Send synchronously without Firestore dependency for reliability
                    String title = "New Location Request";
                    String message = "Someone wants to know your location.";
                    Intent openIntent = new Intent(ctx, HomeActivity.class);
                    NotificationHelper.sendLocalNotification(ctx, title, message, openIntent, req.getRequestId(), req.getFromUserId(), null);
                    notifiedIds.add(req.getRequestId());
                } catch (Exception e) {
                    Log.w(TAG, "Failed to send notification for request: " + req.getRequestId(), e);
                }
            }

            // Update last check timestamp
            prefs.edit()
                    .putLong(KEY_LAST_CHECK_TS, System.currentTimeMillis())
                    .putStringSet(KEY_NOTIFIED_IDS, notifiedIds)
                    .apply();

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in worker", e);
            return Result.failure();
        }
    }
}
