package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.Map;
import java.util.HashMap;
import com.whereu.whereu.R;
import com.whereu.whereu.databinding.ActivitySignInBinding;
import com.wheru.models.User;

import java.util.Date;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "SignInActivity";

    private ActivitySignInBinding binding;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.signInButton.setOnClickListener(v -> signIn());
        binding.buttonPhoneSignIn.setOnClickListener(v -> startActivity(new Intent(SignInActivity.this, PhoneVerificationActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        db.collection("users").whereEqualTo("email", acct.getEmail()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                Toast.makeText(this, "This email is already linked with another account.", Toast.LENGTH_SHORT).show();
                mGoogleSignInClient.signOut();
                mAuth.signOut();
            } else {
                AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, authTask -> {
                            if (authTask.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    db.collection("users").document(user.getUid()).get()
                                            .addOnSuccessListener(documentSnapshot -> {
                                                if (documentSnapshot.exists()) {
                                                    updateUI(user);
                                                } else {
                                                    // User document doesn't exist, create new user and then check mobile number
                                                    createNewUser(user);
                                                    updateUI(user);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error fetching user document: " + e.getMessage());
                                                // Proceed to HomeActivity in case of error fetching user document
                                                createNewUser(user);
                                                updateUI(user);
                                            });
                                } else {
                                    Toast.makeText(SignInActivity.this, "Authentication failed: User is null.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(SignInActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void createNewUser(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String displayName = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String phoneNumber = ""; // Initialize as empty string to prevent overwriting
        String profilePhotoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String existingMobileNumber = "";
                    if (documentSnapshot.exists()) {
                        User existingUser = documentSnapshot.toObject(User.class);
                        if (existingUser != null && existingUser.getMobileNumber() != null) {
                            existingMobileNumber = existingUser.getMobileNumber();
                        }
                    }

                    // Use the existing mobile number if available, otherwise use the one from FirebaseUser (which might be null)
                    String finalPhoneNumber = existingMobileNumber.isEmpty() ? (firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : "") : existingMobileNumber;

                    User user = new User(userId, displayName, email, finalPhoneNumber, profilePhotoUrl, "google", new Date(), null);

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", user.getUserId());
                    userMap.put("displayName", user.getDisplayName());
                    userMap.put("email", user.getEmail());
                    userMap.put("mobileNumber", user.getMobileNumber());
                    userMap.put("profilePhotoUrl", user.getProfilePhotoUrl());
                    userMap.put("accountType", user.getAccountType());
                    userMap.put("registeredAt", user.getRegisteredAt());
                    userMap.put("lastUpdated", FieldValue.serverTimestamp());
                    // Initialize subscription fields
                    userMap.put("isPro", false);
                    userMap.put("planType", null);

                    db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "User created/updated successfully"))
                            .addOnFailureListener(e -> Log.w(TAG, "Error creating/updating user", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking for existing user", e));
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
