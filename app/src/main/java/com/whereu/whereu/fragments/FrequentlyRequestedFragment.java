package com.whereu.whereu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.LocationRequestDetailActivity;
import com.whereu.whereu.adapters.FrequentlyRequestedAdapter;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.utils.FrequentRequestCacheManager;
import com.whereu.whereu.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FrequentlyRequestedFragment extends Fragment implements FrequentlyRequestedAdapter.OnRequestAgainListener {

    private static final String TAG = "FrequentlyRequestedFragment";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration frequentRequestsListener;

    private RecyclerView recyclerView;
    private FrequentlyRequestedAdapter adapter;
    private List<LocationRequest> frequentRequestsList;
    private TextView emptyStateTextView;
    private Chip infoChip;
    private FrequentRequestCacheManager cacheManager;

    public FrequentlyRequestedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        cacheManager = new FrequentRequestCacheManager(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_frequently_requested, container, false);

        recyclerView = view.findViewById(R.id.frequentlyRequestedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        frequentRequestsList = new ArrayList<>();
        adapter = new FrequentlyRequestedAdapter(frequentRequestsList, this);
        recyclerView.setAdapter(adapter);

        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
        infoChip = view.findViewById(R.id.infoChip);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (currentUser != null) {
            // Load from cache first
            List<LocationRequest> cachedRequests = cacheManager.getFrequentRequests();
            if (!cachedRequests.isEmpty()) {
                frequentRequestsList.clear();
                frequentRequestsList.addAll(cachedRequests);
                adapter.notifyItemRangeInserted(0, frequentRequestsList.size());
                updateUI();
            }
            listenForFrequentRequests();
        } else {
            Log.e(TAG, "User not logged in.");
            updateUI();
        }
    }

    private void listenForFrequentRequests() {
        if (currentUser == null) return;

        if (frequentRequestsListener != null) {
            frequentRequestsListener.remove();
        }

        frequentRequestsListener = db.collection("users").document(currentUser.getUid())
                .collection("frequent_requests")
                .orderBy("requestCount", Query.Direction.DESCENDING)
                .orderBy("lastRequested", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        List<LocationRequest> fetchedRequests = new ArrayList<>();
                        List<Task<DocumentSnapshot>> userFetchTasks = new ArrayList<>();

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                LocationRequest model = dc.getDocument().toObject(LocationRequest.class);
                                fetchedRequests.add(model);
                            }
                        }

                        // Fetch user details for each frequent request to filter out deleted users
                        for (LocationRequest model : fetchedRequests) {
                            userFetchTasks.add(db.collection("users").document(model.getToUserId()).get());
                        }

                        Tasks.whenAllSuccess(userFetchTasks).addOnSuccessListener(results -> {
                            int originalSize = frequentRequestsList.size();
                            frequentRequestsList.clear();
                            adapter.notifyItemRangeRemoved(0, originalSize);
                            for (int i = 0; i < fetchedRequests.size(); i++) {
                                DocumentSnapshot userSnapshot = (DocumentSnapshot) results.get(i);
                                if (userSnapshot.exists()) {
                                    // User still exists, add to the list
                                    frequentRequestsList.add(fetchedRequests.get(i));
                                }
                            }

                            // Sort again to ensure correct order after adding/modifying
                            frequentRequestsList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                            adapter.notifyItemRangeInserted(0, frequentRequestsList.size());
                            cacheManager.saveFrequentRequests(frequentRequestsList); // Save to cache
                            updateUI();
                        }).addOnFailureListener(innerE -> {
                            Log.e(TAG, "Error fetching user details for frequent requests", innerE);
                            updateUI(); // Update UI even if user fetch fails
                        });
                    }
                });
    }

    private void updateUI() {
        if (frequentRequestsList.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            infoChip.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            infoChip.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (frequentRequestsListener != null) {
            frequentRequestsListener.remove();
        }
    }

    @Override
    public void onRequestAgain(LocationRequest model) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new LocationRequest
        String newRequestId = db.collection("locationRequests").document().getId();
        LocationRequest newRequest = new LocationRequest(
                currentUser.getUid(),
                model.getToUserId()
        );
        newRequest.setRequestId(newRequestId);
        newRequest.setStatus("pending");
        newRequest.setTimestamp(System.currentTimeMillis());
        newRequest.setLatitude(0.0);
        newRequest.setLongitude(0.0);
        newRequest.setAreaName("");
        newRequest.setDistance(0.0);

        db.collection("locationRequests").document(newRequestId).set(newRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New location request sent for " + model.getToUserId());
                    Toast.makeText(getContext(), "Location request sent to " + model.getToUserId(), Toast.LENGTH_SHORT).show();

                    // Update lastRequested timestamp in frequent_requests sub-collection
                    DocumentReference frequentRequestRef = db.collection("users").document(currentUser.getUid())
                            .collection("frequent_requests").document(model.getToUserId());

                    frequentRequestRef.update("lastRequested", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid1 -> Log.d(TAG, "lastRequested updated for " + model.getToUserId()))
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating lastRequested for " + model.getToUserId(), e));

                    Log.d(TAG, "Calling sendNotificationForRequest for receiver: " + model.getToUserId());
                    NotificationHelper.sendNotificationForRequest(getContext(), currentUser.getUid(), model.getToUserId(), newRequest.getRequestId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending new location request", e);
                    Toast.makeText(getContext(), "Failed to send location request.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onViewFromDbClicked(LocationRequest model) {
        Intent intent = new Intent(getContext(), LocationRequestDetailActivity.class);
        intent.putExtra("locationRequest", model);
        startActivity(intent);
    }
}
