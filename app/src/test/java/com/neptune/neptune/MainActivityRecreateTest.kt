package com.neptune.neptune

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/*
    A test to verify that MainActivity survives configuration changes (recreation)
    Partially written with ChatGPT
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityRecreateTest {

  @Test
  fun survives_configuration_change_recreate() {
    val controller =
        Robolectric.buildActivity(MainActivity::class.java).setup() // create -> start -> resume
    val activity = controller.get()
    assertThat(activity).isNotNull()

    controller.recreate() // simulates config change (rotation)
    val recreated = controller.get()
    assertThat(recreated).isNotNull()
    // UI root still present after recreation
    assertThat(recreated.window.decorView).isNotNull()
  }
}
