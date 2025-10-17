package com.neptune.neptune

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before
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
  @Before
  fun initFirebase() {
    val app: Application = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(app).isEmpty()) {
      val opts =
          FirebaseOptions.Builder()
              .setProjectId("test-project")
              .setApplicationId("1:1234567890:android:testappid") // any non-empty
              .setApiKey("fake-api-key") // any non-empty
              .build()
      FirebaseApp.initializeApp(app, opts)
    }
  }

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
