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
        try {
            mListener = (OnLocationDetailActionListener) context;
        } catch (ClassCastException e) {
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
            name.setText(locationRequest.getReceiverName());
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, MMM dd, yyyy", Locale.getDefault());
            if(locationRequest.getApprovedAt() != null) {
                sharedAt.setText("Shared At: " + sdf.format(locationRequest.getApprovedAt()));
            }
            areaName.setText("Area Name: " + locationRequest.getAreaName());
            coordinates.setText("Coordinates: " + locationRequest.getLatitude() + ", " + locationRequest.getLongitude());
            status.setText("Status: " + locationRequest.getStatus());

            if(locationRequest.getExpiresAt() != null) {
                long timeRemaining = locationRequest.getExpiresAt().getTime() - System.currentTimeMillis();
                long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining);
                expiresIn.setText("Expires In: " + hours + "h left");
            }

            distance.setText("Distance: -- km");
        }

        viewOnMap.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onViewOnMapClick(locationRequest.getLatitude(), locationRequest.getLongitude());
            }
            dismiss();
        });

        requestAgain.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onRequestAgainClick(locationRequest.getReceiverId());
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
