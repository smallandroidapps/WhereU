package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
import com.whereu.whereu.models.TrustedContact;

import java.util.ArrayList;
import java.util.List;

public class TrustedContactsActivity extends AppCompatActivity {

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
        adapter = new TrustedContactAdapter(trustedContactList);
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
            db.collection("users").document(currentUser.getUid())
                    .collection("trustedContacts")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        trustedContactList.clear();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                TrustedContact contact = doc.toObject(TrustedContact.class);
                                if (contact != null) {
                                    trustedContactList.add(contact);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });
        } else {
            Log.d(TAG, "No current user, cannot load trusted contacts.");
        }
    }
}