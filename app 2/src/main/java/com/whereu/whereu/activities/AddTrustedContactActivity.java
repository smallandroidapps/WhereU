package com.whereu.whereu.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.models.TrustedContact;
import com.whereu.whereu.utils.NotificationHelper;

import java.util.HashMap;
import java.util.Map;

public class AddTrustedContactActivity extends AppCompatActivity {

    private EditText editTextDisplayName;
    private EditText editTextPhoneNumber;
    private Button buttonSaveContact;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trusted_contact);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextPhoneNumber = findViewById(R.id.edit_text_phone_number);
        buttonSaveContact = findViewById(R.id.button_save_contact);

        buttonSaveContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrustedContact();
            }
        });
    }

    private void saveTrustedContact() {
        String displayName = editTextDisplayName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (displayName.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter display name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // For simplicity, we'll use the phone number as the document ID for now.
            // In a real application, you might want to generate a unique ID or use a contact's UID if they are also users.
            String contactId = phoneNumber;

            TrustedContact newContact = new TrustedContact(contactId, displayName, phoneNumber);

            db.collection("users").document(userId)
                    .collection("trustedContacts").document(contactId)
                    .set(newContact)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddTrustedContactActivity.this, "Trusted contact added", Toast.LENGTH_SHORT).show();
                        NotificationHelper.sendLocalNotification(AddTrustedContactActivity.this, "New Trusted Contact", displayName + " has been added to your trusted contacts.");
                        finish(); // Close the activity after saving
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddTrustedContactActivity.this, "Error adding contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}