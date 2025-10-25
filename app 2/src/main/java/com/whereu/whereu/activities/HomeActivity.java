package com.whereu.whereu.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.whereu.whereu.R;
import com.whereu.whereu.adapters.FrequentContactAdapter;
import com.whereu.whereu.databinding.ActivityHomeBinding;
import com.whereu.whereu.fragments.LocationDetailsBottomSheetFragment;
import com.whereu.whereu.fragments.ProfileFragment;
import com.whereu.whereu.fragments.RequestsFragment;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.whereu.whereu.utils.NotificationHelper;

public class HomeActivity extends AppCompatActivity implements SearchResultAdapter.OnItemClickListener, FrequentContactAdapter.OnFrequentContactClickListener, LocationDetailsBottomSheetFragment.OnLocationDetailActionListener, RequestsFragment.NotificationListener {

    private static final String TAG = "HomeActivity";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private Group homeContentGroup;
    private TextView titleHome;
    private EditText searchBar;
    private RecyclerView searchResultsRecyclerView;
    private SearchResultAdapter searchResultAdapter;
    private List<SearchResultAdapter.SearchResult> searchResultsList;

    private List<SearchResultAdapter.SearchResult> frequentContacts;
    private RecyclerView frequentlyRequestedRecyclerView;
    private FrequentContactAdapter frequentContactAdapter;
    private TextView frequentlyRequestedTitle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private ListenerRegistration requestListenerRegistration;
    private Map<String, String> requestStatuses = new HashMap<>(); // Map to store request statuses by other user's ID
    private Map<String, String> requestIdMap = new HashMap<>();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();

