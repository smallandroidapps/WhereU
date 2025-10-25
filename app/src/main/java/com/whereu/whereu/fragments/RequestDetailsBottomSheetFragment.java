package com.whereu.whereu.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class RequestDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_LOCATION_REQUEST = "location_request";

    private LocationRequest locationRequest;
    private OnRequestDetailsActionListener listener;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_request_details, container, false);

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

            // Distance will be calculated and set here if available, otherwise a placeholder.
            distanceTextView.setText("Distance: N/A"); // Placeholder for now
            distanceTextView.setVisibility(View.VISIBLE); // Make sure it's visible
            areaNameTextView.setText(String.format("Area: %s", locationRequest.getAreaName() != null ? locationRequest.getAreaName() : "N/A"));

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

            // Show Approve/Reject buttons only for the receiver of a pending request
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
            // Handle case where locationRequest is null
            Toast.makeText(getContext(), "Error: Location request details not available.", Toast.LENGTH_SHORT).show();
            dismiss();
        }

        return view;
    }

    public void setOnRequestDetailsActionListener(OnRequestDetailsActionListener listener) {
        this.listener = listener;
    }
}
