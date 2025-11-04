package com.whereu.whereu.workers;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                    if (req.getTimestamp() > lastCheckTs && !notifiedIds.contains(req.getRequestId())) {
                        newPending.add(req);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch pending requests in worker", e);
                // Try again later
                return Result.retry();
            }

            for (LocationRequest req : newPending) {
                try {
                    // Sender photo handled inside helper; pass application context
                    NotificationHelper.sendNotificationForRequest(ctx, req.getFromUserId(), req.getRequestId(), null);
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
