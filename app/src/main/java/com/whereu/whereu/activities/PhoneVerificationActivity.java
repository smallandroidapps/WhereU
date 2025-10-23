package com.whereu.whereu.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.whereu.whereu.databinding.ActivityPhoneVerificationBinding;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private ActivityPhoneVerificationBinding binding;
    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    private static final String TAG = "PhoneVerification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        binding.buttonSendOtp.setOnClickListener(v -> {
            String phoneNumber = binding.editTextPhoneNumber.getText().toString();
            if (phoneNumber.isEmpty()) {
                binding.editTextPhoneNumber.setError("Phone number is required");
                binding.editTextPhoneNumber.requestFocus();
                return;
            }
            sendVerificationCode(phoneNumber);
        });

        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String otp = binding.editTextOtp.getText().toString();
            if (otp.isEmpty()) {
                binding.editTextOtp.setError("OTP is required");
                binding.editTextOtp.requestFocus();
                return;
            }
            verifyPhoneNumberWithCode(otp);
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Sending OTP to " + phoneNumber, Toast.LENGTH_SHORT).show();
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // This callback will be invoked in two situations:
                    // 1 - Instant verification. A phone number that has previously been
                    //     used for phone sign-in is signed in without delivering an SMS.
                    // 2 - English locale verification. On some devices, if the phone number
                    //     is registered with Google and the device's locale is set to English,
                    //     an SMS is sent but the user doesn't need to enter the code manually.
                    Log.d(TAG, "onVerificationCompleted:" + credential);
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the phone number format is not valid.
                    Log.w(TAG, "onVerificationFailed", e);
                    Toast.makeText(PhoneVerificationActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    // The SMS verification code has been sent to the provided phone number,
                    // now the user can enter the code
                    Log.d(TAG, "onCodeSent:" + s);
                    verificationId = s;
                    resendToken = token;
                    Toast.makeText(PhoneVerificationActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                    binding.editTextOtp.setVisibility(View.VISIBLE);
                    binding.buttonVerifyOtp.setVisibility(View.VISIBLE);
                }
            };

    private void verifyPhoneNumberWithCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithPhoneAuthCredential:success");
                        Toast.makeText(PhoneVerificationActivity.this, "Authentication Successful", Toast.LENGTH_SHORT).show();
                        // Navigate to HomeActivity
                        Intent intent = new Intent(PhoneVerificationActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e(TAG, "signInWithPhoneAuthCredential:failure", task.getException());
                        Toast.makeText(PhoneVerificationActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}