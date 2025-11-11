package com.whereu.whereu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.ProfileSettingsActivity;
import com.whereu.whereu.activities.SignInActivity;
import com.whereu.whereu.activities.TrustedContactsActivity;
import com.whereu.whereu.databinding.FragmentProfileBinding;
import com.wheru.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Animation fadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        binding.linearLayoutProfileContent.startAnimation(fadeInAnimation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Register image picker for updating profile photo
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadProfileImage(uri);
            }
        });

        binding.profileImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Show loader and begin fetching profile
        showLoading(true);
        loadUserProfile();

        binding.saveProfileButton.setOnClickListener(v -> saveProfile());

        binding.cardTrustedContacts.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TrustedContactsActivity.class);
            startActivity(intent);
        });

        binding.switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getContext(), "Location hidden", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Location visible", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchAutoApprove.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getContext(), "Auto-approve enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Auto-approve disabled", Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.expiry_time_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLocationExpiry.setAdapter(adapter);

        binding.cardLogout.setOnClickListener(v -> {
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> Log.d(TAG, "Google Sign Out successful"));
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(requireActivity(), task -> Log.d(TAG, "Google access revoked"));
            Intent intent = new Intent(getContext(), SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        binding.cardEditProfile.setOnClickListener(v -> {
            binding.userName.setEnabled(true);
            binding.editTextMobile.setEnabled(true);
            binding.editTextMobile.setVisibility(View.VISIBLE);
            binding.userMobile.setVisibility(View.GONE);
            binding.saveProfileButton.setVisibility(View.VISIBLE);
            if (binding.editPenOverlay != null) {
                binding.editPenOverlay.setVisibility(View.VISIBLE);
            }
            Toast.makeText(getContext(), "You can now edit your profile.", Toast.LENGTH_SHORT).show();
        });

        // Upgrade to Pro entry point
        if (binding.cardUpgradePro != null) {
            binding.cardUpgradePro.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), com.whereu.whereu.activities.PlansActivity.class));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Unable to open upgrade", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Fetch and reflect Pro status from Firestore
        reflectProStatusFromFirestore();
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && binding != null) {
                            binding.userName.setText(user.getDisplayName());
                            binding.userEmail.setText(user.getEmail());
                            binding.userMobile.setText(user.getMobileNumber() != null ? user.getMobileNumber() : "");
                            binding.editTextMobile.setText(user.getMobileNumber() != null ? user.getMobileNumber() : "");

                            // Check if displayName or mobileNumber is blank/null
                            if (user.getDisplayName() == null || user.getDisplayName().trim().isEmpty() ||
                                user.getMobileNumber() == null || user.getMobileNumber().trim().isEmpty()) {
                                Toast.makeText(getContext(), "Please update your display name and mobile number.", Toast.LENGTH_LONG).show();
                                binding.userName.setEnabled(true);
                                binding.editTextMobile.setEnabled(true);
                                binding.editTextMobile.setVisibility(View.VISIBLE);
                                binding.userMobile.setVisibility(View.GONE);
                                binding.saveProfileButton.setVisibility(View.VISIBLE);
                                if (binding.editPenOverlay != null) {
                                    binding.editPenOverlay.setVisibility(View.VISIBLE);
                                }
                            } else {
                                binding.userName.setEnabled(false);
                                binding.editTextMobile.setEnabled(false);
                                binding.editTextMobile.setVisibility(View.GONE);
                                binding.userMobile.setVisibility(View.VISIBLE);
                                binding.saveProfileButton.setVisibility(View.GONE);
                                if (binding.editPenOverlay != null) {
                                    binding.editPenOverlay.setVisibility(View.GONE);
                                }
                            }
                            // Load profile image if available
                            if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                                com.bumptech.glide.Glide.with(requireContext())
                                        .load(user.getProfilePhotoUrl())
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .into(binding.profileImage);
                            }
                            // Hide loader once data is bound
                            showLoading(false);
                        }
                    } else {
                        Map<String, Object> newUserProfile = new HashMap<>();
                        newUserProfile.put("displayName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
                        newUserProfile.put("email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
                        newUserProfile.put("mobileNumber", "");
                        newUserProfile.put("profilePhotoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");

                        db.collection("users").document(userId).set(newUserProfile)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Default user profile created successfully.");
                                    loadUserProfile();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating default user profile", e);
                                    Toast.makeText(getContext(), "Error creating profile", Toast.LENGTH_SHORT).show();
                                    showLoading(false);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void saveProfile() {
        String displayName = binding.userName.getText().toString().trim();
        String mobileNumber = binding.editTextMobile.getText().toString().trim();

        if (displayName.isEmpty()) {
            binding.userName.setError("Display name cannot be empty.");
            return;
        }

        if (!isValidMobileNumber(mobileNumber)) {
            binding.editTextMobile.setError("Invalid mobile number. Must be a 10-digit Indian number.");
            return;
        }

        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("displayName", displayName);
            updates.put("mobileNumber", mobileNumber);

            db.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                        binding.userName.setEnabled(false);
                        binding.editTextMobile.setEnabled(false);
                        binding.editTextMobile.setVisibility(View.GONE);
                        binding.userMobile.setVisibility(View.VISIBLE);
                        binding.saveProfileButton.setVisibility(View.GONE);
                        if (binding.editPenOverlay != null) {
                            binding.editPenOverlay.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show());
        }
    }

    private void reflectProStatusFromFirestore() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean isPro = doc.getBoolean("isPro");
                        String planType = doc.getString("planType");
                        boolean proActive = isPro != null && isPro;

                        if (binding.proCrown != null) {
                            binding.proCrown.setVisibility(proActive ? View.VISIBLE : View.GONE);
                        }
                        if (binding.cardUpgradePro != null) {
                            binding.cardUpgradePro.setVisibility(proActive ? View.GONE : View.VISIBLE);
                        }
                        // Optionally show plan type somewhere if desired in future
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to local preference check if Firestore fails
                    boolean localPro = com.whereu.whereu.activities.PlansActivity.isProUser(requireContext());
                    if (binding.proCrown != null) {
                        binding.proCrown.setVisibility(localPro ? View.VISIBLE : View.GONE);
                    }
                    if (binding.cardUpgradePro != null) {
                        binding.cardUpgradePro.setVisibility(localPro ? View.GONE : View.VISIBLE);
                    }
                });
    }

    private void showLoading(boolean loading) {
        if (binding == null) return;
        binding.profileLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        // Hide content sections while loading and show them after
        binding.topSectionLayout.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.linearLayoutProfileContent.setVisibility(loading ? View.GONE : View.VISIBLE);
        if (loading) {
            binding.saveProfileButton.setVisibility(View.GONE);
        }
    }

    private boolean isValidMobileNumber(String mobile) {
        if (mobile == null) return false;
        String regex = "^[6-9]\\d{9}$";
        return Pattern.matches(regex, mobile);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void uploadProfileImage(android.net.Uri imageUri) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

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
                                        if (binding != null) {
                                            com.bumptech.glide.Glide.with(requireContext())
                                                    .load(url)
                                                    .placeholder(R.drawable.ic_profile)
                                                    .error(R.drawable.ic_profile)
                                                    .into(binding.profileImage);
                                        }
                                        Toast.makeText(getContext(), "Profile image updated.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save image URL.", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to get image URL.", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Image upload failed.", Toast.LENGTH_SHORT).show());
    }
}
