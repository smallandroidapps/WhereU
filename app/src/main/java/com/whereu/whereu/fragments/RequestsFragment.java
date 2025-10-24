package com.whereu.whereu.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.R;
import com.wheru.adapters.RequestAdapter;
import com.wheru.models.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<Request> requestList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(requestList);
        recyclerView.setAdapter(requestAdapter);

        fetchRequests();

        return view;
    }

    private void fetchRequests() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in state
            addDummyRequests();
            return;
        }

        String currentUserId = currentUser.getUid();

        Task<QuerySnapshot> incomingRequestsTask = db.collection("requests")
                .whereEqualTo("receiverId", currentUserId)
                .get();

        Task<QuerySnapshot> outgoingRequestsTask = db.collection("requests")
                .whereEqualTo("senderId", currentUserId)
                .get();

        Tasks.whenAllSuccess(incomingRequestsTask, outgoingRequestsTask).addOnSuccessListener(list -> {
            requestList.clear();
            for (Object snapshot : list) {
                for (QueryDocumentSnapshot document : (QuerySnapshot) snapshot) {
                    Request request = document.toObject(Request.class);
                    requestList.add(request);
                }
            }

            if (requestList.isEmpty()) {
                addDummyRequests();
            }
            requestAdapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Log.e("RequestsFragment", "Error fetching requests", e);
            addDummyRequests();
        });
    }

    private void addDummyRequests() {
        requestList.add(new Request("1", "dummySenderId1", "dummyReceiverId1", "John Doe", Request.RequestStatus.PENDING, new Date()));
        requestList.add(new Request("2", "dummySenderId2", "dummyReceiverId2", "Jane Smith", Request.RequestStatus.ACCEPTED, new Date()));
        requestAdapter.notifyDataSetChanged();
    }
}