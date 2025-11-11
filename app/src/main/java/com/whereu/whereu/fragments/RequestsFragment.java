package com.whereu.whereu.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.adapters.RequestsPagerAdapter;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.fragments.LocationDetailsBottomSheetFragment;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.databinding.FragmentRequestsBinding;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.provider.Settings;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    public interface NotificationListener {
        void onSendLocalNotification(String title, String message);
    }

    private NotificationListener notificationListener;
    private RequestsPagerAdapter requestsPagerAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationListener) {
            notificationListener = (NotificationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NotificationListener");
        }
    }

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FragmentRequestsBinding binding;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRequestsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return root;
        }

        requestsPagerAdapter = new RequestsPagerAdapter(this);
        binding.viewPager.setAdapter(requestsPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("To Me");
                            break;
                        case 1:
                            tab.setText("From Me");
                            break;
                    }
                }).attach();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        // TODO: Hook real limit check; for now method exists to show upgrade prompt
        setupUpgradeEntryPoint();

        return root;
    }

    @Override
    public void onApproveClicked(LocationRequest request) {
        approveRequestWithLocation(request);
    }

    @Override
    public void onRejectClicked(LocationRequest request) {
        updateRequestStatus(request, "rejected");
    }

    @Override
    public void onRequestAgainClicked(LocationRequest request) {
        // Implement request again logic here
    }

    @Override
    public void onViewLocationClicked(LocationRequest request) {
        LocationDetailsBottomSheetFragment.newInstance(request).show(getChildFragmentManager(), "LocationDetailsBottomSheetFragment");
    }

    private void updateRequestStatus(LocationRequest request, String status) {
        FirebaseFirestore.getInstance().collection("locationRequests").document(request.getRequestId())
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status, Toast.LENGTH_SHORT).show();
                    // Find the ToMeRequestsFragment and tell it to remove the card
                    for (Fragment fragment : getChildFragmentManager().getFragments()) {
                        if (fragment instanceof ToMeRequestsFragment && fragment.isAdded()) {
                            ((ToMeRequestsFragment) fragment).removeRequest(request);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update request", Toast.LENGTH_SHORT).show();
                });
    }

    private float distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (R * c);
    }

    private void requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Location needed")
                    .setMessage("Location is required to share your position and compute distance.")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    private void approveRequestWithLocation(LocationRequest request) {
        // Always set status and approvedTimestamp; enrich with location if permission is granted.
        long approvedTs = System.currentTimeMillis();

        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            pendingApprovalRequest = request;
            requestLocationPermission();
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());
        fusedClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    double lat = 0.0;
                    double lon = 0.0;
                    String areaName = "";
                    if (loc != null) {
                        lat = loc.getLatitude();
                        lon = loc.getLongitude();
                        try {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address addr = addresses.get(0);
                                String locality = addr.getLocality();
                                String admin = addr.getAdminArea();
                                String country = addr.getCountryName();
                                StringBuilder sb = new StringBuilder();
                                if (locality != null && !locality.isEmpty()) sb.append(locality);
                                if (admin != null && !admin.isEmpty()) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(admin);
                                }
                                if (country != null && !country.isEmpty()) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(country);
                                }
                                areaName = sb.toString();
                            }
                        } catch (IOException ignored) {
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "approved");
                    updates.put("approvedTimestamp", approvedTs);
                    if (loc != null) {
                        updates.put("latitude", lat);
                        updates.put("longitude", lon);
                        updates.put("areaName", areaName);
                        // compute distance from requestor (if we have their coords) to approver
                        if (request.getLatitude() != 0.0 || request.getLongitude() != 0.0) {
                            float d = distanceKm(request.getLatitude(), request.getLongitude(), lat, lon);
                            updates.put("distance", d);
                        }
                    }

                    FirebaseFirestore.getInstance().collection("locationRequests").document(request.getRequestId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Request approved", Toast.LENGTH_SHORT).show();
                                for (Fragment fragment : getChildFragmentManager().getFragments()) {
                                    if (fragment instanceof ToMeRequestsFragment && fragment.isAdded()) {
                                        ((ToMeRequestsFragment) fragment).removeRequest(request);
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to approve request", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    // Fallback: update minimal fields
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "approved");
                    updates.put("approvedTimestamp", approvedTs);
                    FirebaseFirestore.getInstance().collection("locationRequests").document(request.getRequestId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Request approved", Toast.LENGTH_SHORT).show();
                                for (Fragment fragment : getChildFragmentManager().getFragments()) {
                                    if (fragment instanceof ToMeRequestsFragment && fragment.isAdded()) {
                                        ((ToMeRequestsFragment) fragment).removeRequest(request);
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(err -> Toast.makeText(getContext(), "Failed to approve request", Toast.LENGTH_SHORT).show());
                });
    }

    private void setupUpgradeEntryPoint() {
        // Reflect pro status from Firestore; if not pro, show upgrade banner
        if (binding.cardUpgradeBanner != null) {
            binding.cardUpgradeBanner.setVisibility(View.GONE);
        }
        if (binding.btnUpgradeNow != null) {
            binding.btnUpgradeNow.setOnClickListener(v -> {
                try {
                    startActivity(new android.content.Intent(requireContext(), com.whereu.whereu.activities.PlansActivity.class));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open upgrade", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        boolean isPro = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("isPro"));
                        if (binding.cardUpgradeBanner != null) {
                            binding.cardUpgradeBanner.setVisibility(isPro ? View.GONE : View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to local preference check
                        boolean localPro = com.whereu.whereu.activities.PlansActivity.isProUser(requireContext());
                        if (binding.cardUpgradeBanner != null) {
                            binding.cardUpgradeBanner.setVisibility(localPro ? View.GONE : View.VISIBLE);
                        }
                    });
        }
    }

    private LocationRequest pendingApprovalRequest; // hold request while asking for permission

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && pendingApprovalRequest != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                approveRequestWithLocation(pendingApprovalRequest);
            } else {
                Toast.makeText(getContext(), "Permission denied. Cannot share location.", Toast.LENGTH_SHORT).show();
            }
            pendingApprovalRequest = null;
        }
    }
}
