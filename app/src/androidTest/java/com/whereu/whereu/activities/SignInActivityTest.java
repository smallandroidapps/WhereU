package com.whereu.whereu.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whereu.whereu.R;

@RunWith(AndroidJUnit4.class)
public class SignInActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSignInButton_launchesGoogleSignIn() {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        onView(withId(R.id.sign_in_button)).perform(click());

        intended(hasComponent("com.google.android.gms.auth.api.signin.internal.SignInHubActivity"));
    }

    @Test
    public void testGoogleSignIn_successful() {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        Intent resultData = new Intent();
        GoogleSignInAccount account = mock(GoogleSignInAccount.class);
        resultData.putExtra("googleSignInAccount", account);
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

        intending(not(isInternal())).respondWith(result);

        onView(withId(R.id.sign_in_button)).perform(click());

        intended(hasComponent(HomeActivity.class.getName()));
    }

    @Test
    public void testGoogleSignIn_failed() {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);

        intending(not(isInternal())).respondWith(result);

        onView(withId(R.id.sign_in_button)).perform(click());

        // You can check that the HomeActivity is not launched, or that a toast is shown.
        // For now, we will just check that the app doesn't crash.
    }

    @Test
    public void testPhoneSignInButton_launchesPhoneVerificationActivity() {
        ActivityScenario<SignInActivity> scenario = ActivityScenario.launch(SignInActivity.class);

        onView(withId(R.id.button_phone_sign_in)).perform(click());

        intended(hasComponent(PhoneVerificationActivity.class.getName()));
    }
}
