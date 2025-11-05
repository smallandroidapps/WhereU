package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettingsActivity";

    private ImageView imageViewProfilePicture;
    private EditText editTextDisplayName;
    private EditText editTextPhoneNumber;
    private EditText editTextEmail;
    private Button buttonSaveProfile;
    private Switch switchHideLocation;
    private boolean forceMobileUpdate = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String accountType;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        if (getIntent().hasExtra("force_mobile_update")) {
            forceMobileUpdate = getIntent().getBooleanExtra("force_mobile_update", false);
            if (forceMobileUpdate) {
                Toast.makeText(this, "Please update your mobile number to proceed.", Toast.LENGTH_LONG).show();
                // Optionally disable back button or other navigation if needed
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                setFinishOnTouchOutside(false);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        imageViewProfilePicture = findViewById(R.id.image_profile_picture);
        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextPhoneNumber = findViewById(R.id.edit_text_phone_number);
        editTextEmail = findViewById(R.id.edit_text_email);
        buttonSaveProfile = findViewById(R.id.button_save_profile);
        switchHideLocation = findViewById(R.id.switch_hide_location);

        buttonSaveProfile.setOnClickListener(v -> saveUserProfile());

        // Register image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadProfileImage(uri);
            }
        });

        // Tap to change profile photo
        imageViewProfilePicture.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        if (forceMobileUpdate) {
            Toast.makeText(this, "Please update your mobile number to proceed.", Toast.LENGTH_LONG).show();
        }

        loadUserProfile();
    }

    @Override
    public void onBackPressed() {
        if (forceMobileUpdate) {
            // Prevent going back if mobile number update is mandatory
            Toast.makeText(this, "Please update your mobile number before proceeding.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("displayName");
                            String email = documentSnapshot.getString("email");
                            String mobileNumber = documentSnapshot.getString("mobileNumber");
                            Boolean hideLocation = documentSnapshot.getBoolean("hideLocation");
                            String accountType = documentSnapshot.getString("accountType");
                            String profilePhotoUrl = documentSnapshot.getString("profilePhotoUrl");

                            if (name != null) {
                                editTextDisplayName.setText(name);
                            }
                            if (email != null) {
                                editTextEmail.setText(email);
                            }
                            if (mobileNumber != null) {
                                editTextPhoneNumber.setText(mobileNumber);
                            }
                            if (hideLocation != null) {
                                switchHideLocation.setChecked(hideLocation);
                            }
                            if (accountType != null) {
                                this.accountType = accountType;
                            }
                            if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty()) {
                                Glide.with(ProfileSettingsActivity.this)
                                        .load(profilePhotoUrl)
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .into(imageViewProfilePicture);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileSettingsActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveUserProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String displayName = editTextDisplayName.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        boolean hideLocation = switchHideLocation.isChecked();

        if (accountType == null) {
            loadUserProfile();
            Toast.makeText(this, "Profile data not fully loaded, please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate mobile number
        if (phoneNumber.isEmpty()) {
            editTextPhoneNumber.setError("Mobile number cannot be empty");
            editTextPhoneNumber.requestFocus();
            return;
        }

        if (!phoneNumber.matches("^[6-9]\\d{9}$")) {
            editTextPhoneNumber.setError("Invalid Indian mobile number (10 digits, starts with 6-9)");
            editTextPhoneNumber.requestFocus();
            return;
        }

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Toast.makeText(ProfileSettingsActivity.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String originalPhone = documentSnapshot.getString("mobileNumber");

            if (accountType.equals("google") && !phoneNumber.equals(originalPhone)) {
                db.collection("users").whereEqualTo("mobileNumber", phoneNumber).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot existingUserDoc = task.getResult().getDocuments().get(0);
                        String existingAccountType = existingUserDoc.getString("accountType");
                        if ("phone".equals(existingAccountType)) {
                            // Orphaned account, delete it and update the current user
                            db.collection("users").document(existingUserDoc.getId()).delete().addOnSuccessListener(aVoid -> {
                                updateCurrentUserProfile(userId, displayName, phoneNumber, email, hideLocation);
                            });
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "Phone number is already linked to another Google account.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        updateCurrentUserProfile(userId, displayName, phoneNumber, email, hideLocation);
                    }
                });
            } else {
                updateCurrentUserProfile(userId, displayName, phoneNumber, email, hideLocation);
            }
        });
    }

    private void updateCurrentUserProfile(String userId, String displayName, String phoneNumber, String email, boolean hideLocation) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("displayName", displayName);
        userUpdates.put("hideLocation", hideLocation);
        userUpdates.put("mobileNumber", phoneNumber); // Always update mobile number

        if (accountType.equals("phone")) { // Only update email for phone accounts
            userUpdates.put("email", email);
        }

        db.collection("users").document(userId).update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileSettingsActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    if (forceMobileUpdate) {
                        // If forced update, go to HomeActivity after successful save
                        Intent homeIntent = new Intent(ProfileSettingsActivity.this, HomeActivity.class);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(homeIntent);
                    }
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
    }

    private void uploadProfileImage(android.net.Uri imageUri) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images/")
                .child(userId + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            String url = downloadUri.toString();
                            db.collection("users").document(userId)
                                    .update("profilePhotoUrl", url)
                                    .addOnSuccessListener(aVoid -> {
                                        Glide.with(ProfileSettingsActivity.this)
                                                .load(url)
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .into(imageViewProfilePicture);
                                        Toast.makeText(ProfileSettingsActivity.this, "Profile image updated.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Failed to save image URL.", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Failed to get image URL.", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show());
    }
}
