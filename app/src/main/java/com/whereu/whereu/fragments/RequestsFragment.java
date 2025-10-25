package com.whereu.whereu.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.R;
import com.wheru.adapters.RequestAdapter;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    public interface NotificationListener {
        void onSendLocalNotification(String title, String message);
    }

    private NotificationListener notificationListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationListener) {
            notificationListener = (NotificationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NotificationListener");
        }
    }

    private static final String TAG = "RequestsFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;

    private RecyclerView frequentlyRequestedRecyclerView;
    private RequestAdapter frequentlyRequestedAdapter;
    private List<LocationRequest> frequentlyRequestedList;
    private TextView frequentlyRequestedTitle;
    private TextView allRequestsTitle;

    private FusedLocationProviderClient fusedLocationClient;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        frequentlyRequestedTitle = view.findViewById(R.id.text_view_frequently_requested_title);
        frequentlyRequestedRecyclerView = view.findViewById(R.id.recycler_view_frequently_requested);
        frequentlyRequestedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        frequentlyRequestedList = new ArrayList<>();
        frequentlyRequestedAdapter = new RequestAdapter(frequentlyRequestedList, this);
        frequentlyRequestedRecyclerView.setAdapter(frequentlyRequestedAdapter);

        allRequestsTitle = view.findViewById(R.id.text_view_all_requests_title);
        recyclerView = view.findViewById(R.id.recycler_view_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(requestList, this);
        recyclerView.setAdapter(requestAdapter);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::fetchRequests);

        fetchRequests();

        return view;
    }

    private void fetchRequests() {
        swipeRefreshLayout.setRefreshing(true);
        if (currentUser == null) return;

        // Fetch frequently requested (approved within last 24 hours)
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        db.collection("locationRequests")
                .whereEqualTo("status", "approved")
                .whereGreaterThanOrEqualTo("approvedTimestamp", twentyFourHoursAgo)
                .orderBy("approvedTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        frequentlyRequestedList.clear();
                        List<Task<User>> userFetchTasks = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LocationRequest request = document.toObject(LocationRequest.class);
                            request.setRequestId(document.getId());

                            String otherUserId = request.getFromUserId().equals(currentUser.getUid()) ? request.getToUserId() : request.getFromUserId();

                            Task<User> userTask = db.collection("users").document(otherUserId).get().continueWith(userTaskSnapshot -> {
                                if (userTaskSnapshot.isSuccessful()) {
                                    return userTaskSnapshot.getResult().toObject(User.class);
                                } else {
                                    return null;
                                }
                            });
                            userFetchTasks.add(userTask);
                            frequentlyRequestedList.add(request);
                        }

                        Tasks.whenAllSuccess(userFetchTasks).addOnSuccessListener(users -> {
                            for (int i = 0; i < users.size(); i++) {
                                User user = (User) users.get(i);
                                if (user != null) {
                                    frequentlyRequestedList.get(i).setUserName(user.getDisplayName());
                                }
                            }
                            frequentlyRequestedAdapter.notifyDataSetChanged();
                            frequentlyRequestedTitle.setVisibility(frequentlyRequestedList.isEmpty() ? View.GONE : View.VISIBLE);
                            frequentlyRequestedRecyclerView.setVisibility(frequentlyRequestedList.isEmpty() ? View.GONE : View.VISIBLE);
                        });
                    }
                });

        // Fetch all other requests
        Task<QuerySnapshot> incomingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("toUserId", currentUser.getUid())
                .get();

        Task<QuerySnapshot> outgoingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .get();

        Tasks.whenAllSuccess(incomingRequestsTask, outgoingRequestsTask).addOnSuccessListener(list -> {
            requestList.clear();
            Map<String, Task<User>> userFetchTasks = new HashMap<>();

            for (Object snapshot : list) {
                for (QueryDocumentSnapshot document : (QuerySnapshot) snapshot) {
                    LocationRequest request = document.toObject(LocationRequest.class);
                    request.setRequestId(document.getId());

                    String otherUserId = request.getFromUserId().equals(currentUser.getUid()) ? request.getToUserId() : request.getFromUserId();

                    if (!userFetchTasks.containsKey(otherUserId)) {
                        Task<User> userTask = db.collection("users").document(otherUserId).get().continueWith(userTaskSnapshot -> {
                            if (userTaskSnapshot.isSuccessful()) {
                                return userTaskSnapshot.getResult().toObject(User.class);
                            } else {
                                return null;
                            }
                        });
                        userFetchTasks.put(otherUserId, userTask);
                    }
                    requestList.add(request);
                }
            }

            Tasks.whenAllComplete(userFetchTasks.values()).addOnCompleteListener(task -> {
                for (LocationRequest request : requestList) {
                    String otherUserId = request.getFromUserId().equals(currentUser.getUid()) ? request.getToUserId() : request.getFromUserId();
                    Task<User> userTask = userFetchTasks.get(otherUserId);
                    if (userTask != null && userTask.isSuccessful()) {
                        User user = userTask.getResult();
                        if (user != null) {
                            request.setUserName(user.getDisplayName());
                        }
                    }
                }
                requestList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                requestAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching requests", e);
            swipeRefreshLayout.setRefreshing(false);
        });
    }


    @Override
    public void onApproveClicked(LocationRequest request) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateRequestWithLocation(request, location);
            } else {
                Toast.makeText(getContext(), "Could not get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRejectClicked(LocationRequest request) {
        updateRequestStatus(request, "rejected");
    }

    private void updateRequestStatus(LocationRequest request, String status) {
        DocumentReference requestRef = db.collection("locationRequests").document(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        if (status.equals("approved")) {
            updates.put("approvedTimestamp", System.currentTimeMillis());
        }

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status, Toast.LENGTH_SHORT).show();
                    if (notificationListener != null && request.getUserName() != null) {
                        String message = status.equals("approved") ?
                                "Your location is now visible to " + request.getUserName() + "." :
                                "You have rejected the location request from " + request.getUserName() + ".";
                        notificationListener.onSendLocalNotification("Location Request " + status.substring(0, 1).toUpperCase() + status.substring(1), message);
                    }
                    fetchRequests();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating request", Toast.LENGTH_SHORT).show());
    }

    private void updateRequestWithLocation(LocationRequest request, Location location) {
        DocumentReference requestRef = db.collection("locationRequests").document(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("latitude", location.getLatitude());
        updates.put("longitude", location.getLongitude());
        updates.put("areaName", getAreaName(location.getLatitude(), location.getLongitude()));
        updates.put("approvedTimestamp", System.currentTimeMillis());

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Location shared successfully!", Toast.LENGTH_SHORT).show();
                    if (notificationListener != null && request.getUserName() != null) {
                        notificationListener.onSendLocalNotification("Location Shared", "Your location has been shared with " + request.getUserName() + ".");
                    }
                    fetchRequests();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error sharing location", Toast.LENGTH_SHORT).show());
    }

    private String getAreaName(double latitude, double longitude) {
        if (getContext() == null) return "Unknown Area";
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
    public void onRequestAgainClicked(LocationRequest request) {
        if (currentUser == null) return;
        LocationRequest newRequest = new LocationRequest(currentUser.getUid(), request.getToUserId().equals(currentUser.getUid()) ? request.getFromUserId() : request.getToUserId());
        db.collection("locationRequests").add(newRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Location request sent again!", Toast.LENGTH_SHORT).show();
                    fetchRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send request.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to send location request to " + newRequest.getToUserId(), e);
                });
    }
}
