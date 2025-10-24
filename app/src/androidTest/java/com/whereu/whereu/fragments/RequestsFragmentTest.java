package com.whereu.whereu.fragments;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.whereu.whereu.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class RequestsFragmentTest {

    @Test
    public void testRequestsFragment_displaysDummyData() {
        // Mock FirebaseUser
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getUid()).thenReturn("test_uid");

        // Mock FirebaseAuth
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        FragmentScenario<RequestsFragment> scenario = FragmentScenario.launchInContainer(RequestsFragment.class);

        scenario.onFragment(fragment -> {
            fragment.mAuth = mockAuth;
        });

        // With the current implementation, dummy data is added when the fetch fails or is empty.
        // We can check if the dummy data is displayed.
        onView(withId(R.id.recycler_view_requests))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText("John Doe"))
                ));
        onView(withId(R.id.recycler_view_requests))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText("Jane Smith"))
                ));
    }
}
