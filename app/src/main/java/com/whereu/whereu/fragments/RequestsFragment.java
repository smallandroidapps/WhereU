package com.whereu.whereu.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.whereu.whereu.activities.MapActivity;
import com.wheru.adapters.RequestAdapter;
import com.whereu.whereu.models.LocationRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    private static final String TAG = "RequestsFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        recyclerView = view.findViewById(R.id.recycler_view_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(requestList, this);
        recyclerView.setAdapter(requestAdapter);

        fetchRequests();

        return view;
    }

    private void fetchRequests() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            addDummyRequests();
            return;
        }

        String currentUserId = currentUser.getUid();

        Task<QuerySnapshot> incomingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("receiverId", currentUserId)
                .get();

        Task<QuerySnapshot> outgoingRequestsTask = db.collection("locationRequests")
                .whereEqualTo("senderId", currentUserId)
                .get();

        Tasks.whenAllSuccess(incomingRequestsTask, outgoingRequestsTask).addOnSuccessListener(list -> {
            requestList.clear();
            for (Object snapshot : list) {
                for (QueryDocumentSnapshot document : (QuerySnapshot) snapshot) {
                    LocationRequest request = document.toObject(LocationRequest.class);
                    request.setRequestId(document.getId());
                    requestList.add(request);
                }
            }

            if (requestList.isEmpty()) {
                addDummyRequests();
            }
            requestAdapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching requests", e);
            addDummyRequests();
        });
    }

    private void addDummyRequests() {
        requestList.add(new LocationRequest("dummySenderId1", "dummyReceiverId1"));
        requestList.add(new LocationRequest("dummySenderId2", "dummyReceiverId2"));
        requestAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestApproved(int position, LocationRequest request) {
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
    public void onRequestRejected(int position, LocationRequest request) {
        updateRequestStatus(request, "rejected");
    }

    @Override
    public void onRequestItemClicked(int position, LocationRequest request) {
        if (request.getStatus().equals("approved")) {
            openMapForRequest(request);
        }
    }

    private void updateRequestWithLocation(LocationRequest request, Location location) {
        DocumentReference requestRef = db.collection("locationRequests").document(request.getRequestId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("approvedAt", new Date());
        updates.put("latitude", location.getLatitude());
        updates.put("longitude", location.getLongitude());

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request approved", Toast.LENGTH_SHORT).show();
                    fetchRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating request", Toast.LENGTH_SHORT).show();
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
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating request", Toast.LENGTH_SHORT).show();
                });
    }

    private void openMapForRequest(LocationRequest request) {
        Intent intent = new Intent(getContext(), MapActivity.class);
        intent.putExtra("latitude", request.getLatitude());
        intent.putExtra("longitude", request.getLongitude());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try approving the request again
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
