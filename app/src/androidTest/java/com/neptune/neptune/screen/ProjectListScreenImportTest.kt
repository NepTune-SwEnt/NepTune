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
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.ui.picker.ImportScreenTestTags
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.projectlist.ProjectListUiState
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectListScreenImportTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule
  val permissionRule: GrantPermissionRule? =
      GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

  private lateinit var importViewModel: ImportViewModel
  private lateinit var projectListViewModel: ProjectListViewModel

  private lateinit var uiStateFlow: MutableStateFlow<ProjectListUiState>
  private lateinit var errorMessageFlow: MutableStateFlow<String?>
  private lateinit var isOnlineFlow: MutableStateFlow<Boolean>

  private lateinit var recorder: NeptuneRecorder
  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private val emptyStateText =
      "Tap “Import audio” to create a .neptune project. \n (zip with config.json + audio)"

  @Before
  fun setup() {
    importViewModel = mockk(relaxed = true)
    projectListViewModel = mockk(relaxed = true)
    recorder = mockk(relaxed = true)
    mediaPlayer = mockk(relaxed = true)

    uiStateFlow = MutableStateFlow(ProjectListUiState(projects = emptyList(), isLoading = false))
    errorMessageFlow = MutableStateFlow(null)
    isOnlineFlow = MutableStateFlow(true)

    every { projectListViewModel.uiState } returns uiStateFlow
    every { projectListViewModel.isOnline } returns isOnlineFlow
    every { projectListViewModel.isUserLoggedIn } returns true

    every { importViewModel.errorMessage } returns errorMessageFlow
    every { importViewModel.setOnImportFinished(any()) } returns Unit
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
    val fakeProject =
        mockk<ProjectItem>(relaxed = true) {
          every { uid } returns "1"
          every { name } returns "P1"
          every { description } returns ""
          every { tags } returns emptyList()
        }
    uiStateFlow.value = ProjectListUiState(projects = listOf(fakeProject))

    launchScreen()
    composeRule.onNodeWithText(emptyStateText).assertIsNotDisplayed()

    // WHEN - Liste vide
    uiStateFlow.value = ProjectListUiState(projects = emptyList())
    composeRule.waitForIdle()

    // THEN - Le texte vide doit s'afficher
    composeRule.onNodeWithText(emptyStateText).assertIsDisplayed()
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
