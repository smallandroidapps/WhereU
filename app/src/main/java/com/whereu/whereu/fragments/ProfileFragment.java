package com.whereu.whereu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.whereu.whereu.R;
import com.whereu.whereu.activities.SignInActivity;
import com.whereu.whereu.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

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
            Intent intent = new Intent(getContext(), SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }
}