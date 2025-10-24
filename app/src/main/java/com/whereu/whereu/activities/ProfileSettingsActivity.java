package com.whereu.whereu.activities;

import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.wheru.models.User;

import java.util.HashMap;
import java.util.Map;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettingsActivity";

    private ImageView imageViewProfilePicture;
    private EditText editTextDisplayName;
    private EditText editTextPhoneNumber;
    private EditText editTextEmail;
    private Button buttonSaveProfile;
    private Switch switchHideLocation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        imageViewProfilePicture = findViewById(R.id.image_profile_picture);
        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextPhoneNumber = findViewById(R.id.edit_text_phone_number);
        editTextEmail = findViewById(R.id.edit_text_email);
        buttonSaveProfile = findViewById(R.id.button_save_profile);
        switchHideLocation = findViewById(R.id.switch_hide_location);

        loadUserProfile();

        buttonSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    private void loadUserProfile() {
        Log.d(TAG, "loadUserProfile called.");
        if (currentUser == null) {
            Log.d(TAG, "currentUser is null. User not logged in.");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "currentUser is not null. User ID: " + currentUser.getUid());
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + userId);
            String userEmail = currentUser.getEmail(); // Get email from FirebaseUser
            if (userEmail != null) {
                editTextEmail.setText(userEmail);
            }
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "User profile exists in Firestore.");
                            String displayName = documentSnapshot.getString("displayName");
                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            String email = documentSnapshot.getString("email"); // Retrieve email from Firestore
                            String profilePhotoUrl = documentSnapshot.getString("profilePhotoUrl");
                            Boolean hideLocation = documentSnapshot.getBoolean("hideLocation");

                            editTextDisplayName.setText(displayName);
                            editTextPhoneNumber.setText(phoneNumber);
                            editTextEmail.setText(email); // Set email to EditText

                            if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty()) {
                                Glide.with(this).load(profilePhotoUrl).into(imageViewProfilePicture);
                            }

                            if (hideLocation != null) {
                                switchHideLocation.setChecked(hideLocation);
                            }
                        } else {
                            Log.d(TAG, "User profile does not exist. Creating default profile.");
                            // Profile not found, create a new one
                            Map<String, Object> newUserProfile = new HashMap<>();
                            newUserProfile.put("displayName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
                            newUserProfile.put("phoneNumber", currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
                            newUserProfile.put("email", currentUser.getEmail() != null ? currentUser.getEmail() : ""); // Add email to new profile
                            newUserProfile.put("profilePhotoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
                            newUserProfile.put("hideLocation", false); // Default to not hiding location

                            db.collection("users").document(userId).set(newUserProfile)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Default user profile created successfully.");
                                        // Now load the newly created profile
                                        loadUserProfile();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error creating default user profile", e);
                                        Toast.makeText(ProfileSettingsActivity.this, "Error creating profile", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading user profile", e);
                        Toast.makeText(ProfileSettingsActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveUserProfile() {
        Log.d(TAG, "saveUserProfile called.");
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String displayName = editTextDisplayName.getText().toString().trim();
            String phoneNumber = editTextPhoneNumber.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            boolean hideLocation = switchHideLocation.isChecked();

            Map<String, Object> user = new HashMap<>();
            user.put("displayName", displayName);
            user.put("phoneNumber", phoneNumber);
            user.put("hideLocation", hideLocation);
            Log.d(TAG, "Saving user profile for ID: " + userId + ", data: " + user.toString());

            // Update email in Firebase Authentication if it has changed
            if (!email.isEmpty() && currentUser.getEmail() != null && !currentUser.getEmail().equals(email)) {
                currentUser.updateEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User email address updated.");
                            } else {
                                Log.e(TAG, "Error updating user email address.", task.getException());
                                Toast.makeText(ProfileSettingsActivity.this, "Error updating email", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            db.collection("users").document(userId)
                    .update(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileSettingsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating user profile", e);
                        Toast.makeText(ProfileSettingsActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}