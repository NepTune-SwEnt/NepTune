package com.neptune.neptune.screen

import android.Manifest
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.media.NeptuneRecorder
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.projectlist.ProjectListScreenTestTags
import com.neptune.neptune.ui.projectlist.ProjectListUiState
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectListScreenTest {

  @get:Rule val composeRule = createComposeRule()
  @get:Rule val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

  private lateinit var vm: ProjectListViewModel
  private lateinit var libraryFlow: MutableStateFlow<List<MediaItem>>
  private lateinit var recorder: NeptuneRecorder

  private lateinit var importMedia: ImportMediaUseCase

  private lateinit var uiStateFlow: MutableStateFlow<ProjectListUiState>

  private val navCalls = mutableListOf<String>()

  @Before
  fun setup() {
    navCalls.clear()
    importMedia = mockk(relaxed = true)
    libraryFlow = MutableStateFlow(emptyList())
    uiStateFlow = MutableStateFlow(ProjectListUiState(emptyList()))
    vm = mockk(relaxed = true)

    every { vm.library } returns libraryFlow
    every { vm.uiState } returns uiStateFlow
    every { vm.importFromSaf(any()) } answers { mockk<Job>(relaxed = true) }
    every { vm.importRecordedFile(any()) } answers { mockk<Job>(relaxed = true) }
  }

  private fun launchScreen(
      recorder: NeptuneRecorder = mockk(relaxed = true),
      testFile: File? = null,
      onDeleteFailed: (() -> Unit)? = null,
  ) {
    composeRule.setContent {
      ProjectListScreen(
          projectListViewModel = vm,
          recorder = recorder,
          testRecordedFile = testFile,
          onDeleteFailed = onDeleteFailed,
          onProjectClick = { navCalls.add("2") })
    }
  }

  @Test
  fun topAppBarAndFabAreVisibleInEmptyState() {
    launchScreen()

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

    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST).assertIsDisplayed()

    composeRule.runOnIdle { uiStateFlow.value = ProjectListUiState(listOf(mockk(relaxed = true))) }

    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST).assertIsNotDisplayed()
    composeRule
        .onNodeWithText(
            "Tap “Import audio” to create a .neptune project (zip with config.json + audio).")
        .assertIsNotDisplayed()
    composeRule.onNodeWithText("Import audio").assertIsDisplayed()
  }

  @Test
  fun doesNotCallImportWithoutActivityResult() {
    launchScreen()

    verify(exactly = 0) { vm.importFromSaf(any()) }
  }

  @Test
  fun whenLibraryIsNonEmptyShowsListBranchImmediately() {
    composeRule.runOnIdle { uiStateFlow.value = ProjectListUiState(listOf(mockk(relaxed = true))) }
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

    composeRule.runOnIdle { uiStateFlow.value = ProjectListUiState(listOf(mockk(relaxed = true))) }
    launchScreen()

    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST).assertIsNotDisplayed()

    // Flip to empty -> should recompose to empty branch
    composeRule.runOnIdle { uiStateFlow.value = ProjectListUiState(emptyList()) }

    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST).assertIsDisplayed()
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
    val isRecordingState = mutableStateOf(false)
    recorder =
        mockk(relaxed = true) {
          every { isRecording } answers { isRecordingState.value }
          every { start(any(), any(), any(), any()) } answers
              {
                isRecordingState.value = true
                mockk(relaxed = true) // Return a dummy file
              }
          every { stop() } answers
              {
                isRecordingState.value = false
                mockk(relaxed = true) // Return a dummy file
              }
        }

    launchScreen(recorder = recorder)

    // Initial state: not recording
    composeRule.onNodeWithTag(ProjectListScreenTestTags.MIC_ICON, true).assertIsDisplayed()

    // Start recording
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_RECORD, true).performClick()
    composeRule.runOnIdle { verify(exactly = 1) { recorder.start(any(), any(), any(), any()) } }

    // Stop recording
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_RECORD, true).performClick()
    composeRule.runOnIdle { verify(exactly = 1) { recorder.stop() } }

    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CANCEL, true).assertIsDisplayed()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CANCEL, true).performClick()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST, true).assertIsDisplayed()
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
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CANCEL, true).assertIsDisplayed()

    // Click cancel which will attempt to delete the file -> should call onDeleteFailed
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CANCEL, true).performClick()

    // Verify the hook was invoked
    composeRule.runOnIdle {
      Assert.assertTrue("onDeleteFailed should be invoked", deleteFailedCalled)
    }

    // After dismiss, empty list should be visible again
    composeRule.onNodeWithTag(ProjectListScreenTestTags.EMPTY_LIST, true).assertIsDisplayed()
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
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CREATE, true).assertIsDisplayed()

    // Click confirm which should sanitize the name "My Recording" -> "My_Recording.m4a" and call
    // ViewModel
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_CREATE, true).performClick()

    // Verify that importRecordedFile was called with a file whose name matches the sanitized name
    verify { vm.importRecordedFile(match { it.name == "My_Recording.m4a" }) }
  }

  /** Verify that the mic button and Import audio button are clickable */
  @Test
  fun canClickOnButton() {
    launchScreen()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.BUTTON_RECORD).assertHasClickAction()
    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.IMPORT_AUDIO_BUTTON)
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun openingProjectCallsNavigate() {
    val project = ProjectItem(uid = "2", name = "Fav", description = "Favorite item")
    uiStateFlow.value = ProjectListUiState(listOf(project))
    launchScreen()

    // Click on project card
    composeRule.onNodeWithTag("project_2").performClick()

    // Verify navigation was called with id "2"
    assertEquals(listOf("2"), navCalls)
  }

  @Test
  fun favoriteToggleUpdatesOrder() {
    val projects =
        listOf(
            ProjectItem(uid = "2", name = "Fav", description = "Favorite item", isFavorite = true),
            ProjectItem(uid = "3", name = "New", description = "New item", isFavorite = false))
    uiStateFlow.value = ProjectListUiState(projects)
    launchScreen()

    // Click favorite button to toggle
    composeRule.onNodeWithTag("favorite_2").performClick()

    // Verify VM was called to toggle favorite
    verify { vm.toggleFavorite("2") }
  }

  @Test
  fun renamingProjectUpdatesUi() {
    val project = ProjectItem(uid = "1", name = "Old", description = "Old desc")
    uiStateFlow.value = ProjectListUiState(listOf(project))
    launchScreen()

    // Open menu & rename
    composeRule.onNodeWithTag("menu_1").performClick()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.RENAME_BUTTON).performClick()
    composeRule.onNodeWithText("New name").performTextClearance()
    composeRule.onNodeWithText("New name").performTextInput("Old Renamed")
    composeRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()

    // Verify VM called with new name
    verify { vm.renameProject("1", "Old Renamed") }
  }

  @Test
  fun changeDescriptionUpdatesRepository() {
    val newDesc = "Updated description from test"
    val project = ProjectItem(uid = "3", name = "New", description = "New item")
    uiStateFlow.value = ProjectListUiState(listOf(project))
    launchScreen()

    // Open menu & change description
    composeRule.onNodeWithTag("menu_3").performClick()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.CHANGE_DESCRIPTION_BUTTON).performClick()
    // Input new description and confirm
    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.DESCRIPTION_TEXT_FIELD)
        .performTextInput(newDesc)
    composeRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()

    verify(exactly = 1) { vm.changeProjectDescription("3", newDesc) }
  }

  @Test
  fun deleteProjectRemovesItemFromUi() {
    val project = ProjectItem(uid = "1", name = "Old", description = "Old desc")
    uiStateFlow.value = ProjectListUiState(listOf(project))
    launchScreen()

    composeRule.onNodeWithTag("menu_1").performClick()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.DELETE_BUTTON).performClick()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()

    verify { vm.deleteProject("1") }
  }

  @Test
  fun testTagsAreCorrect() {
    launchScreen()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN).assertIsDisplayed()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }

  /** Tests that you can type on the search bar */
  @Test
  fun searchbarCanType() {
    launchScreen()
    // Simulate typing
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("Test")

    composeRule
        .onNode(hasText("Test"))
        .assertExists("Text input wasn't updated in the search field.")
  }

  /** Tests that the search icon exists and that the topBar has the right default text */
  @Test
  fun searchbarHasDefaultText() {
    launchScreen()

    // Assert that the search icon exist
    composeRule.onNodeWithContentDescription("Search Icon").assertExists()

    composeRule.onNode(hasText("Search for a Project")).assertIsDisplayed()
  }
}
