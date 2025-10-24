package com.whereu.whereu.activities;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocationDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        locationRequest = (LocationRequest) getIntent().getSerializableExtra("locationRequest");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        updateDetailsCard();

        Button requestAgainButton = findViewById(R.id.button_request_again);
        requestAgainButton.setOnClickListener(v -> requestAgain());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (locationRequest != null) {
            LatLng location = new LatLng(locationRequest.getLatitude(), locationRequest.getLongitude());
            mMap.addMarker(new MarkerOptions().position(location).title("Shared Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }

    private void updateDetailsCard() {
        if (locationRequest == null) return;

        TextView sharedBy = findViewById(R.id.text_shared_by);
        TextView sharedAt = findViewById(R.id.text_shared_at);
        TextView areaName = findViewById(R.id.text_area_name);
        TextView distance = findViewById(R.id.text_distance);

        // In a real app, you would fetch the user's name from the ID
        sharedBy.setText("Shared by: " + locationRequest.getReceiverId());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        if (locationRequest.getApprovedAt() != null) {
            sharedAt.setText("Shared at: " + sdf.format(locationRequest.getApprovedAt()));
        }

        areaName.setText("Approx. Area: " + locationRequest.getAreaName());

        // Calculate and display distance if you have the user's current location
        // For now, we'll leave this blank.
        distance.setText("Distance: -- km away");
    }

    private void requestAgain() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("locationRequests").add(new LocationRequest(locationRequest.getSenderId(), locationRequest.getReceiverId(), locationRequest.getReceiverPhoneNumber(), locationRequest.getReceiverProfilePhotoUrl()));
        Toast.makeText(this, "Location request sent again.", Toast.LENGTH_SHORT).show();
        finish();
    }
}