package com.whereu.whereu.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.models.LocationRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.whereu.whereu.databinding.FragmentToMeRequestsBinding;

public class ToMeRequestsFragment extends Fragment {

    private static final String TAG = "ToMeRequestsFragment";
    private FragmentToMeRequestsBinding binding;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration requestListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentToMeRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(getContext(), requestList, (RequestAdapter.OnRequestActionListener) getParentFragment());
        binding.recyclerViewToMe.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewToMe.setAdapter(requestAdapter);

        listenForRequests();
    }

    private void listenForRequests() {
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            Query query = db.collection("locationRequests")
                    .whereEqualTo("toUserId", currentUserId)
                    .whereEqualTo("status", "pending");

            requestListener = query.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error fetching requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                if (snapshots != null) {
                    Map<String, LocationRequest> latestByFromUserId = new HashMap<>();
                    Set<String> fromUserIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        LocationRequest request = doc.toObject(LocationRequest.class);
                        request.setRequestId(doc.getId());

                        String fromUserId = request.getFromUserId();
                        fromUserIds.add(fromUserId);
                        long reqTs = effectiveTs(request);
                        if (!latestByFromUserId.containsKey(fromUserId) || reqTs > effectiveTs(latestByFromUserId.get(fromUserId))) {
                            latestByFromUserId.put(fromUserId, request);
                        }
                    }

                    List<String> idList = new ArrayList<>(fromUserIds);
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    for (String id : idList) {
                        userTasks.add(db.collection("users").document(id).get());
                    }

                    Tasks.whenAllComplete(userTasks).addOnCompleteListener(t -> {
                        Map<String, LocationRequest> dedupByEmailMobile = new HashMap<>();
                        for (int i = 0; i < userTasks.size(); i++) {
                            String userId = idList.get(i);
                            Task<DocumentSnapshot> task = userTasks.get(i);
                            DocumentSnapshot userDoc = task.isSuccessful() ? task.getResult() : null;

                            String key;
                            if (userDoc != null && userDoc.exists()) {
                                String email = userDoc.getString("email");
                                String mobile = userDoc.getString("mobileNumber");
                                boolean hasEmail = email != null && !email.isEmpty();
                                boolean hasMobile = mobile != null && !mobile.isEmpty();
                                key = (hasEmail && hasMobile) ? (email + "|" + mobile) : userId;
                            } else {
                                key = userId; // Fallback if user doc not readable
                            }

                            LocationRequest req = latestByFromUserId.get(userId);
                            if (req == null) continue;

                            LocationRequest existing = dedupByEmailMobile.get(key);
                            if (existing == null || effectiveTs(req) > effectiveTs(existing)) {
                                dedupByEmailMobile.put(key, req);
                            }
                        }

                        List<LocationRequest> pendingRequests = new ArrayList<>(dedupByEmailMobile.values());
                        Collections.sort(pendingRequests, (o1, o2) -> Long.compare(effectiveTs(o2), effectiveTs(o1)));

                        // Final guard: ensure only one entry per sender (fromUserId)
                        List<LocationRequest> finalList = new ArrayList<>();
                        Set<String> seenSenders = new HashSet<>();
                        for (LocationRequest r : pendingRequests) {
                            String senderId = r.getFromUserId();
                            if (senderId == null) continue;
                            if (seenSenders.add(senderId)) {
                                finalList.add(r);
                            }
                        }

                        requestList.clear();
                        requestList.addAll(finalList);
                        requestAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }
            });
        }
    }

    private long effectiveTs(LocationRequest r) {
        if (r == null) return 0L;
        return r.getApprovedTimestamp() > 0 ? r.getApprovedTimestamp() : r.getTimestamp();
    }

    public void removeRequest(LocationRequest requestToRemove) {
        int position = -1;
        for (int i = 0; i < requestList.size(); i++) {
            if (requestList.get(i).getRequestId().equals(requestToRemove.getRequestId())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            requestList.remove(position);
            requestAdapter.notifyItemRemoved(position);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (binding == null || binding.emptyView == null) return;
        if (requestList.isEmpty()) {
            binding.recyclerViewToMe.setVisibility(View.GONE);
            binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerViewToMe.setVisibility(View.VISIBLE);
            binding.emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestListener != null) {
            requestListener.remove();
        }
        binding = null; // Prevent memory leaks
    }
}
