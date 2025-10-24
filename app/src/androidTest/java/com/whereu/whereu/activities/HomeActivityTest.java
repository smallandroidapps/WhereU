package com.whereu.whereu.activities;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import com.whereu.whereu.R;

@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSearch_withValidQuery_displaysResults() {
        ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(HomeActivity.class);

        onView(withId(R.id.search_bar)).perform(typeText("test"));

        // In a real app, you would mock the data from Firestore and your contacts to verify the search results.
        // For now, we are just checking that the recycler view is displayed.
        onView(withId(R.id.search_results_recycler_view)).check(matches(isDisplayed()));
    }
}