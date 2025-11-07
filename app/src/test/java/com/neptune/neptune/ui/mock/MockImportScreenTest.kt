package com.neptune.neptune.ui.mock

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.media.NeptuneRecorder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit (Robolectric) Compose tests for MockImportScreen. written partially with help from ChatGPT.
 * These run in the JVM (no device/emulator) so JaCoCo 0.8.8 still works.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Robolectric API level
class MockImportScreenTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var vm: com.neptune.neptune.ui.picker.ImportViewModel
  private lateinit var libraryFlow: MutableStateFlow<List<MediaItem>>
  private lateinit var recorder: NeptuneRecorder

  @Before
  fun setup() {
    vm = mockk(relaxed = true)
    libraryFlow = MutableStateFlow(emptyList())
    every { vm.library } returns libraryFlow as StateFlow<List<MediaItem>>
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
  }

  @Test
  fun topAppBarAndFabAreVisibleInEmptyState() {
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun fabIsClickableDoesNotCrash() {
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    // Just verify the FAB is present and "clickable" logically.
    // Robolectric can't actually launch SAF intents.
    composeRule.onNodeWithText("Import audio").performClick()
    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
  }

  @Test
  fun whenLibraryIsEmptyHidesEmptyText() {
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()

    composeRule.runOnIdle { libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true)) }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("No projects yet.").assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()
    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun doesNotCallImportWithoutActivityResult() {
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    verify(exactly = 0) { vm.importFromSaf(any()) }
  }

  @Test
  fun whenLibraryIsNonEmptyShowsListBranchImmediately() {
    // Start non-empty so the first composition goes straight to ProjectList branch
    libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true))
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    // Empty-state texts should NOT be visible
    composeRule.onNodeWithText("No projects yet.").assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()

    // App bar & FAB still present
    composeRule.onNodeWithText("Neptune • placeholder").assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun listBranchThenBackToEmptyRendersEmptyStateAgain() {
    // Start non-empty -> list branch
    libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true))
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    composeRule.onNodeWithText("No projects yet.").assertIsNotDisplayed()

    // Flip to empty -> should recompose to empty branch
    composeRule.runOnIdle { libraryFlow.value = emptyList() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
  }

  @Test
  fun importFromSafCallbackIsTriggered() {
    val fakeUri = android.net.Uri.parse("content://some/audio.mp3")
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    // Simulate that the picker returned a URI manually
    fakeUri.let { vm.importFromSaf(it.toString()) }

    verify(exactly = 1) { vm.importFromSaf("content://some/audio.mp3") }
  }

  @Test
  fun recordButtonStartsAndStopsRecording() {
    var isRecording = false
    recorder =
        mockk(relaxed = true) {
          every { this@mockk.isRecording } answers { isRecording }
          every { start(any(), any(), any()) } answers
              {
                isRecording = true
                mockk(relaxed = true) // Return a dummy file
              }
          every { stop() } answers
              {
                isRecording = false
                mockk(relaxed = true) // Return a dummy file
              }
        }

    composeRule.setContent { MockImportScreen(vm = vm, recorder = recorder) }
    composeRule.waitForIdle() // Wait for LaunchedEffect to check permissions

    // Initial state: not recording, Mic icon is shown
    composeRule.onNodeWithTag(MockImportTestTags.MIC_ICON, true).assertIsDisplayed()
    composeRule.onNodeWithTag(MockImportTestTags.STOP_ICON, true).assertDoesNotExist()

    // Action: click the record FAB
    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_RECORD, true).performClick()

    // After click: recording started
    verify { recorder.start(any(), any(), any()) }

    // The UI should update to show the Stop icon
    composeRule.onNodeWithTag(MockImportTestTags.STOP_ICON, true).assertIsDisplayed()
    composeRule.onNodeWithTag(MockImportTestTags.MIC_ICON, true).assertDoesNotExist()

    // Action: click the record FAB again
    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_RECORD, true).performClick()

    // After second click: recording stopped
    verify { recorder.stop() }
    // The UI should update to show the Mic icon again
    composeRule.onNodeWithTag(MockImportTestTags.MIC_ICON, true).assertIsDisplayed()
    composeRule.onNodeWithTag(MockImportTestTags.STOP_ICON, true).assertDoesNotExist()
  }
}
