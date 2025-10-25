package com.whereu.whereu.activities;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import android.content.ContentResolver;
import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.whereu.whereu.R;
import com.whereu.whereu.databinding.ActivityHomeBinding;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.wheru.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "app/src/main/AndroidManifest.xml", resourceDir = "app/src/main/res", sdk = Build.VERSION_CODES.R, application = android.app.Application.class)
public class HomeActivityTest {

    @Mock
    private Context mockContext;
    @Mock
    private ContentResolver mockContentResolver;
    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private FirebaseAuth mockFirebaseAuth;
    @Mock
    private FirebaseUser mockFirebaseUser;
    @Mock
    private CollectionReference mockCollectionReference;

    private HomeActivity homeActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        when(mockFirebaseAuth.getCurrentUser()).thenReturn(mockFirebaseUser);
        when(mockFirebaseUser.getUid()).thenReturn("testUserId");
        when(mockFirestore.collection(anyString())).thenReturn(mockCollectionReference);

        homeActivity = Robolectric.buildActivity(HomeActivity.class).create().get();

        homeActivity.db = mockFirestore;
        homeActivity.currentUser = mockFirebaseAuth.getCurrentUser();
        homeActivity.searchResultsList = new ArrayList<>();
        homeActivity.searchResultAdapter = mock(SearchResultAdapter.class);
    }

    private List<SearchResultAdapter.SearchResult> createMockDeviceContacts(String name, String number, String contactId) {
        List<SearchResultAdapter.SearchResult> contacts = new ArrayList<>();
        contacts.add(new SearchResultAdapter.SearchResult(name, null, number, null, false, false, null, "not_requested"));
        return contacts;
    }

    @Test
    public void testPerformSearch_deviceContactFound_firestoreUserFound() {
        // Mock device contacts
        when(homeActivity.getDeviceContacts(anyString()))
                .thenReturn(createMockDeviceContacts("John Doe", "1234567890", "1"));

        // Mock Firestore user
        User firestoreUser = new User("firestoreUserId", "John Doe", "john.doe@example.com", "1234567890", "photoUrl", "phone", null, null);
        DocumentSnapshot mockDocumentSnapshot = mock(DocumentSnapshot.class);
        when(mockDocumentSnapshot.exists()).thenReturn(true);
        when(mockDocumentSnapshot.toObject(User.class)).thenReturn(firestoreUser);

        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.singletonList(mockDocumentSnapshot));

        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockCollectionReference.whereIn(anyString(), anyList())).thenReturn(mockCollectionReference);
        when(mockCollectionReference.get()).thenReturn(mockTask);

        // Call the method under test
        homeActivity.performSearch("John Doe");

        // Verify results
        verify(homeActivity.searchResultAdapter, times(1)).notifyDataSetChanged();
        assertTrue(homeActivity.searchResultsList.size() == 1);
        assertTrue(homeActivity.searchResultsList.get(0).isExistingUser());
        assertTrue(homeActivity.searchResultsList.get(0).getUserId().equals("firestoreUserId"));
    }

    @Test
    public void testPerformSearch_noDeviceContactFound() {
        // Mock no device contacts
        when(homeActivity.getDeviceContacts(anyString()))
                .thenReturn(new ArrayList<>());

        // Call the method under test
        homeActivity.performSearch("NonExistent");

        // Verify results
        verify(homeActivity.searchResultAdapter, times(1)).notifyDataSetChanged();
        assertTrue(homeActivity.searchResultsList.isEmpty());
    }

    @Test
    public void testPerformSearch_deviceContactFound_noFirestoreUserFound() {
        // Mock device contacts
        when(homeActivity.getDeviceContacts(anyString()))
                .thenReturn(createMockDeviceContacts("Jane Doe", "0987654321", "2"));

        // Mock no Firestore user
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        when(mockQuerySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockCollectionReference.whereIn(anyString(), anyList())).thenReturn(mockCollectionReference);
        when(mockCollectionReference.get()).thenReturn(mockTask);

        // Call the method under test
        homeActivity.performSearch("Jane Doe");

        // Verify results
        verify(homeActivity.searchResultAdapter, times(1)).notifyDataSetChanged();
        assertTrue(homeActivity.searchResultsList.size() == 1);
        assertTrue(!homeActivity.searchResultsList.get(0).isExistingUser());
    }
}