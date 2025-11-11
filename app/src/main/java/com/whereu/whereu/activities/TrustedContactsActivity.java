package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.adapters.TrustedContactAdapter;
import com.whereu.whereu.models.LocationRequest;
import com.whereu.whereu.models.TrustedContact;

import java.util.ArrayList;
import java.util.List;

public class TrustedContactsActivity extends AppCompatActivity implements TrustedContactAdapter.OnActionButtonClickListener {

    private static final String TAG = "TrustedContactsActivity";

    private RecyclerView recyclerView;
    private TrustedContactAdapter adapter;
    private List<TrustedContact> trustedContactList;
    private FloatingActionButton fabAddTrustedContact;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contacts);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_view_trusted_contacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        trustedContactList = new ArrayList<>();
        adapter = new TrustedContactAdapter(trustedContactList, this);
        recyclerView.setAdapter(adapter);

        fabAddTrustedContact = findViewById(R.id.fab_add_trusted_contact);
        fabAddTrustedContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isPro = com.whereu.whereu.activities.PlansActivity.isProUser(TrustedContactsActivity.this);
                if (!isPro) {
                    Toast.makeText(TrustedContactsActivity.this, "Upgrade to Pro to add trusted contacts", Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(TrustedContactsActivity.this, com.whereu.whereu.activities.PlansActivity.class));
                    } catch (Exception ignored) {}
                    return;
                }
                Intent intent = new Intent(TrustedContactsActivity.this, AddTrustedContactActivity.class);
                startActivity(intent);
            }
        });

        loadTrustedContacts();
    }

    private void loadTrustedContacts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user is not null: " + currentUser.getUid());
            db.collection("users").document(currentUser.getUid())
                    .collection("trustedContacts")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Listen failed.", error);
                            return;
                        }

                        if (value == null) {
                            Log.d(TAG, "Snapshot value is null.");
                            return;
                        }

                        Log.d(TAG, "Snapshot value received. Number of documents: " + value.size());
                        trustedContactList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            TrustedContact contact = doc.toObject(TrustedContact.class);
                            if (contact != null) {
                                trustedContactList.add(contact);
                                Log.d(TAG, "Added trusted contact: " + contact.getDisplayName());
                            } else {
                                Log.w(TAG, "Failed to convert document to TrustedContact: " + doc.getId());
                            }
                        }
                        Log.d(TAG, "Trusted contacts loaded: " + trustedContactList.size());
                        adapter.notifyDataSetChanged();
                    });
        } else {
            Log.d(TAG, "No current user, cannot load trusted contacts.");
        }
    }

    @Override
    public void onActionButtonClick(TrustedContact contact, String action) {
        Log.d(TAG, "Action: " + action + " for contact: " + contact.getDisplayName());
        switch (action) {
            case "Request":
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    LocationRequest newRequest = new LocationRequest(currentUser.getUid(), contact.getUid());

                    db.collection("locationRequests").add(newRequest)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(TrustedContactsActivity.this, "Location request sent to " + contact.getDisplayName(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TrustedContactsActivity.this, "Failed to send location request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(TrustedContactsActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                }
                break;
            case "View Details":
                Intent intent = new Intent(TrustedContactsActivity.this, TrustedContactDetailsActivity.class);
                intent.putExtra("trustedContact", contact);
                startActivity(intent);
                break;
            case "Approve":
                // Handle approve action
                break;
            case "Deny":
                // Handle deny action
                break;
            case "Request Again":
                // Enforce cooldowns per last status: 1 hour if rejected, else 1 minute
                FirebaseUser currentUser2 = mAuth.getCurrentUser();
                if (currentUser2 == null) {
                    Toast.makeText(TrustedContactsActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                    break;
                }
                String fromId = currentUser2.getUid();
                String toId = contact.getUid();
                // Query latest request between these two users
                db.collection("locationRequests")
                        .whereEqualTo("fromUserId", fromId)
                        .whereEqualTo("toUserId", toId)
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            long now = System.currentTimeMillis();
                            boolean blocked = false;
                            long remaining = 0L;
                            if (!querySnapshot.isEmpty()) {
                                com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                LocationRequest lastReq = doc.toObject(LocationRequest.class);
                                if (lastReq != null) {
                                    // Block for 1 hour if rejected; else block for 1 minute
                                    if ("rejected".equals(lastReq.getStatus())) {
                                        long basisTs = lastReq.getRejectedTimestamp() > 0 ? lastReq.getRejectedTimestamp() : lastReq.getTimestamp();
                                        long cooldownMs = 60L * 60L * 1000L;
                                        remaining = (basisTs + cooldownMs) - now;
                                        blocked = remaining > 0;
                                    } else {
                                        long basisTs = lastReq.getTimestamp();
                                        long cooldownMs = 60L * 1000L;
                                        remaining = (basisTs + cooldownMs) - now;
                                        blocked = remaining > 0;
                                    }
                                }
                            }

                            if (blocked) {
                                Toast.makeText(TrustedContactsActivity.this, "Please wait " + SearchResultAdapter.SearchResult.formatCooldownTime(remaining) + " before requesting again.", Toast.LENGTH_SHORT).show();
                            } else {
                                LocationRequest newReq = new LocationRequest(fromId, toId);
                                db.collection("locationRequests").add(newReq)
                                        .addOnSuccessListener(documentReference -> {
                                            Toast.makeText(TrustedContactsActivity.this, "Location request sent to " + contact.getDisplayName(), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(TrustedContactsActivity.this, "Failed to send location request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(TrustedContactsActivity.this, "Failed to check cooldown: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                break;
        }
    }
}
