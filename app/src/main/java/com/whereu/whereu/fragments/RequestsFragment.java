package com.whereu.whereu.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.LocationDetailsActivity;
import com.wheru.adapters.RequestAdapter;
import com.whereu.whereu.models.LocationRequest;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener, RequestDetailsBottomSheetFragment.OnRequestDetailsActionListener {

    private static final String TAG = "RequestsFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;
    private FusedLocationProviderClient fusedLocationClient;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        recyclerView = view.findViewById(R.id.recycler_view_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(requestList, this);
        recyclerView.setAdapter(requestAdapter);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchRequests();
        });

        fetchRequests();

        return view;
    }

    private void fetchRequests() {
        swipeRefreshLayout.setRefreshing(true);
        String currentUserId = mAuth.getCurrentUser().getUid();

        Task<QuerySnapshot> incomingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("receiverId", currentUserId)
                .get();

        Task<QuerySnapshot> outgoingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("senderId", currentUserId)
                .get();

        Tasks.whenAllSuccess(incomingRequestsTask, outgoingRequestsTask).addOnSuccessListener(list -> {
            requestList.clear();
            List<Task<Void>> nameFetchTasks = new ArrayList<>();

            for (Object snapshot : list) {
                for (QueryDocumentSnapshot document : (QuerySnapshot) snapshot) {
                    LocationRequest request = document.toObject(LocationRequest.class);
                    if (request.getExpiresAt() != null && request.getExpiresAt().after(new Date())) {
                        request.setRequestId(document.getId());

                        // Fetch sender's name
                        Task<Void> senderNameTask = db.collection("users").document(request.getSenderId()).get()
                                .continueWith(task -> {
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        request.setSenderName(task.getResult().getString("displayName"));
                                    }
                                    return null;
                                });
                        nameFetchTasks.add(senderNameTask);

                        // Fetch receiver's name
                        Task<Void> receiverNameTask = db.collection("users").document(request.getReceiverId()).get()
                                .continueWith(task -> {
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        request.setReceiverName(task.getResult().getString("displayName"));
                                    }
                                    return null;
                                });
                        nameFetchTasks.add(receiverNameTask);

                        requestList.add(request);
                    } else {
                        document.getReference().delete();
                    }
                }
            }

            Tasks.whenAllSuccess(nameFetchTasks).addOnSuccessListener(aVoid -> {
                // Sort the requestList by timestamp (latest first)
                requestList.sort((r1, r2) -> {
                    Date t1 = "approved".equals(r1.getStatus()) ? r1.getApprovedAt() : r1.getCreatedAt();
                    Date t2 = "approved".equals(r2.getStatus()) ? r2.getApprovedAt() : r2.getCreatedAt();
                    if (t1 == null || t2 == null) return 0;
                    return t2.compareTo(t1); // Latest first
                });
                requestAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user names", e);
                swipeRefreshLayout.setRefreshing(false);
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching requests", e);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void updateRequestStatus(LocationRequest request, String status) {
        DocumentReference requestRef = db.collection("locationRequests").document(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status, Toast.LENGTH_SHORT).show();
                    fetchRequests();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating request", Toast.LENGTH_SHORT).show());
    }

    private String getAreaName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Area";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. You can now try to get the location again.
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestItemClicked(int position, LocationRequest request) {
        RequestDetailsBottomSheetFragment bottomSheet = RequestDetailsBottomSheetFragment.newInstance(request);
        bottomSheet.setOnRequestDetailsActionListener(this);
        bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
    }

    private void updateRequestWithLocation(LocationRequest request, Location location) {
        DocumentReference requestRef = db.collection("locationRequests").document(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("latitude", location.getLatitude());
        updates.put("longitude", location.getLongitude());
        updates.put("areaName", getAreaName(location.getLatitude(), location.getLongitude()));
        updates.put("approvedAt", new Date());

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Location shared successfully!", Toast.LENGTH_SHORT).show();
                    fetchRequests();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error sharing location", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestApproved(int position, LocationRequest request) {
        // This method is for RequestAdapter.OnRequestActionListener
        // The actual approval logic is handled by the bottom sheet's listener
        // No action needed here as the bottom sheet will trigger the other onRequestApproved
    }

    @Override
    public void onRequestRejected(int position, LocationRequest request) {
        // This method is for RequestAdapter.OnRequestActionListener
        // The actual rejection logic is handled by the bottom sheet's listener
        // No action needed here as the bottom sheet will trigger the other onRequestRejected
    }

    // Implementation for RequestDetailsBottomSheetFragment.OnRequestDetailsActionListener
    @Override
    public void onRequestApproved(LocationRequest request) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateRequestWithLocation(request, location);
                    } else {
                        Toast.makeText(getContext(), "Could not get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestRejected(LocationRequest request) {
        updateRequestStatus(request, "rejected");
    }
}