package com.whereu.whereu.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.whereu.whereu.R;
import com.whereu.whereu.databinding.ActivityHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private RecyclerView recentContactsRecyclerView;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize RecyclerView
        recentContactsRecyclerView = binding.recentContactsRecyclerView;
        recentContactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactList = new ArrayList<>();
        // Add some dummy data
        contactList.add(new Contact("John Doe", "123-456-7890"));
        contactList.add(new Contact("Jane Smith", "098-765-4321"));
        contactList.add(new Contact("Peter Jones", "111-222-3333"));
        contactAdapter = new ContactAdapter(contactList);
        recentContactsRecyclerView.setAdapter(contactAdapter);

        // Initialize Bottom Navigation View
        BottomNavigationView bottomNavigationView = binding.bottomNavigationBar;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                // Handle Home navigation
                return true;
            } else if (itemId == R.id.navigation_requests) {
                // Handle Requests navigation
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Handle Profile navigation
                return true;
            }
            return false;
        });

        // Initialize Floating Action Button
        FloatingActionButton fab = binding.fabRequestLocation;
        fab.setOnClickListener(view -> {
            // Handle FAB click (e.g., open a new request screen)
        });
    }
}