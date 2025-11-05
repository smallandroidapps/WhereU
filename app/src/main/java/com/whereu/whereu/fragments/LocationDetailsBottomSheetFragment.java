package com.whereu.whereu.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.MapActivity;
import com.whereu.whereu.models.LocationRequest;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LocationDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnLocationDetailActionListener {
        void onViewOnMapClick(double latitude, double longitude);
        void onRequestAgainClick(String receiverId);
    }

    private OnLocationDetailActionListener mListener;
    private LocationRequest locationRequest;

    public static LocationDetailsBottomSheetFragment newInstance(LocationRequest locationRequest) {
        LocationDetailsBottomSheetFragment fragment = new LocationDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putParcelable("locationRequest", locationRequest);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnLocationDetailActionListener) {
            mListener = (OnLocationDetailActionListener) context;
        } else if (getParentFragment() instanceof OnLocationDetailActionListener) {
            mListener = (OnLocationDetailActionListener) getParentFragment();
        } else {
            throw new ClassCastException(context.toString() + " must implement OnLocationDetailActionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationRequest = getArguments().getParcelable("locationRequest");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_location_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView name = view.findViewById(R.id.text_name);
        TextView sharedAt = view.findViewById(R.id.text_shared_at);
        TextView areaName = view.findViewById(R.id.text_area_name);
        TextView coordinates = view.findViewById(R.id.text_coordinates);
        TextView distance = view.findViewById(R.id.text_distance);
        TextView status = view.findViewById(R.id.text_status);
        TextView expiresIn = view.findViewById(R.id.text_expires_in);
        Button viewOnMap = view.findViewById(R.id.button_view_on_map);
        Button requestAgain = view.findViewById(R.id.button_request_again);
        Button close = view.findViewById(R.id.button_close);

        if (locationRequest != null) {
            name.setText(locationRequest.getUserName());

            // Shared at formatting
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            if (locationRequest.getApprovedTimestamp() != 0) {
                sharedAt.setText("Shared At: " + sdf.format(locationRequest.getApprovedTimestamp()));
            } else {
                sharedAt.setText("Shared At: N/A");
            }

            // Area name (fallback to N/A)
            String area = locationRequest.getAreaName();
            areaName.setText("Area Name: " + (area != null && !area.isEmpty() ? area : "N/A"));

            // Coordinates formatted to 4 decimals
            coordinates.setText(String.format(Locale.getDefault(), "Coordinates: %.4f, %.4f", locationRequest.getLatitude(), locationRequest.getLongitude()));

            // Status
            status.setText("Status: " + locationRequest.getStatus());

            // Expiry formatting (24h from approvedTimestamp). Show Expired if past.
            if (locationRequest.getApprovedTimestamp() > 0) {
                long expiryAt = locationRequest.getApprovedTimestamp() + (24 * 60 * 60 * 1000);
                long timeRemaining = expiryAt - System.currentTimeMillis();
                if (timeRemaining <= 0) {
                    expiresIn.setText("Expires In: Expired");
                } else {
                    long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) - (hours * 60);
                    expiresIn.setText(String.format(Locale.getDefault(), "Expires In: %dh %dm left", hours, minutes));
                }
            } else {
                expiresIn.setText("Expires In: N/A");
            }

            // Distance (km) if available
            double dist = locationRequest.getDistance();
            if (dist > 0) {
                distance.setText(String.format(Locale.getDefault(), "Distance: %.1f km", dist));
            } else {
                distance.setText("Distance: N/A");
            }
        }

        viewOnMap.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onViewOnMapClick(locationRequest.getLatitude(), locationRequest.getLongitude());
            }
            dismiss();
        });

        requestAgain.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRequestAgainClick(locationRequest.getToUserId());
            }
            dismiss();
        });

        close.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
