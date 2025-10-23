package com.whereu.whereu.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whereu.whereu.databinding.ActivityNotificationBinding;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ActivityNotificationBinding binding;
    private RecyclerView pastNotificationsRecyclerView;
    private PastNotificationAdapter pastNotificationAdapter;
    private List<NotificationItem> notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize RecyclerView
        pastNotificationsRecyclerView = binding.pastNotificationsRecyclerView;
        pastNotificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        // Add some dummy data
        notificationList.add(new NotificationItem("John Doe requested your location", "Approved"));
        notificationList.add(new NotificationItem("Jane Smith requested your location", "Rejected"));
        notificationList.add(new NotificationItem("Peter Jones requested your location", "Pending"));
        pastNotificationAdapter = new PastNotificationAdapter(notificationList);
        pastNotificationsRecyclerView.setAdapter(pastNotificationAdapter);

        // Handle Approve button click
        binding.buttonApprove.setOnClickListener(view -> {
            // Logic for approving location request
        });

        // Handle Reject button click
        binding.buttonReject.setOnClickListener(view -> {
            // Logic for rejecting location request
        });
    }
}
