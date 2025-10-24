package com.whereu.whereu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.SignInActivity;
import com.whereu.whereu.databinding.FragmentProfileBinding;
import com.wheru.models.User;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Configure Google Sign In for logout
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        loadUserProfile();

        binding.cardEditProfile.setOnClickListener(v -> {
            // Handle Edit Profile click
            Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
        });

        binding.cardTrustedContacts.setOnClickListener(v -> {
            // Handle Trusted Contacts click
            Toast.makeText(getContext(), "Trusted Contacts clicked", Toast.LENGTH_SHORT).show();
        });

        binding.switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle Hide My Location toggle
            if (isChecked) {
                Toast.makeText(getContext(), "Location hidden", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Location visible", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchAutoApprove.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle Auto-Approve toggle
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
            // Google sign out
            mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(),
                    task -> {
                        // Google Sign Out successful
                        Log.d("ProfileFragment", "Google Sign Out successful");
                    });
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(requireActivity(),
                    task -> {
                        // Google access revoked
                        Log.d("ProfileFragment", "Google access revoked");
                    });
            Intent intent = new Intent(getContext(), SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            binding.userName.setText(user.getDisplayName());
                            binding.userEmail.setText(user.getEmail());
                            // Load profile image using Glide or Picasso if profilePhotoUrl is available
                            // Glide.with(this).load(user.getProfilePhotoUrl()).into(binding.profileImage);
                        }
                    } else {
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
                                    Toast.makeText(getContext(), "Error creating profile", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}