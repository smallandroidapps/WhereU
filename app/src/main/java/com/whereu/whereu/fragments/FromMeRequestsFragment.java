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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.whereu.whereu.adapters.RequestAdapter;
import com.whereu.whereu.databinding.FragmentFromMeRequestsBinding;
import com.whereu.whereu.models.LocationRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FromMeRequestsFragment extends Fragment {

    private static final String TAG = "FromMeRequestsFragment";
    private FragmentFromMeRequestsBinding binding;
    private RequestAdapter requestAdapter;
    private List<LocationRequest> requestList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration requestListener;

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
        requestAdapter = new RequestAdapter(getContext(), requestList, (RequestAdapter.OnRequestActionListener) getParentFragment());
        binding.recyclerViewFromMe.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewFromMe.setAdapter(requestAdapter);

        listenForRequests();
    }

    private void listenForRequests() {
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            Query query = db.collection("locationRequests")
                    .whereEqualTo("fromUserId", currentUserId);

            requestListener = query.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error fetching requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                if (snapshots != null) {
                    Map<String, LocationRequest> latestByToUserId = new HashMap<>();
                    Set<String> toUserIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        LocationRequest request = doc.toObject(LocationRequest.class);
                        request.setRequestId(doc.getId());

                        String toUserId = request.getToUserId();
                        toUserIds.add(toUserId);
                        if (!latestByToUserId.containsKey(toUserId) || request.getTimestamp() > latestByToUserId.get(toUserId).getTimestamp()) {
                            latestByToUserId.put(toUserId, request);
                        }
                    }

                    List<String> idList = new ArrayList<>(toUserIds);
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
                                key = userId;
                            }

                            LocationRequest req = latestByToUserId.get(userId);
                            if (req == null) continue;

                            LocationRequest existing = dedupByEmailMobile.get(key);
                            if (existing == null || effectiveTs(req) > effectiveTs(existing)) {
                                dedupByEmailMobile.put(key, req);
                            }
                        }

                        List<LocationRequest> uniqueRequests = new ArrayList<>(dedupByEmailMobile.values());
                        Collections.sort(uniqueRequests, (o1, o2) -> Long.compare(effectiveTs(o2), effectiveTs(o1)));

                        requestList.clear();
                        requestList.addAll(uniqueRequests);
                        requestAdapter.notifyDataSetChanged();
                    });
                }
            });
        }
    }

    private long effectiveTs(LocationRequest r) {
        if (r == null) return 0L;
        return r.getApprovedTimestamp() > 0 ? r.getApprovedTimestamp() : r.getTimestamp();
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
