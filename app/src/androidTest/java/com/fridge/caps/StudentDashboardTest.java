package com.fridge.caps;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fridge.caps.views.activities.AppointmentsActivity;
import com.fridge.caps.views.activities.CounselorListActivity;
import com.fridge.caps.views.activities.NotificationsActivity;
import com.fridge.caps.views.activities.ProfileActivity;
import com.fridge.caps.views.activities.StudentDashboardActivity;

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
public class StudentDashboardTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    private Intent testIntent() {
        Intent i = new Intent(androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().getTargetContext(), StudentDashboardActivity.class);
        i.putExtra("TEST_MODE", true);
        return i;
    }

    @Test
    public void testDashboardElementsVisible() {
        try (ActivityScenario<StudentDashboardActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.tvWelcome)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBookAppointment)).check(matches(isDisplayed()));
            onView(withId(R.id.btnNotifications)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBookAppointmentNavigates() {
        try (ActivityScenario<StudentDashboardActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.btnBookAppointment)).perform(click());
            intended(hasComponent(CounselorListActivity.class.getName()));
        }
    }

    @Test
    public void testBottomNavProfileNavigates() {
        try (ActivityScenario<StudentDashboardActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.navProfile)).perform(click());
            intended(hasComponent(ProfileActivity.class.getName()));
        }
    }

    @Test
    public void testBottomNavAlertsNavigates() {
        try (ActivityScenario<StudentDashboardActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.navAlerts)).perform(click());
            intended(hasComponent(NotificationsActivity.class.getName()));
        }
    }

    @Test
    public void testHistoryButtonNavigates() {
        try (ActivityScenario<StudentDashboardActivity> ignored = ActivityScenario.launch(testIntent())) {
            onView(withId(R.id.btnHistory)).perform(click());
            intended(hasComponent(AppointmentsActivity.class.getName()));
        }
    }
}
