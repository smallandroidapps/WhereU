package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.whereu.whereu.databinding.ActivityPhoneVerificationBinding;
import com.wheru.models.User;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private static final String TAG = "PhoneVerification";
    private ActivityPhoneVerificationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.buttonSendVerificationCode.setOnClickListener(v -> sendVerificationCode());
        binding.buttonVerifyPhoneNumber.setOnClickListener(v -> verifyPhoneNumber());

        binding.ccp.registerCarrierNumberEditText(binding.editTextPhoneNumber);
    }

    private void sendVerificationCode() {
        if (!binding.ccp.isValidFullNumber()) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        String phoneNumber = binding.ccp.getFullNumberWithPlus();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            Log.d(TAG, "onVerificationCompleted:" + credential);
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
            Log.w(TAG, "onVerificationFailed", e);
            Toast.makeText(PhoneVerificationActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
            Log.d(TAG, "onCodeSent:" + verificationId);
            mVerificationId = verificationId;
            mResendToken = token;
        }
    };

    private void verifyPhoneNumber() {
        String code = binding.editTextVerificationCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mVerificationId == null) {
            Toast.makeText(this, "Please send a verification code first", Toast.LENGTH_SHORT).show();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        String phoneNumber = binding.ccp.getFullNumberWithPlus();
        db.collection("users").whereEqualTo("phoneNumber", phoneNumber).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                Toast.makeText(this, "This phone number is already linked with another account.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, authTask -> {
                            if (authTask.isSuccessful()) {
                                FirebaseUser user = authTask.getResult().getUser();
                                createNewUser(user, phoneNumber);
                                Toast.makeText(PhoneVerificationActivity.this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(PhoneVerificationActivity.this, HomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMessage = "Sign in failed. Please try again.";
                                if (authTask.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage = "Invalid verification code. Please try again.";
                                } else {
                                    finish(); // Go back to SignInActivity for other errors
                                }
                                Toast.makeText(PhoneVerificationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private void createNewUser(FirebaseUser firebaseUser, String phoneNumber) {
        String userId = firebaseUser.getUid();
        User user = new User(userId, "", "", phoneNumber, "", "phone", new Date());
        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User created successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error creating user", e));
    }
}
