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
import com.google.firebase.firestore.FirebaseFirestore;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements SearchResultAdapter.OnItemClickListener, FrequentContactAdapter.OnFrequentContactClickListener, LocationDetailsBottomSheetFragment.OnLocationDetailActionListener {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

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

        setupLocationRequestSnapshotListener();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    checkContactsPermissionAndPerformSearch(s.toString());
                } else {
                    searchResultsList.clear();
                    searchResultAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        BottomNavigationView bottomNavigationView = binding.bottomNavigationBar;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                showHomeContent();
                fetchFrequentlyRequestedContacts();
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

        showHomeContent();
        checkIfMobileNumberExists();
    }

    private void checkIfMobileNumberExists() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String mobileNumber = documentSnapshot.getString("mobileNumber");
                    if (mobileNumber == null || mobileNumber.isEmpty()) {
                        showProfileFragment();
                    }
                }
            });
        }
    }


    private void setupLocationRequestSnapshotListener() {
        if (currentUser == null) return;
        db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        LocationRequest request = doc.toObject(LocationRequest.class);
                        requestStatusMap.put(request.getToUserId(), request.getStatus());
                        requestIdMap.put(request.getToUserId(), doc.getId());
                    }
                    updateSearchResultsRequestStatus();
                });
    }

    private void updateSearchResultsRequestStatus() {
        for (SearchResultAdapter.SearchResult result : searchResultsList) {
            if (requestStatusMap.containsKey(result.getUserId())) {
                result.setRequestStatus(requestStatusMap.get(result.getUserId()));
                result.setRequestId(requestIdMap.get(result.getUserId()));
            }
        }
        searchResultAdapter.notifyDataSetChanged();
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
        return phoneNumber.replaceAll("[^0-9]", "");
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

        Task<List<SearchResultAdapter.SearchResult>> firestoreContactsTask = fetchFirestoreContacts(query);
        List<SearchResultAdapter.SearchResult> deviceContacts = getDeviceContacts(query);

        firestoreContactsTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SearchResultAdapter.SearchResult> firestoreResults = task.getResult();
                Map<String, SearchResultAdapter.SearchResult> combinedResults = new HashMap<>();

                for (SearchResultAdapter.SearchResult deviceContact : deviceContacts) {
                    if (deviceContact.getPhoneNumber() != null) {
                        String last10 = getLast10Digits(deviceContact.getPhoneNumber());
                        if (!last10.isEmpty()) {
                            combinedResults.put(last10, deviceContact);
                        }
                    }
                }

                for (SearchResultAdapter.SearchResult firestoreContact : firestoreResults) {
                    if (firestoreContact.getPhoneNumber() != null) {
                        String last10 = getLast10Digits(firestoreContact.getPhoneNumber());
                        if (last10.isEmpty()) continue;

                        if (combinedResults.containsKey(last10)) {
                            SearchResultAdapter.SearchResult existingContact = combinedResults.get(last10);
                            if (firestoreContact.getDisplayName() != null && !firestoreContact.getDisplayName().isEmpty()) {
                                existingContact.setDisplayName(firestoreContact.getDisplayName());
                            }
                            existingContact.setExistingUser(true);
                            existingContact.setProfilePhotoUrl(firestoreContact.getProfilePhotoUrl());
                            existingContact.setUserId(firestoreContact.getUserId());
                        } else {
                            combinedResults.put(last10, firestoreContact);
                        }
                    }
                }

                List<SearchResultAdapter.SearchResult> finalResults = new ArrayList<>(combinedResults.values());
                searchResultsList.clear();
                searchResultsList.addAll(finalResults);
                updateSearchResultsRequestStatus();
            } else {
                Log.w(TAG, "Error getting documents from Firestore.", task.getException());
            }
        });
    }

    private Task<List<SearchResultAdapter.SearchResult>> fetchFirestoreContacts(String query) {
        CollectionReference usersRef = db.collection("users");
        Query firestoreQuery = usersRef.whereEqualTo("accountType", "google");
        final String normalizedQuery; // Make normalizedQuery final
        final String finalQuery = query; // Create a final copy of query
        try {
            normalizedQuery = normalizePhoneNumber(query);
        } catch (Exception e) {
            Log.e(TAG, "Error normalizing search query for Firestore: " + query, e);
            return Tasks.forResult(new ArrayList<>()); // Return empty list on error
        }

        return firestoreQuery.get().continueWith(task -> {
            List<SearchResultAdapter.SearchResult> firestoreResults = new ArrayList<>();
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String displayName = document.getString("displayName");
                    String mobileNumber = document.getString("mobileNumber");
                    if (mobileNumber == null || mobileNumber.isEmpty()) continue;

                    String normalizedFirestorePhoneNumber = normalizePhoneNumber(mobileNumber);

                    if ((displayName != null && displayName.toLowerCase().contains(finalQuery.toLowerCase())) || normalizedFirestorePhoneNumber.contains(normalizedQuery)) {
                        firestoreResults.add(new SearchResultAdapter.SearchResult(displayName, document.getId(), mobileNumber, document.getString("email"), true, false, document.getString("profilePhotoUrl"), "not_requested"));
                    }
                }
            }
            return firestoreResults;
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
                String normalizedDevicePhoneNumber = "";
                try {
                    normalizedDevicePhoneNumber = normalizePhoneNumber(phoneNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Error normalizing phone number: " + phoneNumber, e);
                    continue;
                }

                if ((name != null && name.toLowerCase().contains(query.toLowerCase())) || normalizedDevicePhoneNumber.contains(normalizedQuery)) {
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
            Log.d(TAG, "Invite functionality for non-existing user.");
            return;
        }

        Log.d(TAG, "onActionButtonClick: requestStatus = " + result.getRequestStatus());
        switch (result.getRequestStatus()) {
            case "not_requested":
            case "expired":
            case "Request Again":
            case "rejected":
                Log.d(TAG, "onActionButtonClick: Calling sendLocationRequest for status: " + result.getRequestStatus());
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
        Log.d(TAG, "sendLocationRequest: currentUser.getUid() = " + currentUser.getUid() + ", result.getUserId() = " + result.getUserId());
        LocationRequest newRequest = new LocationRequest(currentUser.getUid(), result.getUserId());
        db.collection("locationRequests").add(newRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(HomeActivity.this, "Location request sent.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Location request sent successfully to " + result.getUserId());
                    result.setRequestStatus("pending");
                    if (position != null) {
                        searchResultAdapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Failed to send request.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to send location request to " + result.getUserId(), e);
                });
    }

    private void fetchFrequentlyRequestedContacts() {
        if (currentUser == null) return;

        db.collection("locationRequests")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereEqualTo("status", "approved")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> frequentReceiverIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            LocationRequest request = document.toObject(LocationRequest.class);
                            if (System.currentTimeMillis() - request.getApprovedTimestamp() <= 24 * 60 * 60 * 1000) {
                                frequentReceiverIds.add(request.getToUserId());
                            }
                        }
                        fetchFrequentContactDetails(frequentReceiverIds);
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
                        frequentlyRequestedTitle.setVisibility(View.GONE);
                        frequentlyRequestedRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onFrequentContactClick(SearchResultAdapter.SearchResult result) {
        onActionButtonClick(result, -1);
    }

    @Override
    public void onItemClick(SearchResultAdapter.SearchResult result) {
        // Handle item click if needed, e.g., show user profile
        Log.d(TAG, "Clicked on search result: " + result.getDisplayName());
    }

    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        titleHome.setText(R.string.app_name);
        searchBar.setVisibility(View.VISIBLE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        frequentlyRequestedTitle.setVisibility(View.VISIBLE);
        frequentlyRequestedRecyclerView.setVisibility(View.VISIBLE);
        removeFragment();
    }

    private void showRequestsFragment() {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.requests_title);
        searchBar.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        frequentlyRequestedTitle.setVisibility(View.GONE);
        frequentlyRequestedRecyclerView.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RequestsFragment())
                .commit();
    }

    private void showProfileFragment() {
        homeContentGroup.setVisibility(View.GONE);
        titleHome.setText(R.string.profile_title);
        searchBar.setVisibility(View.GONE);
        searchResultsRecyclerView.setVisibility(View.GONE);
        frequentlyRequestedTitle.setVisibility(View.GONE);
        frequentlyRequestedRecyclerView.setVisibility(View.GONE);
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
        // Fetch the full SearchResult for the receiverId to send a new request
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
}
