package com.whereu.whereu.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.whereu.whereu.R;
import com.whereu.whereu.databinding.ActivityHomeBinding;
import com.whereu.whereu.fragments.ProfileFragment;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private Group homeContentGroup;
    private TextView titleHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        homeContentGroup = binding.homeContentGroup;
        titleHome = binding.titleHome;

        BottomNavigationView bottomNavigationView = binding.bottomNavigationBar;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_requests) {
                // Handle Requests navigation
                showHomeContent(); // Placeholder, show home for now
                return true;
            } else if (itemId == R.id.navigation_profile) {
                showProfileFragment();
                return true;
            }
            return false;
        });

        // Set default view
        showHomeContent();
    }

    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        binding.fragmentContainer.setVisibility(View.GONE);
        titleHome.setText("Home");
        removeFragment();
    }

    private void showProfileFragment() {
        homeContentGroup.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        titleHome.setText("Profile");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .commit();
    }

    private void removeFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }
}
