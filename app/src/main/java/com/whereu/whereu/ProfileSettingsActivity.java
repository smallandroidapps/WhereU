package com.whereu.whereu;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.whereu.whereu.databinding.ActivityProfileSettingsBinding;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ActivityProfileSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up user info (dummy data for now)
        binding.userName.setText("John Doe");
        binding.userEmail.setText("john.doe@example.com");
        // binding.profileImage.setImageResource(R.drawable.user_profile_placeholder);

        // Set up click listeners for card views
        binding.cardEditProfile.setOnClickListener(v -> {
            Toast.makeText(ProfileSettingsActivity.this, "Edit Profile Clicked", Toast.LENGTH_SHORT).show();
        });

        binding.cardTrustedContacts.setOnClickListener(v -> {
            Toast.makeText(ProfileSettingsActivity.this, "Trusted Contacts Clicked", Toast.LENGTH_SHORT).show();
        });

        binding.cardLogout.setOnClickListener(v -> {
            Toast.makeText(ProfileSettingsActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();
        });

        // Set up toggle switches
        binding.switchHideLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(ProfileSettingsActivity.this, "Hide My Location ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileSettingsActivity.this, "Hide My Location OFF", Toast.LENGTH_SHORT).show();
            }
        });

        binding.switchAutoApprove.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(ProfileSettingsActivity.this, "Auto-Approve ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileSettingsActivity.this, "Auto-Approve OFF", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up spinner for location share expiry time
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.location_expiry_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLocationExpiry.setAdapter(adapter);

        binding.spinnerLocationExpiry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                Toast.makeText(ProfileSettingsActivity.this, "Expiry Time: " + selectedOption, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
}