package com.whereu.whereu.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
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
import com.whereu.whereu.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private FirebaseUser currentUser;
    private Map<String, String> requestStatusMap = new HashMap<>();
    private Map<String, String> requestIdMap = new HashMap<>();
    private BottomNavigationView bottomNavigationView;
    private ActivityHomeBinding binding;
    private ListenerRegistration requestListener;
    private ListenerRegistration incomingRequestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initUI();
        setupListeners();
        handleIntent(getIntent());
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
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    frequentlyRequestedRecyclerView.setVisibility(View.GONE);
                    frequentlyRequestedTitle.setVisibility(View.GONE);
                    checkContactsPermissionAndPerformSearch(s.toString());
                } else {
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
                    String displayName = documentSnapshot.getString("displayName");
                    if (mobileNumber == null || mobileNumber.isEmpty() || displayName == null || displayName.isEmpty()) {
                        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                        Toast.makeText(HomeActivity.this, "Please complete your profile.", Toast.LENGTH_LONG).show();
                    } else {
                        showHomeContent();
                    }
                }
            });
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
        LocationRequest newRequest = new LocationRequest(currentUser.getUid(), result.getUserId());
        db.collection("locationRequests").add(newRequest)
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

        db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereEqualTo("status", "approved")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Set<String> frequentReceiverIds = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LocationRequest request = document.toObject(LocationRequest.class);
                            if (System.currentTimeMillis() - request.getApprovedTimestamp() <= 24 * 60 * 60 * 1000) {
                                frequentReceiverIds.add(request.getToUserId());
                            }
                        }
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
                            frequentContacts.add(new SearchResultAdapter.SearchResult(user.getDisplayName(), user.getUserId(), user.getMobileNumber(), user.getEmail(), true, false, user.getProfilePhotoUrl(), requestStatusMap.getOrDefault(user.getUserId(), "not_requested")));
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
    public void onItemClick(SearchResultAdapter.SearchResult result) {
        Log.d(TAG, "Clicked on search result: " + result.getDisplayName());
    }

    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        titleHome.setText(R.string.app_name);
        searchBar.setVisibility(View.VISIBLE);
        frequentlyRequestedTitle.setVisibility(View.VISIBLE);
        frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
        if (searchResultsList.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
        }
        removeFragment();
        fetchFrequentlyRequestedContacts(); 
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

    @Override
    public void onViewOnMapClick(double latitude, double longitude) {
        Intent intent = new Intent(HomeActivity.this, MapActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        startActivity(intent);
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
