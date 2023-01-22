package com.aiavatar.app.feature.onboard.presentation.walkthrough

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.*
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiavatar.app.R
import org.junit.Test
import org.junit.runner.RunWith

import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*

@RunWith(AndroidJUnit4::class)
class OneTimeWalkThroughFragmentTest {

    @Test
    fun testFistRun() {
        val mockNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
        mockNavController.navInflater.inflate(R.navigation.home_nav_graph).apply {
            setStartDestination(R.id.walkthrough_fragment)
            mockNavController.setGraph(this, null)
        }

        val scenario = launchFragmentInContainer {
            WalkThroughFragment().also { fragment ->
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleObserver ->
                    viewLifecycleObserver?.lifecycle?.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {

                        }
                    })
                    if (viewLifecycleObserver != null) {
                        Navigation.setViewNavController(fragment.requireView(), mockNavController)
                    }
                }
            }
        }

        scenario.onFragment {
            onView(withId(R.id.btn_next)).perform(
                click(),
                click(),
                click()
            )
            assertThat("Verify Upload steps destination", mockNavController.currentDestination?.id, `is`(
                equalTo(R.id.upload_step_1)
            ))
        }
    }
}