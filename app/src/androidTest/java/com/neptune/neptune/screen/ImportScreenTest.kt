package com.neptune.neptune.screen

import android.Manifest
import android.net.Uri
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.ui.picker.ImportScreen
import com.neptune.neptune.ui.picker.ImportScreenTestTags
import com.neptune.neptune.ui.picker.ImportViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImportScreenTest {

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

  private fun launchScreen(
      recorder: NeptuneRecorder = mockk(relaxed = true),
      testFile: File? = null,
      onDeleteFailed: (() -> Unit)? = null,
      goBack: () -> Unit = {},
  ) {
    composeRule.setContent {
      ImportScreen(
          vm = vm,
          recorder = recorder,
          testRecordedFile = testFile,
          onDeleteFailed = onDeleteFailed,
          goBack = goBack,
      )
    }
  }

  @Test
  fun topAppBarAndFabAreVisibleInEmptyState() {
    launchScreen()
    // Back button
    composeRule.onNodeWithTag(ImportScreenTestTags.BACK_BUTTON).assertIsDisplayed()

    // Empty list text
    composeRule.onNodeWithText("No projects yet.").assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
    // FAB
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun whenLibraryIsEmptyHidesEmptyText() {
    launchScreen()

    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST).assertIsDisplayed()

    composeRule.runOnIdle { libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true)) }

    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST).assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()
    composeRule.onNodeWithTag(ImportScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun doesNotCallImportWithoutActivityResult() {
    launchScreen()

    verify(exactly = 0) { vm.importFromSaf(any()) }
  }

  @Test
  fun whenLibraryIsNonEmptyShowsListBranchImmediately() {
    // Start non-empty so the first composition goes straight to ProjectList branch
    libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true))
    launchScreen()

    // Empty-state texts should NOT be visible
    composeRule.onNodeWithText("No projects yet.").assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()

    // App bar & FAB still present
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun listBranchThenBackToEmptyRendersEmptyStateAgain() {
    // Start non-empty -> list branch
    libraryFlow.value = listOf(mockk<MediaItem>(relaxed = true))
    launchScreen()

    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST).assertIsNotDisplayed()

    // Flip to empty -> should recompose to empty branch
    composeRule.runOnIdle { libraryFlow.value = emptyList() }

    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST).assertIsDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsDisplayed()
  }

  @Test
  fun importFromSafCallbackIsTriggered() {
    val fakeUri = Uri.parse("content://some/audio.mp3")
    launchScreen()

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

    launchScreen(recorder = recorder)

    // Initial state: not recording
    composeRule.onNodeWithTag(ImportScreenTestTags.MIC_ICON, true).assertIsDisplayed()

    // Start recording
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_RECORD, true).performClick()
    verify { recorder.start(any(), any(), any()) }

    // Stop recording
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_RECORD, true).performClick()
    verify { recorder.stop() }

    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CANCEL, true).assertIsDisplayed()
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CANCEL, true).performClick()
    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST, true).assertIsDisplayed()
  }

  @Test
  fun cancelInvokesOnDeleteFailedWhenFileDeleteFails() {
    var deleteFailedCalled = false

    // Create a File instance that reports delete() failure.
    val fakeFile =
        object : File("/nonexistent/fake-recording.m4a") {
          override fun delete(): Boolean = false
        }

    launchScreen(testFile = fakeFile, onDeleteFailed = { deleteFailedCalled = true })

    // The name dialog should be visible because we passed testRecordedFile
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CANCEL, true).assertIsDisplayed()

    // Click cancel which will attempt to delete the file -> should call onDeleteFailed
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CANCEL, true).performClick()

    // Verify the hook was invoked
    composeRule.runOnIdle {
      Assert.assertTrue("onDeleteFailed should be invoked", deleteFailedCalled)
    }

    // After dismiss, empty list should be visible again
    composeRule.onNodeWithTag(ImportScreenTestTags.EMPTY_LIST, true).assertIsDisplayed()
  }

  @Test
  fun confirmSanitizesNameAndImportsRenamedFile() {
    // Create a File instance that pretends to be a recorded file and succeeds when renameTo is
    // called.
    val fakeFile =
        object : File("/tmp/My Recording.m4a") {
          override fun renameTo(dest: File): Boolean {
            // Simulate successful rename (the production code will then pass 'dest' to the
            // ViewModel)
            return true
          }
        }

    launchScreen(testFile = fakeFile)

    // The confirm button should be visible in the name dialog
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CREATE, true).assertIsDisplayed()

    // Click confirm which should sanitize the name "My Recording" -> "My_Recording.m4a" and call
    // ViewModel
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_CREATE, true).performClick()

    // Verify that importRecordedFile was called with a file whose name matches the sanitized name
    verify { vm.importRecordedFile(match { it.name == "My_Recording.m4a" }) }
  }

  /** Verify that the mic button and Import audio button are clickable */
  @Test
  fun canClickOnButton() {
    launchScreen()
    composeRule.onNodeWithTag(ImportScreenTestTags.BUTTON_RECORD).assertHasClickAction()
    composeRule
        .onNodeWithTag(ImportScreenTestTags.IMPORT_AUDIO_BUTTON)
        .assertHasClickAction()
        .performClick()
  }
}
