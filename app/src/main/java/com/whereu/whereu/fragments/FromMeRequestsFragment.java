package com.whereu.whereu.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.databinding.FragmentFromMeRequestsBinding;
import com.whereu.whereu.models.LocationRequest;

import java.util.ArrayList;
import java.util.List;

public class FromMeRequestsFragment extends Fragment {

    private FragmentFromMeRequestsBinding binding;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFromMeRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(requestList, (RequestAdapter.OnRequestActionListener) getParentFragment());
        binding.recyclerViewFromMe.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFromMe.setAdapter(requestAdapter);

        fetchRequests();
    }

    public void fetchRequests() {
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            db.collection("locationRequests")
                    .whereEqualTo("fromUserId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            requestList.clear();
                            for (LocationRequest request : task.getResult().toObjects(LocationRequest.class)) {
                                requestList.add(request);
                            }
                            requestAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}