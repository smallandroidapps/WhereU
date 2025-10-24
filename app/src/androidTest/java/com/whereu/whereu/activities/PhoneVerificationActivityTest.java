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
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.whereu.whereu.R;

@RunWith(AndroidJUnit4.class)
public class PhoneVerificationActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSendVerificationCode_withEmptyPhoneNumber_showsToast() {
        ActivityScenario.launch(PhoneVerificationActivity.class);
        onView(withId(R.id.button_send_verification_code)).perform(click());
        onView(withText("Please enter a valid phone number")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void testVerifyPhoneNumber_withoutSendingCode_showsToast() {
        ActivityScenario.launch(PhoneVerificationActivity.class);
        onView(withId(R.id.edit_text_verification_code)).perform(typeText("123456"));
        onView(withId(R.id.button_verify_phone_number)).perform(click());
        onView(withText("Please send a verification code first")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void testVerifyPhoneNumber_withEmptyCode_showsToast() {
        ActivityScenario<PhoneVerificationActivity> scenario = ActivityScenario.launch(PhoneVerificationActivity.class);
        scenario.onActivity(activity -> {
            // Manually set verificationId to simulate that a code has been sent
            activity.mVerificationId = "mock-verification-id";
        });

        onView(withId(R.id.button_verify_phone_number)).perform(click());
        onView(withText("Please enter the verification code")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }

    @Test
    public void testVerifyPhoneNumber_withInvalidCode_showsErrorToast() {
        ActivityScenario<PhoneVerificationActivity> scenario = ActivityScenario.launch(PhoneVerificationActivity.class);
        scenario.onActivity(activity -> {
            // Manually set verificationId to simulate that a code has been sent
            activity.mVerificationId = "mock-verification-id";
        });

        onView(withId(R.id.edit_text_verification_code)).perform(typeText("111111"));
        onView(withId(R.id.button_verify_phone_number)).perform(click());

        // This test relies on Firebase Auth returning an error.
        // In a real app, you would mock this response. For now, we check for the toast.
        // The actual message depends on the exception, which is FirebaseAuthInvalidCredentialsException
        // The activity shows "Invalid verification code. Please try again."
        onView(withText("Invalid verification code. Please try again.")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
    }
}