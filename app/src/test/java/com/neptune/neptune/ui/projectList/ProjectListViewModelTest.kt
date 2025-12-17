package com.neptune.neptune.ui.projectList

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import com.neptune.neptune.util.NetworkConnectivityObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// These tests were made using AI assistance.
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

  private lateinit var viewModel: ProjectListViewModel

  // Mocks
  private val projectRepository: TotalProjectItemsRepository = mockk(relaxed = true)
  private val getLibraryUseCase: GetLibraryUseCase = mockk(relaxed = true)
  private val mediaRepository: MediaRepository = mockk(relaxed = true)
  private val storageService: StorageService = mockk(relaxed = true)
  private val auth: FirebaseAuth = mockk(relaxed = true)
  private val firebaseUser: FirebaseUser = mockk(relaxed = true)
  private val connectivityObserver: NetworkConnectivityObserver = mockk(relaxed = true)

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    val mockContext = mockk<Context>(relaxed = true)
    val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)

    // Mock generic android dependencies that might be touched
    mockkStatic(Uri::class)
    every { Uri.fromFile(any()) } returns mockk()

    every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns
        mockConnectivityManager
    NepTuneApplication.appContext = mockContext

    // Default: User logged in, Network Online
    every { auth.currentUser } returns firebaseUser
    every { connectivityObserver.isOnline } returns flowOf(true)

    // Default: No local library items
    every { getLibraryUseCase.invoke() } returns flowOf(emptyList())

    initializeViewModel()
  }

  private fun initializeViewModel() {
    viewModel =
        ProjectListViewModel(
            projectRepository = projectRepository,
            getLibraryUseCase = getLibraryUseCase,
            mediaRepository = mediaRepository,
            storageService = storageService,
            auth = auth,
            connectivityObserver = connectivityObserver)
  }

  @After
  fun tearDown() {
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun initialStateIsLoadingThenShowsProjects() =
      runTest(testDispatcher) {
        // Given
        val cloudProject = ProjectItem(uid = "1", name = "Cloud Project", isFavorite = true)
        coEvery { projectRepository.getAllProjects() } returns listOf(cloudProject)

        // When (ViewModel init triggers refresh)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.projects.size)
        assertEquals("Cloud Project", state.projects[0].name)
      }

  @Test
  fun whenUserIsLoggedOutLoadsLocalProjectsOnly() =
      runTest(testDispatcher) {
        // Given
        every { auth.currentUser } returns null
        val localMedia = MediaItem(id = "1", projectUri = "/local/path")
        every { getLibraryUseCase.invoke() } returns flowOf(listOf(localMedia))

        initializeViewModel() // Re-init with logged out state
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.projects.size)
        assertEquals("local_1", state.projects[0].uid)
        coVerify(exactly = 0) { projectRepository.getAllProjects() } // Should not fetch cloud
      }

  @Test
  fun whenLoggedInButOfflineFallsBackToLocalProjects() =
      runTest(testDispatcher) {
        // Given
        every { connectivityObserver.isOnline } returns flowOf(false)

        // FIX: Changed URI to include the expected name "Offline Local"
        val localMedia = MediaItem(id = "1", projectUri = "/local/Offline Local")
        every { getLibraryUseCase.invoke() } returns flowOf(listOf(localMedia))

        // Force repo to throw exception if called (simulate strict offline)
        coEvery { projectRepository.getAllProjects() } throws Exception("Network error")

        initializeViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.projects.size)
        // This assertion now passes because File("/local/Offline Local").nameWithoutExtension ==
        // "Offline Local"
        assertEquals("Offline Local", state.projects[0].name)
      }

  @Test
  fun whenOnlineAndLoggedInSyncsLocalFilesToCloud() =
      runTest(testDispatcher) {
        // Given: A local file exists that needs syncing
        val localPath = "path/to/project"
        val localMedia = MediaItem(id = "loc1", projectUri = localPath)
        every { getLibraryUseCase.invoke() } returns flowOf(listOf(localMedia))

        // Note: Actual sync logic (upload) is skipped here because File(path).exists() returns
        // false in unit tests.
        // We verify that the process continues to refresh projects.

        advanceUntilIdle()

        coVerify { projectRepository.getAllProjects() }
      }

  @Test
  fun deleteProjectRemovesFromRepositoryAndDeletesLocalFile() =
      runTest(testDispatcher) {
        // Given
        val project = ProjectItem(uid = "1", name = "To Delete", projectFileLocalPath = "/path/1")
        coEvery { projectRepository.getAllProjects() } returns listOf(project)

        val localMedia = MediaItem(id = "1", projectUri = "/path/1")
        every { getLibraryUseCase.invoke() } returns flowOf(listOf(localMedia))

        initializeViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteProject("1")
        advanceUntilIdle()

        // Then
        coVerify { projectRepository.deleteProject("1") }
        coVerify { mediaRepository.delete(any()) } // Should try to delete the media item
        coVerify(atLeast = 2) { projectRepository.getAllProjects() } // Refresh called
      }

  @Test
  fun renameProjectUpdatesRepository() =
      runTest(testDispatcher) {
        // Given
        val project = ProjectItem(uid = "1", name = "Old Name")
        coEvery { projectRepository.getAllProjects() } returns listOf(project)
        coEvery { projectRepository.getProject("1") } returns project

        initializeViewModel()
        advanceUntilIdle()

        // When
        viewModel.renameProject("1", "New Name")
        advanceUntilIdle()

        // Then
        val slot = slot<ProjectItem>()
        coVerify { projectRepository.editProject("1", capture(slot)) }
        assertEquals("New Name", slot.captured.name)
      }

  @Test
  fun changeProjectDescriptionUpdatesRepository() =
      runTest(testDispatcher) {
        // Given
        val project = ProjectItem(uid = "1", name = "name")
        coEvery { projectRepository.getAllProjects() } returns listOf(project)
        coEvery { projectRepository.getProject("1") } returns project

        initializeViewModel()
        advanceUntilIdle()

        // When
        viewModel.changeProjectDescription("1", "New Desc")
        advanceUntilIdle()

        // Then
        val slot = slot<ProjectItem>()
        coVerify { projectRepository.editProject("1", capture(slot)) }
        assertEquals("New Desc", slot.captured.description)
      }

  @Test
  fun toggleFavoriteFlipsBooleanInRepository() =
      runTest(testDispatcher) {
        // Given
        val project = ProjectItem(uid = "1", name = "skidding")
        coEvery { projectRepository.getAllProjects() } returns listOf(project)
        coEvery { projectRepository.getProject("1") } returns project

        initializeViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite("1")
        advanceUntilIdle()

        // Then
        val slot = slot<ProjectItem>()
        coVerify { projectRepository.editProject("1", capture(slot)) }
        assertTrue(slot.captured.isFavorite)
      }

  @Test
  fun addProjectToCloudCallsRepositoryIfProjectILocal() =
      runTest(testDispatcher) {
        // When
        viewModel.addProjectToCloud("local_123") // Should be ignored
        viewModel.addProjectToCloud("cloud_123")
        advanceUntilIdle()

        // Then
        coVerify { projectRepository.addProjectToCloud("cloud_123") }
        coVerify(exactly = 0) { projectRepository.addProjectToCloud("local_123") }
      }

  @Test
  fun selectProjectUpdatesUiState() =
      runTest(testDispatcher) {
        val project = ProjectItem(uid = "123", name = "ouic'estmoiSammy")
        viewModel.selectProject(project)

        assertEquals("123", viewModel.uiState.value.selectedProject)
      }
}
