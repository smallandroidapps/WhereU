package com.whereu.whereu.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.whereu.whereu.R;

public class LocationDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnLocationDetailActionListener {
        void onViewOnMapClick(String coordinates);
        void onRequestAgainClick(String targetUserId);
    }

    private OnLocationDetailActionListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnLocationDetailActionListener) {
            listener = (OnLocationDetailActionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLocationDetailActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private static final String ARG_NAME = "name";
    private static final String ARG_SHARED_AT = "sharedAt";
    private static final String ARG_AREA_NAME = "areaName";
    private static final String ARG_COORDINATES = "coordinates";
    private static final String ARG_DISTANCE = "distance";
    private static final String ARG_STATUS = "status";
    private static final String ARG_EXPIRES_IN = "expiresIn";

    public static LocationDetailsBottomSheetFragment newInstance(String name, String sharedAt, String areaName, String coordinates, String distance, String status, String expiresIn) {
        LocationDetailsBottomSheetFragment fragment = new LocationDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_SHARED_AT, sharedAt);
        args.putString(ARG_AREA_NAME, areaName);
        args.putString(ARG_COORDINATES, coordinates);
        args.putString(ARG_DISTANCE, distance);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_EXPIRES_IN, expiresIn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_location_details, container, false);

        TextView tvName = view.findViewById(R.id.text_name);
        TextView tvSharedAt = view.findViewById(R.id.text_shared_at);
        TextView tvAreaName = view.findViewById(R.id.text_area_name);
        TextView tvCoordinates = view.findViewById(R.id.text_coordinates);
        TextView tvDistance = view.findViewById(R.id.text_distance);
        TextView tvStatus = view.findViewById(R.id.text_status);
        TextView tvExpiresIn = view.findViewById(R.id.text_expires_in);
        Button btnViewOnMap = view.findViewById(R.id.button_view_on_map);
        Button btnRequestAgain = view.findViewById(R.id.button_request_again);
        Button btnClose = view.findViewById(R.id.button_close);

        if (getArguments() != null) {
            String coordinates = getArguments().getString(ARG_COORDINATES);
            String targetUserId = getArguments().getString(ARG_NAME); // Assuming name is targetUserId for now

            tvName.setText(getArguments().getString(ARG_NAME));
            tvSharedAt.setText(String.format("Shared At: %s", getArguments().getString(ARG_SHARED_AT)));
            tvAreaName.setText(String.format("Area Name: %s", getArguments().getString(ARG_AREA_NAME)));
            tvCoordinates.setText(String.format("Coordinates: %s", coordinates));
            tvDistance.setText(String.format("Distance from Me: %s", getArguments().getString(ARG_DISTANCE)));
            tvStatus.setText(String.format("Status: %s", getArguments().getString(ARG_STATUS)));
            tvExpiresIn.setText(String.format("Expires In: %s", getArguments().getString(ARG_EXPIRES_IN)));

            btnViewOnMap.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewOnMapClick(coordinates);
                    dismiss();
                }
            });

            btnRequestAgain.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRequestAgainClick(targetUserId);
                    dismiss();
                }
            });
        }

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }
}