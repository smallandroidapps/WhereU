package com.whereu.whereu.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.whereu.whereu.R;
import com.whereu.whereu.adapters.FrequentContactAdapter;
import com.whereu.whereu.adapters.FrequentlyRequestedAdapter;
import com.whereu.whereu.activities.SearchResultAdapter;
import com.whereu.whereu.databinding.ActivityHomeBinding;
import com.whereu.whereu.fragments.LocationDetailsBottomSheetFragment;
import com.whereu.whereu.fragments.ProfileFragment;
import com.whereu.whereu.fragments.RequestsFragment;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;
import com.whereu.whereu.utils.NotificationHelper;
import android.provider.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity implements SearchResultAdapter.OnItemClickListener, FrequentlyRequestedAdapter.OnRequestAgainListener, FrequentContactAdapter.OnFrequentContactClickListener, LocationDetailsBottomSheetFragment.OnLocationDetailActionListener, RequestsFragment.NotificationListener {

    private static final String TAG = "HomeActivity";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 101;
    private static final int PERMISSIONS_REQUEST_LOCATION = 102;
    private static final int PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 103;

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
    private FirebaseUser currentUser;
    private String userDisplayName;
    private Map<String, String> requestStatusMap = new HashMap<>();
    private Map<String, String> requestIdMap = new HashMap<>();
    // Stores latest approvedTimestamp per contact for expiry calculations
    private Map<String, Long> lastApprovedTsMap = new HashMap<>();
    // Stores latest sent timestamp per contact for cooldown and UI display
    private Map<String, Long> lastSentTsMap = new HashMap<>();
    private BottomNavigationView bottomNavigationView;
    private ActivityHomeBinding binding;
    private ListenerRegistration requestListener;
    private ListenerRegistration incomingRequestListener;
    private ListenerRegistration userProfileListener;
    private FusedLocationProviderClient fusedLocationClient;
    private Set<String> dismissedFrequentIds = new HashSet<>();
    private InterstitialAd mInterstitialAd;
    private boolean hasMobileNumber = false;
    private static final String PREFS_NAME = "wheru_prefs";
    private static final String KEY_ONLY_WHERU_CONTACTS = "only_wheru_contacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initUI();
        setupListeners();
        handleIntent(getIntent());
        // Request key runtime permissions without blocking UI
        requestInitialPermissions();
        scheduleBackgroundPolling();
        // Initialize Mobile Ads SDK
        MobileAds.initialize(this);
        loadInterstitial();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initUI() {
        homeContentGroup = binding.homeContentGroup;
        titleHome = binding.titleHome;
        searchBar = binding.searchBar;
        searchResultsRecyclerView = binding.searchResultsRecyclerView;

        searchResultsList = new ArrayList<>();
        searchResultAdapter = new SearchResultAdapter(searchResultsList, new SearchResultAdapter.OnItemClickListener() {
            @Override
            public void onActionButtonClick(SearchResultAdapter.SearchResult result, int position) {
                HomeActivity.this.onActionButtonClick(result, position);
            }

            @Override
            public void onItemClick(SearchResultAdapter.SearchResult result) {
                if (result == null) return;
                if (!result.isExistingUser()) {
                    Toast.makeText(HomeActivity.this, "Invite this contact to WhereU", Toast.LENGTH_SHORT).show();
                    return;
                }

                String requestId = result.getRequestId();
                // Try to load by requestId first (if present)
                if (requestId != null && !requestId.isEmpty()) {
                    FirebaseFirestore.getInstance().collection("locationRequests").document(requestId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    LocationRequest req = documentSnapshot.toObject(LocationRequest.class);
                                    if (req != null) {
                                        req.setUserName(result.getDisplayName());
                                        showLocationDetailsBottomSheet(req, result.getProfilePhotoUrl());
                                    } else {
                                        // Fallback to latest by from/to
                                        openBottomSheetWithLatestOrStub(result);
                                    }
                                } else {
                                    // Fallback to latest by from/to
                                    openBottomSheetWithLatestOrStub(result);
                                }
                            })
                            .addOnFailureListener(e -> openBottomSheetWithLatestOrStub(result));
                } else {
                    // No requestId mapped yet; attempt from/to query or stub
                    openBottomSheetWithLatestOrStub(result);
                }
            }
        });
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchResultAdapter);

        frequentlyRequestedRecyclerView = binding.frequentlyRequestedRecyclerView;
        frequentlyRequestedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        frequentContacts = new ArrayList<>();
        frequentContactAdapter = new FrequentContactAdapter(frequentContacts, this);
        frequentlyRequestedRecyclerView.setAdapter(frequentContactAdapter);
        frequentlyRequestedTitle = binding.frequentlyRequestedTitle;

        bottomNavigationView = binding.bottomNavigationBar;

        // Initialize toggle state and listener
        if (binding.toggleMyContactsOnly != null) {
            boolean onlyWheru = isOnlyWheruContactsEnabled();
            binding.toggleMyContactsOnly.setChecked(onlyWheru);
            binding.toggleMyContactsOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_ONLY_WHERU_CONTACTS, isChecked)
                        .apply();
                if (searchBar != null && searchBar.getText() != null && searchBar.getText().length() > 0) {
                    checkContactsPermissionAndPerformSearch(searchBar.getText().toString());
                }
            });
        }
    }

    @Override
    public void onItemClick(SearchResultAdapter.SearchResult result) {
        // Not used
    }

    private void setupListeners() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Show clear icon when there is text
                    searchBar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, R.drawable.ic_clear, 0);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    frequentlyRequestedRecyclerView.setVisibility(View.GONE);
                    frequentlyRequestedTitle.setVisibility(View.GONE);
                    checkContactsPermissionAndPerformSearch(s.toString());
                } else {
                    // Show filter icon when empty
                    searchBar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, R.drawable.ic_filter, 0);
                    searchResultsList.clear();
                    searchResultAdapter.notifyDataSetChanged();
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
                    frequentlyRequestedTitle.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle taps on the end drawable to clear text when visible
        searchBar.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable end = searchBar.getCompoundDrawables()[2];
                if (end != null) {
                    int drawableWidth = end.getBounds().width();
                    int rightEdge = searchBar.getRight();
                    int touchX = (int) event.getRawX();
                    if (touchX >= rightEdge - drawableWidth - searchBar.getPaddingRight()) {
                        // If there is text, treat end icon as clear
                        if (searchBar.getText() != null && searchBar.getText().length() > 0) {
                            searchBar.setText("");
                            return true;
                        }
                    }
                }
            }
            return false;
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            boolean isPro = com.whereu.whereu.activities.PlansActivity.isProUser(this);
            if (!isPro && mInterstitialAd != null) {
                mInterstitialAd.show(this);
            } else if (!isPro) {
                // Attempt reload if not ready yet
                loadInterstitial();
            }
            if (itemId == R.id.navigation_home) {
                // Clear search when returning to Home
                if (searchBar != null && searchBar.getText() != null && searchBar.getText().length() > 0) {
                    searchBar.setText("");
                }
                showHomeContent();
                return true;
            } else if (itemId == R.id.navigation_requests) {
                showRequestsFragment();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                showProfileFragment();
                return true;
            }
            return false;
        });

        setupFirestoreListeners();
        setupUserProfileListener();
    }

    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        // Google sample interstitial ad unit ID
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this,
                "ca-app-pub-3041890013064110/2975228294",
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Reload next interstitial after dismissal
                                mInterstitialAd = null;
                                loadInterstitial();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                mInterstitialAd = null;
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            String openFragment = intent.getStringExtra("open_fragment");
            String action = intent.getAction();
            // Handle custom deep links: wheru://open?id=123&tab=to|from
            if (Intent.ACTION_VIEW.equals(action)) {
                Uri data = intent.getData();
                if (data != null) {
                    String scheme = data.getScheme();
                    String host = data.getHost();
                    if ("wheru".equalsIgnoreCase(scheme) && "open".equalsIgnoreCase(host)) {
                        int initialTab = 0; // default to "To Me"
                        String tabParam = data.getQueryParameter("tab");
                        if (tabParam != null) {
                            if ("from".equalsIgnoreCase(tabParam)) {
                                initialTab = 1; // From Me
                            } else if ("to".equalsIgnoreCase(tabParam)) {
                                initialTab = 0; // To Me
                            }
                        } else {
                            // Fallback: infer from path segments if provided
                            List<String> segments = data.getPathSegments();
                            if (segments != null) {
                                if (segments.contains("from")) initialTab = 1;
                                else if (segments.contains("to")) initialTab = 0;
                            }
                        }

                        String requestId = data.getQueryParameter("id");
                        // If request id is present but tab is not specified, prefer To Me
                        if (requestId != null && tabParam == null) {
                            initialTab = 0;
                        }

                        showRequestsFragment(initialTab, requestId);
                        bottomNavigationView.setSelectedItemId(R.id.navigation_requests);
                        return;
                    }
                }
            }
            if ("requests".equals(openFragment) || (action != null && action.startsWith("OPEN_REQUESTS"))) {
                int initialTab = intent.getIntExtra("requests_tab", 0);
                // If action specifies tab, override
                if ("OPEN_REQUESTS_FROM_ME".equals(action)) {
                    initialTab = 1;
                } else if ("OPEN_REQUESTS_TO_ME".equals(action)) {
                    initialTab = 0;
                }
                String openRequestId = intent.getStringExtra("open_request_id");
                showRequestsFragment(initialTab, openRequestId);
                bottomNavigationView.setSelectedItemId(R.id.navigation_requests);
                return;
            }
        }
        checkIfProfileIsComplete();
    }

    private void checkIfProfileIsComplete() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String mobileNumber = documentSnapshot.getString("mobileNumber");
                    userDisplayName = documentSnapshot.getString("displayName");
                    if (mobileNumber == null || mobileNumber.isEmpty() || userDisplayName == null || userDisplayName.isEmpty()) {
                        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                        Toast.makeText(HomeActivity.this, "Please complete your profile.", Toast.LENGTH_LONG).show();
                    } else {
                        showHomeContent();
                    }
                }
            });
        }
    }

    private void setWelcomeMessage(String userName) {
        if (userName != null && !userName.isEmpty()) {
            binding.titleHome.setText("Welcome, " + userName);
        } else {
            binding.titleHome.setText(R.string.app_name);
        }
    }

    private void setupFirestoreListeners() {
        if (currentUser == null) return;

        // Listener for incoming requests to notify the approver
        incomingRequestListener = db.collection("locationRequests")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for incoming requests.", e);
                        return;
                    }
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            LocationRequest request = dc.getDocument().toObject(LocationRequest.class);
                            request.setRequestId(dc.getDocument().getId());
                            db.collection("users").document(request.getFromUserId()).get().addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String senderPhotoUrl = documentSnapshot.getString("profilePhotoUrl");
                                    NotificationHelper.sendNotificationForRequest(this, request.getFromUserId(), request.getRequestId(), senderPhotoUrl);
                                }
                            });
                        }
                    }
                });

        // Listener for changes to requests the current user has sent (approved/rejected)
        requestListener = db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for outgoing requests.", e);
                        return;
                    }
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            LocationRequest request = dc.getDocument().toObject(LocationRequest.class);
                            request.setRequestId(dc.getDocument().getId()); // Add this line
                            handleRequestStatusChange(request);
                        }
                    }
                });
    }

    private void handleRequestStatusChange(LocationRequest request) {
        db.collection("users").document(request.getToUserId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String receiverName = documentSnapshot.getString("displayName");
                String message = "";
                if ("approved".equals(request.getStatus())) {
                    message = receiverName + " has approved your location request.";
                } else if ("rejected".equals(request.getStatus())) {
                    message = receiverName + " has rejected your location request.";
                }
                if (!message.isEmpty()) {
                    NotificationHelper.sendLocalNotification(this, "Request Update", message);
                }
            }
        });

        // Track latest timestamps for this contact
        if ("approved".equals(request.getStatus())) {
            long apTs = request.getApprovedTimestamp();
            if (apTs > 0) {
                lastApprovedTsMap.put(request.getToUserId(), apTs);
            }
        }
        long sentTs = request.getTimestamp();
        if (sentTs > 0) {
            lastSentTsMap.put(request.getToUserId(), sentTs);
        }

        // Update the status in the search results
        requestStatusMap.put(request.getToUserId(), request.getStatus());
        requestIdMap.put(request.getToUserId(), request.getRequestId());
        updateSearchResultsRequestStatus();

        // Also refresh frequent contacts list if present
        updateFrequentContactsStatus();
    }

    private void updateFrequentContactsStatus() {
        if (frequentContacts == null || frequentContacts.isEmpty()) return;
        for (SearchResultAdapter.SearchResult contact : frequentContacts) {
            String uid = contact.getUserId();
            if (uid != null && requestStatusMap.containsKey(uid)) {
                contact.setRequestStatus(requestStatusMap.get(uid));
                contact.setRequestId(requestIdMap.get(uid));
                Long apTs = lastApprovedTsMap.get(uid);
                if (apTs != null) contact.setLastRequestTimestamp(apTs);
                Long sTs = lastSentTsMap.get(uid);
                if (sTs != null) contact.setRequestSentTimestamp(sTs);
            }
        }
        runOnUiThread(() -> {
            if (frequentContactAdapter != null) {
                frequentContactAdapter.notifyDataSetChanged();
            }
        });
    }

    private void updateSearchResultsRequestStatus() {
        long now = System.currentTimeMillis();
        long fiveMinutesMs = 5L * 60L * 1000L;
        for (SearchResultAdapter.SearchResult result : searchResultsList) {
            String uid = result.getUserId();
            if (uid != null && requestStatusMap.containsKey(uid)) {
                String status = requestStatusMap.get(uid);
                Long sentTs = lastSentTsMap.get(uid);
                // Use last approved timestamp to gate 'approved' state within 5 minutes
                if ("approved".equals(status)) {
                    Long approvedTs = lastApprovedTsMap.get(uid);
                    result.setLastRequestTimestamp(approvedTs != null ? approvedTs : 0L);
                    if (approvedTs != null && (now - approvedTs) <= fiveMinutesMs) {
                        result.setRequestStatus("approved");
                    } else {
                        // Treat older approvals as expired to encourage re-request
                        result.setRequestStatus("expired");
                    }
                } else if ("sent".equals(status) || "pending".equals(status)) {
                    result.setRequestStatus(status);
                    if (sentTs != null) {
                        result.setRequestSentTimestamp(sentTs);
                    }
                } else {
                    result.setRequestStatus(status);
                }
                result.setRequestId(requestIdMap.get(uid));
            }
        }
        runOnUiThread(() -> searchResultAdapter.notifyDataSetChanged());
    }

    private void checkContactsPermissionAndPerformSearch(String query) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            performSearch(query);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performSearch(searchBar.getText().toString());
            } else {
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission denied for notifications", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_BACKGROUND_LOCATION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    new AlertDialog.Builder(this)
                            .setTitle("Allow location always")
                            .setMessage("Please set Location permission to 'Allow all the time' in Settings for background updates.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("Not now", null)
                            .show();
                } else {
                    Toast.makeText(this, "Background location not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^0-9]+", "");
    }

    private String getLast10Digits(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = normalizePhoneNumber(phoneNumber);
        if (normalized.length() > 10) {
            return normalized.substring(normalized.length() - 10);
        }
        return normalized;
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            searchResultsList.clear();
            searchResultAdapter.notifyDataSetChanged();
            return;
        }

        List<SearchResultAdapter.SearchResult> deviceContacts = getDeviceContacts(query);
        List<String> phoneNumbers = new ArrayList<>();
        for (SearchResultAdapter.SearchResult contact : deviceContacts) {
            phoneNumbers.add(getLast10Digits(contact.getPhoneNumber()));
        }

        if (phoneNumbers.isEmpty()) {
            searchResultsList.clear();
            searchResultsList.addAll(deviceContacts);
            searchResultAdapter.notifyDataSetChanged();
            return;
        }

        db.collection("users").whereIn("mobileNumber", phoneNumbers).get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, User> firestoreUsers = new HashMap<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                firestoreUsers.put(getLast10Digits(user.getMobileNumber()), user);
            }

            for (SearchResultAdapter.SearchResult deviceContact : deviceContacts) {
                String normalizedNumber = getLast10Digits(deviceContact.getPhoneNumber());
                if (firestoreUsers.containsKey(normalizedNumber)) {
                    User firestoreUser = firestoreUsers.get(normalizedNumber);
                    deviceContact.setExistingUser(true);
                    deviceContact.setUserId(firestoreUser.getUserId());
                    deviceContact.setProfilePhotoUrl(firestoreUser.getProfilePhotoUrl());
                    // Preserve device contact name for search by saved name; keep Firestore linkage without overwriting
                }
            }

            // Apply filter if enabled
            boolean onlyWheru = isOnlyWheruContactsEnabled();
            List<SearchResultAdapter.SearchResult> filtered = new ArrayList<>();
            for (SearchResultAdapter.SearchResult dc : deviceContacts) {
                if (!onlyWheru || dc.isExistingUser()) {
                    filtered.add(dc);
                }
            }

            searchResultsList.clear();
            searchResultsList.addAll(filtered);
            updateSearchResultsRequestStatus();
        });
    }

    private boolean isOnlyWheruContactsEnabled() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ONLY_WHERU_CONTACTS, true);
    }

    @SuppressLint("Range")
    private List<SearchResultAdapter.SearchResult> getDeviceContacts(String query) {
        // Use a LinkedHashMap to deduplicate by normalized last-10 digits while preserving order
        Map<String, SearchResultAdapter.SearchResult> uniqueByPhone = new java.util.LinkedHashMap<>();
        ContentResolver contentResolver = getContentResolver();
        String normalizedQuery = normalizePhoneNumber(query);

        if (query.length() < 2 && normalizedQuery.length() < 2) {
            return new ArrayList<>();
        }

        // Build dynamic selection: all name tokens must match (AND), or number matches
        List<String> args = new ArrayList<>();
        List<String> nameClauses = new ArrayList<>();
        for (String token : query.trim().split("\\s+")) {
            if (token.length() >= 2) {
                nameClauses.add(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " LIKE ?");
                args.add("%" + token + "%");
            }
        }
        StringBuilder selection = new StringBuilder();
        if (!nameClauses.isEmpty()) {
            selection.append("(");
            for (int i = 0; i < nameClauses.size(); i++) {
                if (i > 0) selection.append(" AND ");
                selection.append(nameClauses.get(i));
            }
            selection.append(")");
        }
        if (normalizedQuery.length() >= 2) {
            if (selection.length() > 0) selection.append(" OR ");
            selection.append(ContactsContract.CommonDataKinds.Phone.NUMBER).append(" LIKE ?");
            args.add("%" + normalizedQuery + "%");
        }

        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER},
                selection.toString(),
                args.toArray(new String[0]),
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (phoneNumber != null) {
                    String key = getLast10Digits(phoneNumber);
                    // Keep the first seen entry for this number
                    if (!uniqueByPhone.containsKey(key)) {
                        uniqueByPhone.put(key, new SearchResultAdapter.SearchResult(name, "", phoneNumber, "", false, false, "", "not_requested"));
                    }
                }
            }
            cursor.close();
        }
        return new ArrayList<>(uniqueByPhone.values());
    }

    @Override
    public void onActionButtonClick(SearchResultAdapter.SearchResult result, int position) {
        if (currentUser == null) return;

        if (!result.isExistingUser()) {
            Toast.makeText(this, "Invite functionality coming soon!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent sending a request to self
        if (result.getUserId() != null && result.getUserId().equals(currentUser.getUid())) {
            Toast.makeText(this, "You cannot request your own location.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (result.getRequestStatus()) {
            case "not_requested":
            case "expired":
            case "rejected":
                sendLocationRequest(result, position);
                break;
            case "approved":
            case "sent":
                db.collection("locationRequests").document(result.getRequestId()).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        LocationRequest approvedRequest = documentSnapshot.toObject(LocationRequest.class);
                        if (approvedRequest != null) {
                            showLocationDetailsBottomSheet(approvedRequest);
                        }
                    } else {
                        Toast.makeText(this, "Could not find request details.", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case "pending":
                Toast.makeText(this, "Request already sent.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void sendLocationRequest(SearchResultAdapter.SearchResult result, Integer position) {
        // Gate sending requests by mobile number presence
        if (!ensureMobileNumberPresent()) {
            return;
        }
        // Prevent self-requests as a safety check
        if (currentUser != null && result.getUserId() != null && result.getUserId().equals(currentUser.getUid())) {
            Toast.makeText(this, "You cannot request your own location.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if user is in cooldown period
        if (result.isInCooldown()) {
            long remainingTime = result.getCooldownRemainingTime();
            Toast.makeText(this, "Please wait " + SearchResultAdapter.SearchResult.formatCooldownTime(remainingTime) + " before sending another request", Toast.LENGTH_SHORT).show();
            return;
        }

        // Capture requestor's current location when sending request
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        LocationRequest newRequest = new LocationRequest(currentUser.getUid(), result.getUserId());
        
        if (fineGranted || coarseGranted) {
            FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
            fusedClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            newRequest.setLatitude(location.getLatitude());
                            newRequest.setLongitude(location.getLongitude());
                            
                            // Get area name from coordinates
                            try {
                                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address addr = addresses.get(0);
                                    StringBuilder areaName = new StringBuilder();
                                    if (addr.getLocality() != null && !addr.getLocality().isEmpty()) {
                                        areaName.append(addr.getLocality());
                                    }
                                    if (addr.getAdminArea() != null && !addr.getAdminArea().isEmpty()) {
                                        if (areaName.length() > 0) areaName.append(", ");
                                        areaName.append(addr.getAdminArea());
                                    }
                                    if (addr.getCountryName() != null && !addr.getCountryName().isEmpty()) {
                                        if (areaName.length() > 0) areaName.append(", ");
                                        areaName.append(addr.getCountryName());
                                    }
                                    newRequest.setAreaName(areaName.toString());
                                }
                            } catch (IOException e) {
                                // Ignore geocoding errors
                            }
                        }
                        
                        // Save request to Firestore
                        saveLocationRequestToFirestore(newRequest, result, position);
                    })
                    .addOnFailureListener(e -> {
                        // If location fails, still save the request without coordinates
                        saveLocationRequestToFirestore(newRequest, result, position);
                    });
        } else {
            // No location permission, save request without coordinates
            saveLocationRequestToFirestore(newRequest, result, position);
        }
    }

    private boolean ensureMobileNumberPresent() {
        if (hasMobileNumber) return true;
        Toast.makeText(this, "Please add your mobile number to continue.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, ProfileSettingsActivity.class);
        intent.putExtra("force_mobile_update", true);
        startActivity(intent);
        // Also surface the Profile tab for clarity
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
        }
        return false;
    }
    
    private void startCooldownTimer(SearchResultAdapter.SearchResult result, Integer position) {
        // Create a countdown timer for the cooldown period
        CountDownTimer timer = new CountDownTimer(60000, 1000) { // 1 minute cooldown
            private boolean blinkStarted = false;

            public void onTick(long millisUntilFinished) {
                // Update the result with remaining cooldown time
                result.setCooldownRemainingTime(millisUntilFinished);

                // Update only the timer text on the visible view holder, avoid full card redraws
                if (position != null && position >= 0) {
                    RecyclerView.ViewHolder vh = searchResultsRecyclerView.findViewHolderForAdapterPosition(position);
                    if (vh instanceof SearchResultAdapter.SearchResultViewHolder) {
                        SearchResultAdapter.SearchResultViewHolder holder = (SearchResultAdapter.SearchResultViewHolder) vh;
                        holder.actionButton.setText("Wait " + SearchResultAdapter.SearchResult.formatCooldownTime(millisUntilFinished));
                        holder.actionButton.setEnabled(false);

                        // Start a subtle blink on the timer text once
                        if (!blinkStarted) {
                            blinkStarted = true;
                            android.view.animation.AlphaAnimation blink = new android.view.animation.AlphaAnimation(1.0f, 0.4f);
                            blink.setDuration(500);
                            blink.setRepeatMode(android.view.animation.Animation.REVERSE);
                            blink.setRepeatCount(android.view.animation.Animation.INFINITE);
                            holder.actionButton.startAnimation(blink);
                        }
                    } else {
                        // If holder not attached, do a minimal item update
                        searchResultAdapter.notifyItemChanged(position);
                    }
                } else {
                    searchResultAdapter.notifyDataSetChanged();
                }
            }

            public void onFinish() {
                // Cooldown finished, check if request is still "sent" and update to "pending" if so
                if ("sent".equals(result.getRequestStatus())) {
                    result.setRequestStatus("pending");
                }
                result.setCooldownRemainingTime(0);

                if (position != null && position >= 0) {
                    RecyclerView.ViewHolder vh = searchResultsRecyclerView.findViewHolderForAdapterPosition(position);
                    if (vh instanceof SearchResultAdapter.SearchResultViewHolder) {
                        SearchResultAdapter.SearchResultViewHolder holder = (SearchResultAdapter.SearchResultViewHolder) vh;
                        holder.actionButton.clearAnimation();
                        holder.actionButton.setText("Request Sent");
                        holder.actionButton.setEnabled(false);
                    }
                    searchResultAdapter.notifyItemChanged(position);
                } else {
                    searchResultAdapter.notifyDataSetChanged();
                }
            }
        }.start();
    }

    private void saveLocationRequestToFirestore(LocationRequest request, SearchResultAdapter.SearchResult result, Integer position) {
        // Double-check we are not saving a self-request
        if (currentUser != null && request != null && currentUser.getUid().equals(request.getToUserId())) {
            Toast.makeText(HomeActivity.this, "You cannot request your own location.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("locationRequests").add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(HomeActivity.this, "Location request sent.", Toast.LENGTH_SHORT).show();
                    // Set status to "sent" and record timestamp for cooldown
                    result.setRequestStatus("sent");
                    result.setRequestSentTimestamp(System.currentTimeMillis());
                    result.setRequestId(documentReference.getId());
                    // Update in-memory status map for immediate UI refresh
                    if (request.getToUserId() != null) {
                        requestStatusMap.put(request.getToUserId(), "sent");
                        requestIdMap.put(request.getToUserId(), documentReference.getId());
                        lastSentTsMap.put(request.getToUserId(), result.getRequestSentTimestamp());
                    }
                    if (position != null && position >= 0) {
                        searchResultAdapter.notifyItemChanged(position);
                    } else {
                        // If position is invalid, refresh the entire list
                        searchResultAdapter.notifyDataSetChanged();
                    }
                    // Refresh visible statuses on the search list
                    updateSearchResultsRequestStatus();
                    // Start countdown timer for cooldown
                    startCooldownTimer(result, position);
                })
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "Failed to send request.", Toast.LENGTH_SHORT).show());
    }

    private void showLocationDetailsBottomSheet(LocationRequest req) {
        showLocationDetailsBottomSheet(req, null);
    }

    private void showLocationDetailsBottomSheet(LocationRequest req, @Nullable String photoUrl) {
        if (req == null) return;
        String tag = "location_details";
        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        if (existing != null && existing.isAdded()) {
            return; // Prevent multiple popups stacking
        }
        LocationDetailsBottomSheetFragment fragment = LocationDetailsBottomSheetFragment.newInstance(req, photoUrl);
        fragment.show(getSupportFragmentManager(), tag);
    }

    private void openBottomSheetWithLatestOrStub(SearchResultAdapter.SearchResult result) {
        if (currentUser == null || result == null || result.getUserId() == null) {
            Toast.makeText(HomeActivity.this, "Could not open details", Toast.LENGTH_SHORT).show();
            return;
        }
        // Attempt to fetch latest request between current user and this contact WITHOUT orderBy
        db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereEqualTo("toUserId", result.getUserId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    LocationRequest latest = null;
                    long latestTs = Long.MIN_VALUE;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        LocationRequest lr = doc.toObject(LocationRequest.class);
                        if (lr != null) {
                            lr.setRequestId(doc.getId());
                            long ts = lr.getApprovedTimestamp() > 0 ? lr.getApprovedTimestamp() : lr.getTimestamp();
                            if (ts > latestTs) {
                                latestTs = ts;
                                latest = lr;
                            }
                        }
                    }
                    if (latest == null) {
                        // Create a stub request if nothing found
                        latest = new LocationRequest(currentUser.getUid(), result.getUserId());
                        String s = result.getRequestStatus();
                        latest.setStatus(s != null ? s : "not_requested");
                        latest.setTimestamp(0);
                        latest.setApprovedTimestamp(0);
                    }
                    latest.setUserName(result.getDisplayName());
                    showLocationDetailsBottomSheet(latest, result.getProfilePhotoUrl());
                })
                .addOnFailureListener(e -> {
                    // Final fallback to a stub
                    LocationRequest stub = new LocationRequest(currentUser.getUid(), result.getUserId());
                    String s = result.getRequestStatus();
                    stub.setStatus(s != null ? s : "not_requested");
                    stub.setTimestamp(0);
                    stub.setApprovedTimestamp(0);
                    stub.setUserName(result.getDisplayName());
                    showLocationDetailsBottomSheet(stub, result.getProfilePhotoUrl());
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rebuild and refresh statuses whenever screen becomes visible
        updateSearchResultsRequestStatus();
        loadDismissedFrequentIdsAndFetch();
        // Re-check background location after returning from Settings
        ensureAlwaysLocationPermission();
    }

    private void fetchFrequentlyRequestedContacts() {
        if (currentUser == null) return;

        long cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000; // last 30 days

        db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereGreaterThan("timestamp", cutoff)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Increased limit to get a better sample
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Long> latestApprovedTimestampMap = new HashMap<>();
                        Map<String, Long> latestSentTimestampMap = new HashMap<>();
                        Map<String, String> latestRequestStatusMap = new HashMap<>();
                        Map<String, String> latestRequestIdMap = new HashMap<>();
                        Set<String> frequentReceiverIds = new HashSet<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LocationRequest request = document.toObject(LocationRequest.class);
                            if (request == null || request.getToUserId() == null || request.getToUserId().equals(currentUser.getUid())) {
                                continue;
                            }
                            String toId = request.getToUserId();
                            frequentReceiverIds.add(toId);

                            // Track the latest status
                            String existingStatus = latestRequestStatusMap.get(toId);
                            if (existingStatus == null) {
                                latestRequestStatusMap.put(toId, request.getStatus());
                                latestRequestIdMap.put(toId, document.getId());
                            }

                            // Track the latest approvedTimestamp per receiver (for expiry)
                            if ("approved".equals(request.getStatus())) {
                                long approvedTs = request.getApprovedTimestamp();
                                Long existingApprovedTs = latestApprovedTimestampMap.get(toId);
                                if (approvedTs > 0 && (existingApprovedTs == null || approvedTs > existingApprovedTs)) {
                                    latestApprovedTimestampMap.put(toId, approvedTs);
                                }
                            }

                            // Track latest sent timestamp per receiver (for cooldown/UI)
                            long ts = request.getTimestamp();
                            Long existingSentTs = latestSentTimestampMap.get(toId);
                            if (ts > 0 && (existingSentTs == null || ts > existingSentTs)) {
                                latestSentTimestampMap.put(toId, ts);
                            }
                        }

                        // Update global maps
                        lastApprovedTsMap.clear();
                        lastApprovedTsMap.putAll(latestApprovedTimestampMap);
                        lastSentTsMap.clear();
                        lastSentTsMap.putAll(latestSentTimestampMap);
                        requestStatusMap.clear();
                        requestStatusMap.putAll(latestRequestStatusMap);
                        requestIdMap.clear();
                        requestIdMap.putAll(latestRequestIdMap);

                        // Refresh search list statuses using the rebuilt maps
                        updateSearchResultsRequestStatus();

                        // Filter out dismissed frequent IDs
                        frequentReceiverIds.removeAll(dismissedFrequentIds);
                        fetchFrequentContactDetails(new ArrayList<>(frequentReceiverIds));
                    } else {
                        Log.w(TAG, "Error getting frequently requested contacts.", task.getException());
                    }
                });
    }

    private void fetchFrequentContactDetails(List<String> userIds) {
        if (userIds.isEmpty()) {
            frequentlyRequestedTitle.setVisibility(View.GONE);
            frequentlyRequestedRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Firestore whereIn supports up to 10 items; batch queries
        final int batchSize = 10;
        final int totalBatches = (userIds.size() + batchSize - 1) / batchSize;
        final java.util.concurrent.atomic.AtomicInteger completedBatches = new java.util.concurrent.atomic.AtomicInteger(0);
        final List<SearchResultAdapter.SearchResult> aggregated = new ArrayList<>();

        for (int start = 0; start < userIds.size(); start += batchSize) {
            List<String> batch = userIds.subList(start, Math.min(start + batchSize, userIds.size()));
            db.collection("users")
                    .whereIn("userId", batch)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                String baseStatus = requestStatusMap.getOrDefault(user.getUserId(), "not_requested");
                                // If approved and beyond expiry, mark as expired
                                if ("approved".equals(baseStatus)) {
                                    Long apTs = lastApprovedTsMap.get(user.getUserId());
                                    if (apTs != null) {
                                        long expiryMs = 24L * 60 * 60 * 1000; // 24 hours
                                        if (System.currentTimeMillis() - apTs > expiryMs) {
                                            baseStatus = "expired";
                                        }
                                    }
                                }

                                SearchResultAdapter.SearchResult sr = new SearchResultAdapter.SearchResult(
                                        user.getDisplayName(),
                                        user.getUserId(),
                                        user.getMobileNumber(),
                                        user.getEmail(),
                                        true,
                                        false,
                                        user.getProfilePhotoUrl(),
                                        baseStatus
                                );
                                Long ts = lastApprovedTsMap.get(user.getUserId());
                                if (ts != null) {
                                    sr.setLastRequestTimestamp(ts);
                                }
                                Long sTs = lastSentTsMap.get(user.getUserId());
                                if (sTs != null) {
                                    sr.setRequestSentTimestamp(sTs);
                                }
                                String reqId = requestIdMap.get(user.getUserId());
                                if (reqId != null) {
                                    sr.setRequestId(reqId);
                                }
                                aggregated.add(sr);
                            }
                        } else {
                            Log.w(TAG, "Error fetching frequent contact details batch.", task.getException());
                        }

                        if (completedBatches.incrementAndGet() == totalBatches) {
                            frequentContacts.clear();
                            frequentContacts.addAll(aggregated);
                            frequentContactAdapter.notifyDataSetChanged();
                            frequentlyRequestedTitle.setVisibility(View.VISIBLE);
                            frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    @Override
    public void onRequestAgain(LocationRequest request) {
        if (currentUser == null) return;
        db.collection("users").document(request.getToUserId()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    SearchResultAdapter.SearchResult result = new SearchResultAdapter.SearchResult(
                            user.getDisplayName(),
                            user.getUserId(),
                            user.getMobileNumber(),
                            user.getEmail(),
                            true,
                            false,
                            user.getProfilePhotoUrl(),
                            "not_requested"
                    );
                    sendLocationRequest(result, -1);
                }
            }
        });
    }

    @Override
    public void onClearFrequentContact(SearchResultAdapter.SearchResult contact) {
        if (currentUser == null || contact == null || contact.getUserId() == null) {
            return;
        }
        String dismissedId = contact.getUserId();
        dismissedFrequentIds.add(dismissedId);

        // Update UI immediately
        for (int i = 0; i < frequentContacts.size(); i++) {
            if (dismissedId.equals(frequentContacts.get(i).getUserId())) {
                frequentContacts.remove(i);
                break;
            }
        }
        frequentContactAdapter.notifyDataSetChanged();
        if (frequentContacts.isEmpty()) {
            frequentlyRequestedTitle.setVisibility(View.GONE);
            frequentlyRequestedRecyclerView.setVisibility(View.GONE);
        }

        // Persist dismissal
        db.collection("users").document(currentUser.getUid())
                .update("dismissedFrequentIds", FieldValue.arrayUnion(dismissedId))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dismissed ID saved: " + dismissedId))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to save dismissed ID", e));
    }



    @Override
    public void onFrequentContactClick(SearchResultAdapter.SearchResult contact) {
        // Not used
    }



    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        setWelcomeMessage(userDisplayName);
        searchBar.setVisibility(View.VISIBLE);
        if (frequentContacts != null && !frequentContacts.isEmpty()) {
            frequentlyRequestedTitle.setVisibility(View.VISIBLE);
            frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
        } else {
            frequentlyRequestedTitle.setVisibility(View.GONE);
            frequentlyRequestedRecyclerView.setVisibility(View.GONE);
        }
        if (searchResultsList.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
        removeFragment();
        loadDismissedFrequentIdsAndFetch(); 
    }

    private void loadDismissedFrequentIdsAndFetch() {
        if (currentUser == null) {
            fetchFrequentlyRequestedContacts();
            return;
        }
        // Auto-hide frequent section while loading
        if (frequentlyRequestedTitle != null) {
            frequentlyRequestedTitle.setVisibility(View.GONE);
        }
        if (frequentlyRequestedRecyclerView != null) {
            frequentlyRequestedRecyclerView.setVisibility(View.GONE);
        }
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    dismissedFrequentIds.clear();
                    if (documentSnapshot.exists()) {
                        List<String> dismissed = (List<String>) documentSnapshot.get("dismissedFrequentIds");
                        if (dismissed != null) {
                            dismissedFrequentIds.addAll(dismissed);
                        }
                    }
                    fetchFrequentlyRequestedContacts();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to load dismissed frequent IDs", e);
                    fetchFrequentlyRequestedContacts();
                });
    }

    private void showRequestsFragment() {
        showRequestsFragment(0);
    }

    private void showRequestsFragment(int initialTab) {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.requests_title);
        RequestsFragment fragment = new RequestsFragment();
        Bundle args = new Bundle();
        args.putInt("initial_tab", initialTab);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showRequestsFragment(int initialTab, @Nullable String openRequestId) {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.requests_title);
        RequestsFragment fragment = new RequestsFragment();
        Bundle args = new Bundle();
        args.putInt("initial_tab", initialTab);
        if (openRequestId != null && !openRequestId.isEmpty()) {
            args.putString("open_request_id", openRequestId);
        }
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showProfileFragment() {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.profile_title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .commit();
    }

    private void setupUserProfileListener() {
        if (currentUser == null) return;
        userProfileListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "User profile listener failed.", e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        String displayName = snapshot.getString("displayName");
                        setWelcomeMessage(displayName);
                        String mobile = snapshot.getString("mobileNumber");
                        hasMobileNumber = mobile != null && !mobile.isEmpty();
                        String photoUrl = snapshot.getString("profilePhotoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(binding.profileImage);
                        } else {
                            binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    }
                });
    }

    private void removeFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
        }
    }

    private void requestInitialPermissions() {
        // Notifications (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
            }
        }
        // Contacts
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        // Location (if needed for features like viewing or sharing location)
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fineGranted || !coarseGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
        }
        // Encourage "Always allow" background location for reliable updates
        ensureAlwaysLocationPermission();
        // Encourage disabling battery optimizations for background reliability
        promptBackgroundRunPermission();
    }

    private void ensureAlwaysLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean backgroundGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!backgroundGranted && fineGranted) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Allow location always")
                            .setMessage("To share or refresh your location in the background, please set Location permission to 'Allow all the time' in Settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("Not now", null)
                            .show();
                }
            }
        }
    }

    private void promptBackgroundRunPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Allow background run")
                        .setMessage("To keep WhereU running reliably in the background, please disable battery optimizations for the app.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Not now", null)
                        .show();
            }
        }
    }

    private void scheduleBackgroundPolling() {
        androidx.work.Constraints constraints = new androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();

        androidx.work.PeriodicWorkRequest workRequest = new androidx.work.PeriodicWorkRequest.Builder(
                com.whereu.whereu.workers.NewRequestsWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "new_requests_poll",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );

        // Also run an immediate one-time check to notify quickly at startup
        androidx.work.OneTimeWorkRequest immediate = new androidx.work.OneTimeWorkRequest.Builder(
                com.whereu.whereu.workers.NewRequestsWorker.class)
                .setConstraints(constraints)
                .build();
        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
                "new_requests_poll_immediate",
                androidx.work.ExistingWorkPolicy.REPLACE,
                immediate
        );
    }

    @Override
    public void onViewOnMapClick(double latitude, double longitude) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", latitude, longitude, latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps app not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestAgainClick(String receiverId) {
        if (currentUser == null) return;
        db.collection("users").document(receiverId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    SearchResultAdapter.SearchResult result = new SearchResultAdapter.SearchResult(
                            user.getDisplayName(),
                            user.getUserId(),
                            user.getMobileNumber(),
                            user.getEmail(),
                            true,
                            false,
                            user.getProfilePhotoUrl(),
                            "not_requested"
                    );
                    sendLocationRequest(result, -1);
                }
            }
        });
    }

    @Override
    public void onSendLocalNotification(String title, String message) {
        NotificationHelper.sendLocalNotification(this, title, message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestListener != null) {
            requestListener.remove();
        }
        if (incomingRequestListener != null) {
            incomingRequestListener.remove();
        }
        if (userProfileListener != null) {
            userProfileListener.remove();
        }
    }
}
