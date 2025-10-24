package com.whereu.whereu.activities;

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
import com.whereu.whereu.models.LocationRequest;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocationDetailsBottomSheet extends BottomSheetDialogFragment {

    private LocationRequest locationRequest;

    public static LocationDetailsBottomSheet newInstance(LocationRequest locationRequest) {
        LocationDetailsBottomSheet fragment = new LocationDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("locationRequest", locationRequest);
        fragment.setArguments(args);
        return fragment;
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
            name.setText(locationRequest.getReceiverId()); // In a real app, you would fetch the user's name from the ID
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.getDefault());
            sharedAt.setText("Shared At: " + sdf.format(locationRequest.getApprovedAt()));
            areaName.setText("Area Name: " + locationRequest.getAreaName());
            coordinates.setText("Coordinates: " + locationRequest.getLatitude() + ", " + locationRequest.getLongitude());
            status.setText("Status: " + locationRequest.getStatus());

            long timeRemaining = locationRequest.getExpiresAt().getTime() - System.currentTimeMillis();
            long hours = timeRemaining / (1000 * 60 * 60);
            expiresIn.setText("Expires In: " + hours + "h left");

            // In a real app, you would calculate the distance from the user's current location
            distance.setText("Distance: -- km");
        }

        viewOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.putExtra("latitude", locationRequest.getLatitude());
            intent.putExtra("longitude", locationRequest.getLongitude());
            startActivity(intent);
        });

        requestAgain.setOnClickListener(v -> {
            // In a real app, you would create a new request in Firestore
            dismiss();
        });

        close.setOnClickListener(v -> dismiss());
    }
}
