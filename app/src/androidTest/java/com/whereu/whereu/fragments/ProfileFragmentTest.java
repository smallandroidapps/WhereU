package com.whereu.whereu.fragments;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whereu.whereu.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testUserProfile_isDisplayed() {
        // Mock FirebaseUser
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getUid()).thenReturn("test_uid");
        when(mockUser.getDisplayName()).thenReturn("Test User");
        when(mockUser.getEmail()).thenReturn("test@example.com");

        // Mock FirebaseAuth
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        // Launch the fragment
        FragmentScenario<ProfileFragment> scenario = FragmentScenario.launchInContainer(ProfileFragment.class);

        scenario.onFragment(fragment -> {
            fragment.mAuth = mockAuth;
        });

        // In a real app, you would also mock the Firestore database to return a user profile.
        // For now, we just check that the user's name and email are displayed from the mock user.
        onView(withId(R.id.user_name)).check(matches(withText("Test User")));
        onView(withId(R.id.user_email)).check(matches(withText("test@example.com")));
    }
}
