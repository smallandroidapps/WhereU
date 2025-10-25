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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        loadUserProfile();

        binding.saveButton.setOnClickListener(v -> saveMobileNumber());

        binding.cardEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ProfileSettingsActivity.class);
            startActivity(intent);
        });

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
                        if (user != null && binding != null) {
                            binding.userName.setText(user.getDisplayName());
                            binding.userEmail.setText(user.getEmail());
                            binding.editTextMobile.setText(user.getMobileNumber() != null ? user.getMobileNumber() : "");
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
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMobileNumber() {
        String mobileNumber = binding.editTextMobile.getText().toString().trim();
        if (isValidMobileNumber(mobileNumber)) {
            if (currentUser != null) {
                db.collection("users").document(currentUser.getUid())
                        .update("mobileNumber", mobileNumber)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Mobile number updated.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update mobile number.", Toast.LENGTH_SHORT).show());
            }
        } else {
            binding.editTextMobile.setError("Invalid mobile number. Must be a 10-digit Indian number.");
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
}