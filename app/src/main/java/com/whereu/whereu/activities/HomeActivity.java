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
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import com.whereu.whereu.databinding.ActivityHomeBinding;
import com.whereu.whereu.fragments.LocationDetailsBottomSheetFragment;
import com.whereu.whereu.fragments.ProfileFragment;
import com.whereu.whereu.fragments.RequestsFragment;
import com.whereu.whereu.models.LocationRequest;
import com.wheru.models.User;
import com.whereu.whereu.utils.NotificationHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity implements SearchResultAdapter.OnItemClickListener, FrequentContactAdapter.OnFrequentContactClickListener, LocationDetailsBottomSheetFragment.OnLocationDetailActionListener, RequestsFragment.NotificationListener {

    private static final String TAG = "HomeActivity";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 101;
    private static final int PERMISSIONS_REQUEST_LOCATION = 102;

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
    private Map<String, Long> lastApprovedTsMap = new HashMap<>();
    private BottomNavigationView bottomNavigationView;
    private ActivityHomeBinding binding;
    private ListenerRegistration requestListener;
    private ListenerRegistration incomingRequestListener;
    private FusedLocationProviderClient fusedLocationClient;
    private Set<String> dismissedFrequentIds = new HashSet<>();

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
        requestInitialPermissions();
        scheduleBackgroundPolling();
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
        searchResultAdapter = new SearchResultAdapter(searchResultsList, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchResultAdapter);

        frequentlyRequestedRecyclerView = binding.frequentlyRequestedRecyclerView;
        frequentlyRequestedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        frequentContacts = new ArrayList<>();
        frequentContactAdapter = new FrequentContactAdapter(frequentContacts, this);
        frequentlyRequestedRecyclerView.setAdapter(frequentContactAdapter);
        frequentlyRequestedTitle = binding.frequentlyRequestedTitle;

        bottomNavigationView = binding.bottomNavigationBar;
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
            if (itemId == R.id.navigation_home) {
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
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "requests".equals(intent.getStringExtra("open_fragment"))) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_requests);
        } else {
            checkIfProfileIsComplete();
        }
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

        // Update the status in the search results
        requestStatusMap.put(request.getToUserId(), request.getStatus());
        requestIdMap.put(request.getToUserId(), request.getRequestId());
        updateSearchResultsRequestStatus();
    }

    private void updateSearchResultsRequestStatus() {
        for (SearchResultAdapter.SearchResult result : searchResultsList) {
            if (requestStatusMap.containsKey(result.getUserId())) {
                result.setRequestStatus(requestStatusMap.get(result.getUserId()));
                result.setRequestId(requestIdMap.get(result.getUserId()));
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
                    deviceContact.setDisplayName(firestoreUser.getDisplayName()); // Overwrite device name with Firestore name
                }
            }

            searchResultsList.clear();
            searchResultsList.addAll(deviceContacts);
            updateSearchResultsRequestStatus();
        });
    }

    @SuppressLint("Range")
    private List<SearchResultAdapter.SearchResult> getDeviceContacts(String query) {
        List<SearchResultAdapter.SearchResult> deviceContacts = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        String normalizedQuery = normalizePhoneNumber(query);

        if (query.length() < 2 && normalizedQuery.length() < 2) {
            return deviceContacts;
        }

        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " LIKE ? OR " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + normalizedQuery + "%"},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (phoneNumber != null) {
                    deviceContacts.add(new SearchResultAdapter.SearchResult(name, "", phoneNumber, "", false, false, "", "not_requested"));
                }
            }
            cursor.close();
        }
        return deviceContacts;
    }

    @Override
    public void onActionButtonClick(SearchResultAdapter.SearchResult result, int position) {
        if (currentUser == null) return;

        if (!result.isExistingUser()) {
            Toast.makeText(this, "Invite functionality coming soon!", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (result.getRequestStatus()) {
            case "not_requested":
            case "expired":
            case "rejected":
                sendLocationRequest(result, position);
                break;
            case "approved":
                db.collection("locationRequests").document(result.getRequestId()).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        LocationRequest approvedRequest = documentSnapshot.toObject(LocationRequest.class);
                        if (approvedRequest != null) {
                            LocationDetailsBottomSheetFragment.newInstance(approvedRequest).show(getSupportFragmentManager(), "LocationDetailsBottomSheetFragment");
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
    
    private void saveLocationRequestToFirestore(LocationRequest request, SearchResultAdapter.SearchResult result, Integer position) {
        db.collection("locationRequests").add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(HomeActivity.this, "Location request sent.", Toast.LENGTH_SHORT).show();
                    result.setRequestStatus("pending");
                    if (position != null) {
                        searchResultAdapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "Failed to send request.", Toast.LENGTH_SHORT).show());
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
                        Map<String, Long> latestRequestTimestampMap = new HashMap<>();
                        Map<String, String> latestRequestStatusMap = new HashMap<>();
                        Set<String> frequentReceiverIds = new HashSet<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LocationRequest request = document.toObject(LocationRequest.class);
                            if (request == null || request.getToUserId() == null || request.getToUserId().equals(currentUser.getUid())) {
                                continue;
                            }
                            String toId = request.getToUserId();
                            frequentReceiverIds.add(toId);

                            // Store the latest timestamp and status for each receiver
                            Long existingTimestamp = latestRequestTimestampMap.get(toId);
                            if (existingTimestamp == null || request.getTimestamp() > existingTimestamp) {
                                latestRequestTimestampMap.put(toId, request.getTimestamp());
                                latestRequestStatusMap.put(toId, request.getStatus());
                            }
                        }

                        // Update global maps
                        lastApprovedTsMap.clear();
                        lastApprovedTsMap.putAll(latestRequestTimestampMap);
                        requestStatusMap.clear();
                        requestStatusMap.putAll(latestRequestStatusMap);

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

        db.collection("users")
                .whereIn("userId", userIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        frequentContacts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            SearchResultAdapter.SearchResult sr = new SearchResultAdapter.SearchResult(
                                    user.getDisplayName(),
                                    user.getUserId(),
                                    user.getMobileNumber(),
                                    user.getEmail(),
                                    true,
                                    false,
                                    user.getProfilePhotoUrl(),
                                    requestStatusMap.getOrDefault(user.getUserId(), "not_requested")
                            );
                            Long ts = lastApprovedTsMap.get(user.getUserId());
                            if (ts != null) {
                                sr.setLastRequestTimestamp(ts);
                            }
                            frequentContacts.add(sr);
                        }
                        frequentContactAdapter.notifyDataSetChanged();
                        frequentlyRequestedTitle.setVisibility(View.VISIBLE);
                        frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        Log.w(TAG, "Error fetching frequent contact details.", task.getException());
                    }
                });
    }

    @Override
    public void onFrequentContactClick(SearchResultAdapter.SearchResult result) {
        onActionButtonClick(result, -1);
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
    public void onItemClick(SearchResultAdapter.SearchResult result) {
        Log.d(TAG, "Clicked on search result: " + result.getDisplayName());
    }

    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        setWelcomeMessage(userDisplayName);
        searchBar.setVisibility(View.VISIBLE);
        frequentlyRequestedTitle.setVisibility(View.VISIBLE);
        frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
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
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.requests_title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RequestsFragment())
                .commit();
    }

    private void showProfileFragment() {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.profile_title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .commit();
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
                    sendLocationRequest(result, null);
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
    }
}
