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
                // Handle request again action
                break;
        }
    }
}