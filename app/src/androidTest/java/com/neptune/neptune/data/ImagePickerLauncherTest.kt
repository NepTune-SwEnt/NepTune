package com.neptune.neptune.data

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import org.junit.After
import org.junit.Before
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

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeImageUri: Uri

  @Before
  fun setUp() {
    Intents.init()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val tempFile = File.createTempFile("test_image", ".jpg", context.cacheDir)
    fakeImageUri = Uri.fromFile(tempFile)
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  /** Tests that the launcher can be created and that launching it does not crash. */
  @Test
  fun launcherCanBeCreatedAndLaunchedWithoutCrashing() {
    var launcher: ActivityResultLauncher<String>? = null
    composeTestRule.setContent {
      launcher = rememberImagePickerLauncher(onImageCropped = {})
      Button(onClick = { launcher?.launch("image/*") }) { Text("Launch Picker") }
    }

    composeTestRule.onNodeWithText("Launch Picker").performClick()

    assertThat(launcher).isNotNull()
  }

  /** Tests the full success flow: picking an image and successfully cropping it. */
  @Test
  fun launcherSuccessFlowReturnsCroppedUri() {
    val pickerResult =
        Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(fakeImageUri))
    Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(pickerResult)

    val uCropResultIntent = Intent().putExtra(UCrop.EXTRA_OUTPUT_URI, fakeImageUri)
    val uCropResult = Instrumentation.ActivityResult(Activity.RESULT_OK, uCropResultIntent)
    Intents.intending(hasComponent(UCropActivity::class.java.name)).respondWith(uCropResult)

    composeTestRule.setContent {
      val receivedUri = remember { mutableStateOf<Uri?>(Uri.EMPTY) }
      val launcher =
          rememberImagePickerLauncher(onImageCropped = { uri -> receivedUri.value = uri })
      Button(onClick = { launcher.launch("image/*") }) { Text("Launch Picker") }
      // The received URI has a cache-busting timestamp, so we clear it for a stable assertion.
      val testableUri = receivedUri.value?.buildUpon()?.clearQuery()?.build()
      Text("Result: ${testableUri}")
    }

    composeTestRule.onNodeWithText("Launch Picker").performClick()

    composeTestRule.onNodeWithText("Result: $fakeImageUri").assertExists()
  }

  /** Tests the flow where the user cancels the image picker. */
  @Test
  fun launcherWhenPickerCanceledReturnsNull() {
    val pickerResult = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
    Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(pickerResult)

    composeTestRule.setContent {
      val receivedUri = remember { mutableStateOf<Uri?>(Uri.EMPTY) }
      val launcher =
          rememberImagePickerLauncher(onImageCropped = { uri -> receivedUri.value = uri })
      Button(onClick = { launcher.launch("image/*") }) { Text("Launch Picker") }
      Text("Result: ${receivedUri.value}")
    }

    composeTestRule.onNodeWithText("Launch Picker").performClick()

    composeTestRule.onNodeWithText("Result: null").assertExists()
  }

  /** Tests the flow where the uCrop activity fails or is canceled. */
  @Test
  fun launcherWhenUCropFailsReturnsNull() {
    val pickerResult =
        Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(fakeImageUri))
    Intents.intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(pickerResult)

    val uCropResult = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
    Intents.intending(hasComponent(UCropActivity::class.java.name)).respondWith(uCropResult)

    composeTestRule.setContent {
      val receivedUri = remember { mutableStateOf<Uri?>(Uri.EMPTY) }
      val launcher =
          rememberImagePickerLauncher(onImageCropped = { uri -> receivedUri.value = uri })
      Button(onClick = { launcher.launch("image/*") }) { Text("Launch Picker") }
      Text("Result: ${receivedUri.value}")
    }

    composeTestRule.onNodeWithText("Launch Picker").performClick()

    composeTestRule.onNodeWithText("Result: null").assertExists()
  }
}
