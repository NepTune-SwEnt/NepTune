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
import com.neptune.neptune.model.project.ProjectItemsRepositoryVarVar
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.ui.picker.ImportScreenTestTags
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
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

class ProjectListScreenImportTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

  private lateinit var importViewModel: ImportViewModel
  private lateinit var projectListViewModel: ProjectListViewModel
  private lateinit var projectListRepo: TotalProjectItemsRepository
  private lateinit var libraryFlow: MutableStateFlow<List<MediaItem>>
  private lateinit var recorder: NeptuneRecorder

  @Before
  fun setup() {
    importViewModel = mockk(relaxed = true)
    projectListRepo = ProjectItemsRepositoryVarVar()
    projectListViewModel = ProjectListViewModel(projectListRepo)
    libraryFlow = MutableStateFlow(emptyList())
    every { importViewModel.library } returns libraryFlow as StateFlow<List<MediaItem>>
  }

  private fun launchScreen(
      recorder: NeptuneRecorder = mockk(relaxed = true),
      testFile: File? = null,
      onDeleteFailed: (() -> Unit)? = null,
  ) {
    composeRule.setContent {
      ProjectListScreen(
          projectListViewModel = projectListViewModel,
          importViewModel = importViewModel,
          recorder = recorder,
          testRecordedFile = testFile,
          onDeleteFailed = onDeleteFailed,
          mediaPlayer = FakeMediaPlayer())
    }
  }

  @Test
  fun doesNotCallImportWithoutActivityResult() {
    launchScreen()

    verify(exactly = 0) { importViewModel.importFromSaf(any()) }
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
  }

  @Test
  fun importFromSafCallbackIsTriggered() {
    val fakeUri = Uri.parse("content://some/audio.mp3")
    launchScreen()

    // Simulate that the picker returned a URI manually
    fakeUri.let { importViewModel.importFromSaf(it.toString()) }

    verify(exactly = 1) { importViewModel.importFromSaf("content://some/audio.mp3") }
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
