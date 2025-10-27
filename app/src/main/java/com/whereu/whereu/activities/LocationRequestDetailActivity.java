package com.whereu.whereu.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.whereu.whereu.R;
import com.whereu.whereu.models.LocationRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationRequestDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_request_detail);

        LocationRequest locationRequest = getIntent().getParcelableExtra("locationRequest");

        if (locationRequest != null) {
            TextView requestIdTextView = findViewById(R.id.text_request_id);
            TextView senderIdTextView = findViewById(R.id.text_sender_id);
            TextView receiverIdTextView = findViewById(R.id.text_receiver_id);
            TextView statusTextView = findViewById(R.id.text_status);
            TextView timestampTextView = findViewById(R.id.text_timestamp);
            TextView latitudeTextView = findViewById(R.id.text_latitude);
            TextView longitudeTextView = findViewById(R.id.text_longitude);
            TextView areaNameTextView = findViewById(R.id.text_area_name);
            TextView distanceTextView = findViewById(R.id.text_distance);

            requestIdTextView.setText(String.format("Request ID: %s", locationRequest.getRequestId()));
            senderIdTextView.setText(String.format("Sender ID: %s", locationRequest.getFromUserId()));
            receiverIdTextView.setText(String.format("Receiver ID: %s", locationRequest.getToUserId()));
            statusTextView.setText(String.format("Status: %s", locationRequest.getStatus()));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedTimestamp = sdf.format(new Date(locationRequest.getTimestamp()));
            timestampTextView.setText(String.format("Timestamp: %s", formattedTimestamp));

            latitudeTextView.setText(String.format("Latitude: %.4f", locationRequest.getLatitude()));
            longitudeTextView.setText(String.format("Longitude: %.4f", locationRequest.getLongitude()));
            areaNameTextView.setText(String.format("Area Name: %s", locationRequest.getAreaName()));
            distanceTextView.setText(String.format("Distance: %.2f km", locationRequest.getDistance()));
        }
    }
}