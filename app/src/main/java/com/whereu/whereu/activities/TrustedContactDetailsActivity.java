package com.whereu.whereu.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.models.TrustedContact;

public class TrustedContactDetailsActivity extends AppCompatActivity {

    private TextView trustedContactName;
    private TextView trustedContactPhone;
    private Button buttonRequestLocation;
    private Button buttonRemoveContact;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TrustedContact trustedContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_contact_details);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        trustedContactName = findViewById(R.id.trusted_contact_name);
        trustedContactPhone = findViewById(R.id.trusted_contact_phone);
        buttonRequestLocation = findViewById(R.id.button_request_location);
        buttonRemoveContact = findViewById(R.id.button_remove_contact);

        trustedContact = (TrustedContact) getIntent().getSerializableExtra("trustedContact");

        if (trustedContact != null) {
            trustedContactName.setText(trustedContact.getDisplayName());
            trustedContactPhone.setText(trustedContact.getPhoneNumber());
        }

        buttonRequestLocation.setOnClickListener(v -> sendLocationRequest());
        buttonRemoveContact.setOnClickListener(v -> removeTrustedContact());
    }

    private void sendLocationRequest() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && trustedContact != null) {
            String senderId = currentUser.getUid();
            String senderName = currentUser.getDisplayName();
            String receiverId = trustedContact.getUid();
            String receiverName = trustedContact.getDisplayName();

            com.whereu.whereu.models.LocationRequest newRequest = new com.whereu.whereu.models.LocationRequest(
                    senderId, senderName, receiverId, receiverName, trustedContact.getPhoneNumber(), trustedContact.getProfilePhotoUrl(), System.currentTimeMillis(), "pending", 0.0, 0.0, "");

            db.collection("locationRequests").add(newRequest)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(TrustedContactDetailsActivity.this, "Location request sent to " + trustedContact.getDisplayName(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TrustedContactDetailsActivity.this, "Failed to send location request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in or contact not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeTrustedContact() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && trustedContact != null) {
            db.collection("users").document(currentUser.getUid())
                    .collection("trustedContacts").document(trustedContact.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(TrustedContactDetailsActivity.this, "Trusted contact removed", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TrustedContactDetailsActivity.this, "Error removing contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in or contact not found", Toast.LENGTH_SHORT).show();
        }
    }
}