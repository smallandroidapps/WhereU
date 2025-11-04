package com.whereu.whereu.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whereu.whereu.adapters.RequestsPagerAdapter;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.databinding.FragmentRequestsBinding;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    public interface NotificationListener {
        void onSendLocalNotification(String title, String message);
    }

    private NotificationListener notificationListener;

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

        RequestsPagerAdapter requestsPagerAdapter = new RequestsPagerAdapter(this);
        binding.viewPager.setAdapter(requestsPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Pending");
                            break;
                        case 1:
                            tab.setText("From Me");
                            break;
                        case 2:
                            tab.setText("To Me");
                            break;
                    }
                }).attach();

        // Refresh current fragment when swiping
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            int currentPos = binding.viewPager.getCurrentItem();
            binding.viewPager.setCurrentItem(currentPos, false);
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        return root;
    }

    @Override
    public void onApproveClicked(LocationRequest request) {
        updateRequestStatus(request, "approved");
    }

    @Override
    public void onRejectClicked(LocationRequest request) {
        updateRequestStatus(request, "rejected");
    }

    @Override
    public void onRequestAgainClicked(LocationRequest request) {
        // Implement request again logic here
    }

    private void updateRequestStatus(LocationRequest request, String status) {
        // Update request status in Firestore
    }
}