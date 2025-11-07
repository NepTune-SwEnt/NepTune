package com.neptune.neptune.data

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the [rememberImagePickerLauncher] Composable.
 *
 * These tests run on an Android device or emulator to verify the behavior of the composable that
 * interacts with Android framework features like ActivityResultLaunchers.
 *
 * These tests were created using AI assistance.
 */
@RunWith(AndroidJUnit4::class)
class ImagePickerLauncherTest {

  // The ComposeTestRule provides a testing environment for composables.
  @get:Rule val composeTestRule = createComposeRule()

  /** Tests that the launcher can be created and that launching it does not crash. */
  @Test
  fun launcherCanBeCreatedAndLaunchedWithoutCrashing() {
    // Arrange: A variable to hold the launcher instance.
    var launcher: ActivityResultLauncher<String>? = null

    composeTestRule.setContent {
      // State must be declared within a @Composable context.
      val receivedUri = remember { mutableStateOf<Uri?>(null) }

      // Create the launcher and store a reference to it.
      launcher = rememberImagePickerLauncher { uri ->
        // This callback would be executed after image selection and cropping.
        receivedUri.value = uri
      }

      // A simple button to trigger the launcher.
      Button(onClick = { launcher?.launch("image/*") }) { Text("Launch Picker") }
    }

    // Act: Simulate a user clicking the button to launch the image picker.
    composeTestRule.onNodeWithText("Launch Picker").performClick()

    // Assert: The most crucial check here is that the performClick() action above did not
    // crash the application, which confirms the launcher is wired up correctly.
    // We also check that the launcher object itself was successfully created.
    assertThat(launcher).isNotNull()
  }
}
