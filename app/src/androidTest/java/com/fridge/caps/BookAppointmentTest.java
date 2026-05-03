package com.fridge.caps;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fridge.caps.views.activities.BookAppointmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class BookAppointmentTest {

    private Intent testIntent() {
        Intent intent = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), BookAppointmentActivity.class);
        intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, "test_counselor_001");
        intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME, "Dr. Sara Khan");
        intent.putExtra("TEST_MODE", true);
        return intent;
    }

    /**
     * Verifies testBookingScreenElementsVisible scenario.
     */
    @Test
    public void testBookingScreenElementsVisible() {
        try (ActivityScenario<BookAppointmentActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.tvCounselorName)).check(matches(withText("Dr. Sara Khan")));
            onView(withId(R.id.llDateChips)).check(matches(isDisplayed()));
            onView(withId(R.id.btnConfirm)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies testNotesFieldAcceptsInput scenario.
     */
    @Test
    public void testNotesFieldAcceptsInput() {
        try (ActivityScenario<BookAppointmentActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.etNotes))
                    .perform(typeText("I have been feeling anxious lately"), closeSoftKeyboard());
            onView(withId(R.id.etNotes)).check(matches(withText("I have been feeling anxious lately")));
        }
    }

    /**
     * Verifies testConfirmShowsValidationPath scenario.
     */
    @Test
    public void testConfirmShowsValidationPath() {
        try (ActivityScenario<BookAppointmentActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.btnConfirm)).perform(click());
            onView(withId(R.id.btnConfirm)).check(matches(isDisplayed()));
        }
    }
}
