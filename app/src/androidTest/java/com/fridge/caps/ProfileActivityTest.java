package com.fridge.caps;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fridge.caps.views.activities.EditProfileActivity;
import com.fridge.caps.views.activities.LoginActivity;
import com.fridge.caps.views.activities.ProfileActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private Intent testIntent() {
        Intent intent = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), ProfileActivity.class);
        intent.putExtra("TEST_MODE", true);
        return intent;
    }

    /**
     * Verifies testProfileElementsVisible scenario.
     */
    @Test
    public void testProfileElementsVisible() {
        try (ActivityScenario<ProfileActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.tvUsername)).check(matches(isDisplayed()));
            onView(withId(R.id.tvEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStatTotal)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStatUpcoming)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStatCancelled)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies testEditButtonNavigatesToEditProfile scenario.
     */
    @Test
    public void testEditButtonNavigatesToEditProfile() {
        try (ActivityScenario<ProfileActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.btnEditProfile)).perform(click());
            intended(hasComponent(EditProfileActivity.class.getName()));
        }
    }

    /**
     * Verifies testSignOutNavigatesToLogin scenario.
     */
    @Test
    public void testSignOutNavigatesToLogin() {
        try (ActivityScenario<ProfileActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.btnSignOut)).perform(click());
            intended(hasComponent(LoginActivity.class.getName()));
        }
    }
}
