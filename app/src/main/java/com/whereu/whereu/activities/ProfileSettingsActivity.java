package com.whereu.whereu.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String accountType;

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
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        accountType = documentSnapshot.getString("accountType");
                        editTextDisplayName.setText(documentSnapshot.getString("displayName"));
                        editTextPhoneNumber.setText(documentSnapshot.getString("phoneNumber"));
                        editTextEmail.setText(documentSnapshot.getString("email"));

                        if (accountType != null) {
                            if ("google".equals(accountType)) {
                                editTextEmail.setEnabled(false);
                            } else if ("phone".equals(accountType)) {
                                editTextPhoneNumber.setEnabled(false);
                            }
                        }

                        String profilePhotoUrl = documentSnapshot.getString("profilePhotoUrl");
                        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty()) {
                            Glide.with(this).load(profilePhotoUrl).into(imageViewProfilePicture);
                        }

                        Boolean hideLocation = documentSnapshot.getBoolean("hideLocation");
                        if (hideLocation != null) {
                            switchHideLocation.setChecked(hideLocation);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileSettingsActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
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

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Toast.makeText(ProfileSettingsActivity.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String originalPhone = documentSnapshot.getString("phoneNumber");

            if (accountType.equals("google") && !phoneNumber.equals(originalPhone)) {
                db.collection("users").whereEqualTo("phoneNumber", phoneNumber).get().addOnCompleteListener(task -> {
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
        if (accountType.equals("google")) {
            userUpdates.put("phoneNumber", phoneNumber);
        } else { // phone account
            userUpdates.put("email", email);
        }

        db.collection("users").document(userId).update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileSettingsActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileSettingsActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show());
    }
}
