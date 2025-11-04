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
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.adapters.RequestsPagerAdapter;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.fragments.LocationDetailsBottomSheetFragment;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.databinding.FragmentRequestsBinding;

public class RequestsFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    public interface NotificationListener {
        void onSendLocalNotification(String title, String message);
    }

    private NotificationListener notificationListener;
    private RequestsPagerAdapter requestsPagerAdapter;

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

        requestsPagerAdapter = new RequestsPagerAdapter(this);
        binding.viewPager.setAdapter(requestsPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("To Me");
                            break;
                        case 1:
                            tab.setText("From Me");
                            break;
                    }
                }).attach();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
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

    @Override
    public void onViewLocationClicked(LocationRequest request) {
        LocationDetailsBottomSheetFragment.newInstance(request).show(getChildFragmentManager(), "LocationDetailsBottomSheetFragment");
    }

    private void updateRequestStatus(LocationRequest request, String status) {
        FirebaseFirestore.getInstance().collection("locationRequests").document(request.getRequestId())
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status, Toast.LENGTH_SHORT).show();
                    // Find the ToMeRequestsFragment and tell it to remove the card
                    for (Fragment fragment : getChildFragmentManager().getFragments()) {
                        if (fragment instanceof ToMeRequestsFragment && fragment.isAdded()) {
                            ((ToMeRequestsFragment) fragment).removeRequest(request);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update request", Toast.LENGTH_SHORT).show();
                });
    }
}
