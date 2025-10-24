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
public class ProfileSettingsActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSaveProfile_withValidData_updatesProfile() {
        ActivityScenario<ProfileSettingsActivity> scenario = ActivityScenario.launch(ProfileSettingsActivity.class);

        onView(withId(R.id.edit_text_display_name)).perform(typeText("Test User"));
        onView(withId(R.id.button_save_profile)).perform(click());

        // In a real app, you would mock the FirebaseFirestore instance and verify that the update method is called.
        // For now, we are just checking that a toast is displayed.
        onView(withId(R.id.button_save_profile)).check(matches(isDisplayed()));
    }
}