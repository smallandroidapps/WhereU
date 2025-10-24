package com.whereu.whereu.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.content.SharedPreferences;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.whereu.whereu.R;
import com.whereu.whereu.databinding.ActivityHomeBinding;
import com.whereu.whereu.fragments.ProfileFragment;
import com.whereu.whereu.fragments.RequestsFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whereu.whereu.models.LocationRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import android.util.Log;
import android.annotation.SuppressLint;
import android.widget.Switch;
import android.widget.Button;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Tasks;
import android.content.ContentResolver;

public class HomeActivity extends AppCompatActivity implements SearchResultAdapter.OnItemClickListener {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final String TAG = "HomeActivity";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final String KEY_CACHED_SEARCH_RESULTS = "cached_search_results";

    private ActivityHomeBinding binding;
    private Group homeContentGroup;
    private TextView titleHome;
    private EditText searchBar;
    private RecyclerView searchResultsRecyclerView;
    private SearchResultAdapter searchResultAdapter;
    private List<SearchResultAdapter.SearchResult> searchResultsList;
    private RecyclerView recentContactsRecyclerView;
    private FirebaseFirestore db;
    private RecyclerView suggestionsRecyclerView;
    private SearchResultAdapter suggestionsAdapter;
    private List<SearchResultAdapter.SearchResult> suggestionsList;
    private RecyclerView suggestedContactsCarousel;
    private SearchResultAdapter suggestedContactsAdapter;
    private List<SearchResultAdapter.SearchResult> suggestedContactsList;
    private Switch toggleMyContactsOnly;
    private Button requestLocationButton;
    private SearchResultAdapter.SearchResult selectedSearchResult;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        homeContentGroup = binding.homeContentGroup;
        titleHome = binding.titleHome;
        searchBar = binding.searchBar;
        searchResultsRecyclerView = binding.searchResultsRecyclerView;
        recentContactsRecyclerView = binding.recentContactsRecyclerView;
        suggestionsRecyclerView = binding.suggestionsRecyclerView;

        searchResultsList = new ArrayList<>();
        searchResultAdapter = new SearchResultAdapter(searchResultsList, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchResultAdapter);

        suggestionsList = new ArrayList<>();
        suggestionsAdapter = new SearchResultAdapter(suggestionsList, this);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);

        requestLocationButton = binding.requestLocationButton;
        requestLocationButton.setVisibility(View.GONE);
        requestLocationButton.setOnClickListener(v -> {
            if (selectedSearchResult != null) {
                sendLocationRequest(selectedSearchResult.getUserId());
            }
        });

        suggestedContactsCarousel = binding.suggestedContactsCarousel;
        suggestedContactsCarousel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        suggestedContactsList = new ArrayList<>();
        suggestedContactsAdapter = new SearchResultAdapter(suggestedContactsList, this);
        suggestedContactsCarousel.setAdapter(suggestedContactsAdapter);

        toggleMyContactsOnly = binding.toggleMyContactsOnly;
        toggleMyContactsOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            performSearch(searchBar.getText().toString());
        });

        fetchSuggestedContacts();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    recentContactsRecyclerView.setVisibility(View.GONE);
                    suggestionsRecyclerView.setVisibility(View.GONE);
                    checkContactsPermissionAndPerformSearch(s.toString());
                } else {
                    searchResultsRecyclerView.setVisibility(View.GONE);
                    recentContactsRecyclerView.setVisibility(View.VISIBLE);
                    suggestionsRecyclerView.setVisibility(View.VISIBLE);
                    // Clear search results
                    searchResultsList.clear();
                    searchResultAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        BottomNavigationView bottomNavigationView = binding.bottomNavigationBar;
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

        // Set default view
        showHomeContent();
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
                // Permission granted, perform search
                performSearch(searchBar.getText().toString());
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
                searchResultsRecyclerView.setVisibility(View.GONE);
                recentContactsRecyclerView.setVisibility(View.VISIBLE);
                searchResultsList.clear();
                searchResultAdapter.notifyDataSetChanged();
            }
        }
    }

    private void showSuggestions() {
        suggestionsList.clear();
        Set<String> recentSearches = getRecentSearches();
        for (String search : recentSearches) {
            suggestionsList.add(new SearchResultAdapter.SearchResult(search, "", "", "", false, false, ""));
        }
        suggestionsAdapter.notifyDataSetChanged();
    }

    private Task<List<SearchResultAdapter.SearchResult>> fetchFirestoreContacts(String query, boolean myContactsOnly) {
        if (currentUser == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        CollectionReference usersRef = db.collection("users");
        Query firestoreQuery = usersRef.whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff");

        return firestoreQuery.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                List<SearchResultAdapter.SearchResult> firestoreResults = new ArrayList<>();
                List<Task<Void>> contactCheckTasks = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String userId = document.getId();
                    String displayName = document.getString("displayName");
                    String email = document.getString("email");
                    String phoneNumber = document.getString("phoneNumber"); // Assuming phoneNumber field exists
                    String profilePhotoUrl = document.getString("profilePhotoUrl"); // Assuming profilePhotoUrl field exists

                    if (myContactsOnly) {
                        Task<Void> contactCheckTask = db.collection("users").document(currentUser.getUid())
                                .collection("contacts").document(userId).get()
                                .continueWith(contactTask -> {
                                    if (contactTask.isSuccessful() && contactTask.getResult().exists()) {
                                        firestoreResults.add(new SearchResultAdapter.SearchResult(displayName, userId, phoneNumber, email, true, false, profilePhotoUrl));
                                    }
                                    return null;
                                });
                        contactCheckTasks.add(contactCheckTask);
                    } else {
                        firestoreResults.add(new SearchResultAdapter.SearchResult(displayName, userId, phoneNumber, email, true, false, profilePhotoUrl));
                    }
                }
                return Tasks.whenAllComplete(contactCheckTasks).continueWith(allContactsTask -> firestoreResults);
            } else {
                Log.w(TAG, "Error getting documents from Firestore.", task.getException());
                return Tasks.forException(task.getException());
            }
        });
    }

    private void updateSearchResults(List<SearchResultAdapter.SearchResult> newResults) {
        // This method will be called to update the main searchResultsList
        // and notify the adapter. It handles potential duplicates.
        for (SearchResultAdapter.SearchResult newResult : newResults) {
            boolean found = false;
            for (SearchResultAdapter.SearchResult existingResult : searchResultsList) {
                if (existingResult.getUserId().equals(newResult.getUserId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                searchResultsList.add(newResult);
            }
        }
        searchResultsList.sort((r1, r2) -> r1.getDisplayName().compareToIgnoreCase(r2.getDisplayName()));
        searchResultAdapter.notifyDataSetChanged();
        if (!searchResultsList.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            suggestionsRecyclerView.setVisibility(View.GONE);
            recentContactsRecyclerView.setVisibility(View.GONE);
        } else {
            if (!suggestionsList.isEmpty()) {
                suggestionsRecyclerView.setVisibility(View.VISIBLE);
                searchResultsRecyclerView.setVisibility(View.GONE);
                recentContactsRecyclerView.setVisibility(View.GONE);
            } else {
                recentContactsRecyclerView.setVisibility(View.VISIBLE);
                searchResultsRecyclerView.setVisibility(View.GONE);
                suggestionsRecyclerView.setVisibility(View.GONE);
            }
        }
    }

    private List<SearchResultAdapter.SearchResult> getDeviceContacts(String query) {
        List<SearchResultAdapter.SearchResult> deviceContacts = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Email.ADDRESS},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " LIKE ? OR " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ? OR " + ContactsContract.CommonDataKinds.Email.ADDRESS + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%", "%" + query + "%"},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                @SuppressLint("Range") String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                // For device contacts, we don't have a userId or email directly, so we'll use phoneNumber as a unique identifier for now
                deviceContacts.add(new SearchResultAdapter.SearchResult(name, phoneNumber, phoneNumber, email != null ? email : "", false, false, ""));
            }
            cursor.close();
        }
        return deviceContacts;
    }

    private void saveRecentSearch(String query) {
        Set<String> recentSearches = getRecentSearches();
        recentSearches.add(query);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_RECENT_SEARCHES, recentSearches)
                .apply();
    }

    private Set<String> getRecentSearches() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getStringSet(KEY_RECENT_SEARCHES, new HashSet<>());
    }

    private void fetchFirestoreContacts(String query) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String displayName = document.getString("displayName");
                            String phoneNumber = document.getString("phoneNumber");
                            String userId = document.getId();
                            String profilePhotoUrl = document.getString("profilePhotoUrl");
                            searchResultsList.add(new SearchResultAdapter.SearchResult(displayName, userId, "", "", true, false, profilePhotoUrl));
                        }
                        searchResultAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(HomeActivity.this, "Error getting Firestore contacts", Toast.LENGTH_SHORT).show();
                    }
                });

        db.collection("users")
                .whereGreaterThanOrEqualTo("phoneNumber", query)
                .whereLessThanOrEqualTo("phoneNumber", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String displayName = document.getString("displayName");
                            String phoneNumber = document.getString("phoneNumber");
                            String userId = document.getId();
                            String profilePhotoUrl = document.getString("profilePhotoUrl");
                            // Avoid duplicates if already added by displayName search
                            boolean exists = false;
                            for (SearchResultAdapter.SearchResult result : searchResultsList) {
                                if (result.getUserId().equals(userId)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                searchResultsList.add(new SearchResultAdapter.SearchResult(displayName, userId, "", "", true, false, profilePhotoUrl));
                            }
                        }
                        searchResultAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(HomeActivity.this, "Error getting Firestore contacts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchSuggestedContacts() {
        if (currentUser == null) {
            return;
        }

        db.collection("users")
                .limit(5) // Limit to 5 suggested contacts for the carousel
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        suggestedContactsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getId();
                            if (!userId.equals(currentUser.getUid())) { // Don't suggest current user
                                String displayName = document.getString("displayName");
                                String email = document.getString("email");
                                String phoneNumber = document.getString("phoneNumber");
                                String profilePhotoUrl = document.getString("profilePhotoUrl");
                                suggestedContactsList.add(new SearchResultAdapter.SearchResult(displayName, userId, phoneNumber, email, true, false, profilePhotoUrl));
                            }
                        }
                        suggestedContactsAdapter.notifyDataSetChanged();
                        if (!suggestedContactsList.isEmpty()) {
                            suggestedContactsCarousel.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private void performSearch(String query) {
        final String currentUserEmail = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getEmail() : null;

        if (query.isEmpty()) {
            searchResultsList.clear();
            searchResultAdapter.notifyDataSetChanged();
            return;
        }

        List<SearchResultAdapter.SearchResult> cachedResults = getCachedSearchResults(query);
        if (!cachedResults.isEmpty()) {
            searchResultsList.clear();
            searchResultsList.addAll(cachedResults);
            searchResultAdapter.notifyDataSetChanged();
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            suggestionsRecyclerView.setVisibility(View.GONE);
            recentContactsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Clear previous search results
        searchResultsList.clear();
        searchResultAdapter.notifyDataSetChanged();

        List<SearchResultAdapter.SearchResult> deviceContacts = getDeviceContacts(query);
        Task<List<SearchResultAdapter.SearchResult>> firestoreContactsTask = fetchFirestoreContacts(query, toggleMyContactsOnly.isChecked());

        firestoreContactsTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SearchResultAdapter.SearchResult> combinedResults = new ArrayList<>();
                combinedResults.addAll(deviceContacts);
                combinedResults.addAll(task.getResult());

                // Apply myContactsOnly filter to combined results if toggle is checked
                if (toggleMyContactsOnly.isChecked()) {
                    // Fetch all WherU user phone numbers and emails to cross-reference with device contacts
                    db.collection("users").get().addOnCompleteListener(wheruUsersTask -> {
                        if (wheruUsersTask.isSuccessful()) {
                            Set<String> wheruUserPhoneNumbers = new HashSet<>();
                            Set<String> wheruUserEmails = new HashSet<>();
                            for (QueryDocumentSnapshot document : wheruUsersTask.getResult()) {
                                String phoneNumber = document.getString("phoneNumber");
                                String email = document.getString("email");
                                if (phoneNumber != null) wheruUserPhoneNumbers.add(phoneNumber);
                                if (email != null) wheruUserEmails.add(email);
                            }

                            List<SearchResultAdapter.SearchResult> filteredResults = new ArrayList<>();
                            for (SearchResultAdapter.SearchResult result : combinedResults) {
                                // If it's a Firestore result, it's already filtered by myContactsOnly in fetchFirestoreContacts
                                // If it's a device contact, check if its phone number or email exists in WherU users
                                if (result.isExistingUser() || // Already a WherU user from Firestore
                                        (wheruUserPhoneNumbers.contains(result.getPhoneNumber()) || wheruUserEmails.contains(result.getEmail()))) {
                                    filteredResults.add(result);
                                }
                            }
                            updateSearchResults(filteredResults, currentUserEmail);
                            saveSearchResultsToCache(query, filteredResults);
                        } else {
                            Log.w(TAG, "Error fetching WherU users for contact filtering.", wheruUsersTask.getException());
                            // If fetching WherU users fails, proceed without filtering device contacts
                            updateSearchResults(combinedResults);
                            saveSearchResultsToCache(query, combinedResults);
                        }
                    });
                } else {
                    updateSearchResults(combinedResults);
                    saveSearchResultsToCache(query, combinedResults);
                }
            } else {
                Log.w(TAG, "Error combining search results.", task.getException());
                // Even if Firestore fails, update with device contacts
                updateSearchResults(deviceContacts);
                saveSearchResultsToCache(query, deviceContacts);
            }
            saveRecentSearch(query);
        });
    }

    private void showSuggestions(String query) {
        suggestionsList.clear();
        Set<String> recentSearches = getRecentSearches();
        for (String search : recentSearches) {
            suggestionsList.add(new SearchResultAdapter.SearchResult(search, "", "", "", false, false, ""));
        }
        suggestionsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestLocationClick(SearchResultAdapter.SearchResult result) {
        // Handle request location click
        sendLocationRequest(result.getUserId());
        showSuggestions("");
    }

    private void sendLocationRequest(String targetUserId) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderUserId = currentUser.getUid();
        String senderDisplayName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Unknown User";

        // Create a new location request document in Firestore
        // You might want to add more details like timestamp, status, etc.
        db.collection("locationRequests")
                .add(new LocationRequest(senderUserId, senderDisplayName, targetUserId))
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(HomeActivity.this, "Location request sent to " + targetUserId, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Failed to send location request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onInviteClick(SearchResultAdapter.SearchResult result) {
        // Handle invite click
        sendInvite(result.getDisplayName(), result.getPhoneNumber()); // Assuming userId is the phone number for non-users
    }

    private void sendInvite(String displayName, String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            sendSmsMessage(displayName, phoneNumber);
        }
    }

    private void sendSmsMessage(String displayName, String phoneNumber) {
        String message = "Hi, I'm using WhereU to share my location. Join me! Download WhereU from [Link to app store]";
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "Invitation sent to " + displayName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(SearchResultAdapter.SearchResult result) {
        selectedSearchResult = result; // Store the selected result
        requestLocationButton.setVisibility(View.VISIBLE); // Make the button visible
        searchBar.setText(result.getDisplayName());
        searchBar.setSelection(searchBar.getText().length()); // Put cursor at the end
        performSearch(result.getDisplayName());

        if (result.isCurrentUserEmail()) {
            sendLocationRequest(result.getUserId());
        }
    }

    private void showHomeContent() {
        homeContentGroup.setVisibility(View.VISIBLE);
        binding.fragmentContainer.setVisibility(View.GONE);
        titleHome.setText("Home");
        removeFragment();
    }

    private void showRequestsFragment() {
        homeContentGroup.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        titleHome.setText("Requests");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RequestsFragment())
                .commit();
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

    private void saveSearchResultsToCache(String query, List<SearchResultAdapter.SearchResult> results) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(results);
        editor.putString(KEY_CACHED_SEARCH_RESULTS + "_" + query, json);
        editor.apply();
    }

    private List<SearchResultAdapter.SearchResult> getCachedSearchResults(String query) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_CACHED_SEARCH_RESULTS + "_" + query, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<SearchResultAdapter.SearchResult>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void clearSearchResultsCache() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Iterate through all keys and remove those starting with KEY_CACHED_SEARCH_RESULTS
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith(KEY_CACHED_SEARCH_RESULTS)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }


    private void updateSearchResults(List<SearchResultAdapter.SearchResult> results, String currentUserEmail) {
        searchResultsList.clear();
        for (SearchResultAdapter.SearchResult result : results) {
            if (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(result.getEmail())) {
                // Create a new SearchResult with the isCurrentUserEmail flag set
                searchResultsList.add(new SearchResultAdapter.SearchResult(
                        result.getDisplayName(),
                        result.getUserId(),
                        result.getPhoneNumber(),
                        result.getEmail(),
                        result.isExistingUser(),
                        true, // Set isCurrentUserEmail to true
                        result.getProfilePhotoUrl()
                ));
            } else {
                searchResultsList.add(result);
            }
        }
        searchResultAdapter.notifyDataSetChanged();
    }
}