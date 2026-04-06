package com.fridge.caps;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fridge.caps.views.activities.FeedbackActivity;

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
public class FeedbackActivityTest {

    private Intent testIntent() {
        Intent intent = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), FeedbackActivity.class);
        intent.putExtra(FeedbackActivity.EXTRA_TIMESLOT_ID, "slot_test_001");
        intent.putExtra(FeedbackActivity.EXTRA_COUNSELOR_ID, "counselor_test_001");
        intent.putExtra(FeedbackActivity.EXTRA_COUNSELOR_NAME, "Dr. Sara Khan");
        intent.putExtra(FeedbackActivity.EXTRA_COUNSELOR_SPECIALIZATION, "Anxiety & Stress");
        intent.putExtra(FeedbackActivity.EXTRA_APPOINTMENT_DATE, "2026-04-05");
        intent.putExtra("TEST_MODE", true);
        return intent;
    }

    @Test
    public void testFeedbackScreenElementsVisible() {
        try (ActivityScenario<FeedbackActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.tvCounselorName)).check(matches(withText("Dr. Sara Khan")));
            onView(withId(R.id.etComment)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSubmit)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testCommentFieldAcceptsInput() {
        try (ActivityScenario<FeedbackActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.etComment))
                    .perform(typeText("Very helpful session."), closeSoftKeyboard());
            onView(withId(R.id.etComment)).check(matches(withText("Very helpful session.")));
        }
    }

    @Test
    public void testStarSelectionWorks() {
        try (ActivityScenario<FeedbackActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.star4)).perform(click());
            onView(withId(R.id.star4)).check(matches(isDisplayed()));
        }
    }
}