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

import com.fridge.caps.views.activities.LoginActivity;
import com.fridge.caps.views.activities.RegisterActivity;

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
public class LoginActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Verifies testLoginScreenElementsVisible scenario.
     */
    @Test
    public void testLoginScreenElementsVisible() {
        Intent i = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), LoginActivity.class);
        i.putExtra("TEST_MODE", true);
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(i)) {
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
            onView(withId(R.id.tvRegisterLink)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies testRegisterLinkNavigatesToRegister scenario.
     */
    @Test
    public void testRegisterLinkNavigatesToRegister() {
        Intent i = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), LoginActivity.class);
        i.putExtra("TEST_MODE", true);
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(i)) {
            onView(withId(R.id.tvRegisterLink)).perform(click());
            intended(hasComponent(RegisterActivity.class.getName()));
        }
    }
}
