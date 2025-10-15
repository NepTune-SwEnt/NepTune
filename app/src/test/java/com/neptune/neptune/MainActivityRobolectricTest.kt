package com.neptune.neptune

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// A basic test to verify that MainActivity launches via Robolectric.
//written partially with ChatGPT
@RunWith(RobolectricTestRunner::class)
class MainActivityRobolectricTest {

    @Test
    fun launches_and_sets_content() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create().start().resume().visible()
        val activity = controller.get()
        assertThat(activity).isNotNull()
        // simple sanity: theme context exists, content view is not null
        assertThat(activity.window.decorView).isNotNull()
    }
}
