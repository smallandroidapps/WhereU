package com.whereu.whereu.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class RequestDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_LOCATION_REQUEST = "location_request";

    private LocationRequest locationRequest;
    private OnRequestDetailsActionListener listener;
    private FusedLocationProviderClient fusedLocationClient;

    public interface OnRequestDetailsActionListener {
        void onRequestApproved(LocationRequest request);
        void onRequestRejected(LocationRequest request);
    }

    public static RequestDetailsBottomSheetFragment newInstance(LocationRequest request) {
        RequestDetailsBottomSheetFragment fragment = new RequestDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LOCATION_REQUEST, request);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationRequest = getArguments().getParcelable(ARG_LOCATION_REQUEST);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_request_details, container, false);

        ImageView avatarView = view.findViewById(R.id.detail_user_avatar);
        TextView userNameTextView = view.findViewById(R.id.detail_user_name);
        TextView coordinatesTextView = view.findViewById(R.id.detail_coordinates);
        TextView timestampTextView = view.findViewById(R.id.detail_timestamp);
        TextView distanceTextView = view.findViewById(R.id.detail_distance);
        TextView areaNameTextView = view.findViewById(R.id.detail_area_name);
        Button openInMapsButton = view.findViewById(R.id.open_in_maps_button);
        LinearLayout actionButtonsLayout = view.findViewById(R.id.action_buttons_layout);
        Button approveButton = view.findViewById(R.id.approve_button);
        Button rejectButton = view.findViewById(R.id.reject_button);

        if (locationRequest != null) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            boolean isReceiver = currentUserId.equals(locationRequest.getToUserId());

            userNameTextView.setText(String.format("User: %s", locationRequest.getUserName()));
            coordinatesTextView.setText(String.format(Locale.getDefault(), "Coordinates: %.4f, %.4f", locationRequest.getLatitude(), locationRequest.getLongitude()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            timestampTextView.setText(String.format("Timestamp: %s", sdf.format(locationRequest.getApprovedTimestamp() != 0 ? locationRequest.getApprovedTimestamp() : locationRequest.getTimestamp())));

            distanceTextView.setText("Distance: Calculating...");
            updateDistance(distanceTextView);
            distanceTextView.setVisibility(View.VISIBLE);
            areaNameTextView.setText(String.format("Area: %s", locationRequest.getAreaName() != null ? locationRequest.getAreaName() : "N/A"));

            // Load avatar for the other user in the request
            String otherUserId = isReceiver ? locationRequest.getFromUserId() : locationRequest.getToUserId();
            if (avatarView != null && otherUserId != null && !otherUserId.isEmpty()) {
                FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    Glide.with(this)
                                            .load(user.getProfilePhotoUrl())
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .error(R.drawable.ic_profile_placeholder)
                                            .into(avatarView);
                                }
                            }
                        });
            }

            openInMapsButton.setOnClickListener(v -> {
                String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", locationRequest.getLatitude(), locationRequest.getLongitude(), locationRequest.getLatitude(), locationRequest.getLongitude());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(getContext(), "Google Maps app not installed.", Toast.LENGTH_SHORT).show();
                }
            });

            if (isReceiver && "pending".equals(locationRequest.getStatus())) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
                approveButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRequestApproved(locationRequest);
                        dismiss();
                    }
                });
                rejectButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRequestRejected(locationRequest);
                        dismiss();
                    }
                });
            } else {
                actionButtonsLayout.setVisibility(View.GONE);
            }

        } else {
            Toast.makeText(getContext(), "Error: Location request details not available.", Toast.LENGTH_SHORT).show();
            dismiss();
        }

        return view;
    }

    private void updateDistance(TextView distanceTextView) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            distanceTextView.setText("Distance: Location permission needed");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null && locationRequest != null) {
                float distance = distanceKm(location.getLatitude(), location.getLongitude(),
                        locationRequest.getLatitude(), locationRequest.getLongitude());
                distanceTextView.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distance));
            } else {
                distanceTextView.setText("Distance: N/A");
            }
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

    public void setOnRequestDetailsActionListener(OnRequestDetailsActionListener listener) {
        this.listener = listener;
    }
}
