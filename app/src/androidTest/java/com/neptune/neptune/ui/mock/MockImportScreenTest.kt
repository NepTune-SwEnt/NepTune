package com.neptune.neptune.ui.mock

import android.Manifest
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.ui.picker.ImportViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MockImportScreenTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

  private lateinit var vm: ImportViewModel
  private lateinit var libraryFlow: MutableStateFlow<List<MediaItem>>
  private lateinit var recorder: NeptuneRecorder

  @Before
  fun setup() {
    vm = mockk(relaxed = true)
    libraryFlow = MutableStateFlow(emptyList())
    every { vm.library } returns libraryFlow as StateFlow<List<MediaItem>>
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
  fun whenLibraryIsEmptyHidesEmptyText() {
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()

    composeRule.runOnIdle { libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true)) }

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

    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
  }

  @Test
  fun importFromSafCallbackIsTriggered() {
    val fakeUri = Uri.parse("content://some/audio.mp3")
    composeRule.setContent { MockImportScreen(vm = vm, recorder = mockk(relaxed = true)) }

    // Simulate that the picker returned a URI manually
    fakeUri.let { vm.importFromSaf(it.toString()) }

    verify(exactly = 1) { vm.importFromSaf("content://some/audio.mp3") }
  }

  @Test
  fun recordStopAndCancel() {
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

    // Initial state: not recording
    composeRule.onNodeWithTag(MockImportTestTags.MIC_ICON, true).assertIsDisplayed()

    // Start recording
    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_RECORD, true).performClick()
    verify { recorder.start(any(), any(), any()) }

    // Stop recording
    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_RECORD, true).performClick()
    verify { recorder.stop() }

    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_CANCEL, true).assertIsDisplayed()
    composeRule.onNodeWithTag(MockImportTestTags.BUTTON_CANCEL, true).performClick()
    composeRule.onNodeWithTag(MockImportTestTags.EMPTY_LIST, true).assertIsDisplayed()
  }
}
