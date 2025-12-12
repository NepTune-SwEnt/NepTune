package com.neptune.neptune.screen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// These tests were maid using AI assistance
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}

@ExperimentalCoroutinesApi
class MainViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule()
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser

  private lateinit var fakeRepository: FakeSampleRepository
  private lateinit var fakeProfileRepository: FakeProfileRepository
  private lateinit var viewModel: MainViewModel
  private lateinit var mockStorageService: StorageService

  @Before
  // This function was made using AI assistance
  fun setup() {
    mockAuth = mock()
    mockFirebaseUser = mock()
    mockStorageService = mock()

    fakeRepository = FakeSampleRepository()
    fakeProfileRepository = FakeProfileRepository()

    whenever(mockAuth.currentUser).thenReturn(mockFirebaseUser)
    whenever(mockFirebaseUser.uid).thenReturn("fake_user_id_for_test")

    viewModel =
        MainViewModel(
            sampleRepo = fakeRepository,
            profileRepo = fakeProfileRepository,
            useMockData = true,
            auth = mockAuth,
            storageService = mockStorageService)
  }

  @Test
  fun discoverSamplesLoadsCorrectly() {
    val discover = viewModel.discoverSamples.value
    Assert.assertEquals(4, discover.size)
    Assert.assertEquals("Sample 1", discover[0].name)
  }

  @Test
  fun followedSamplesLoadsCorrectly() {
    val followed = viewModel.followedSamples.value
    Assert.assertEquals(2, followed.size)
    Assert.assertEquals("Sample 5", followed[0].name)
  }

  @Test
  fun isCurrentUserMatchesCurrentFirebaseUserId() {
    Assert.assertTrue(viewModel.isCurrentUser("fake_user_id_for_test"))
    Assert.assertFalse(viewModel.isCurrentUser("someone_else"))
    Assert.assertFalse(viewModel.isCurrentUser(""))
  }

  @Test
  fun getSampleCoverUrlFetchesAndCachesUrl() {
    runBlocking {
      val imagePath = "images/cover.jpg"
      val expectedUrl = "https://fake.url/cover.jpg"

      whenever(mockStorageService.getDownloadUrl(imagePath)).thenReturn(expectedUrl)

      val sample =
          Sample(
              id = "sample_cover_test",
              name = "Cover Test",
              description = "Desc",
              durationSeconds = 10,
              ownerId = "owner",
              storagePreviewSamplePath = "audio.mp3",
              storageImagePath = imagePath,
              likes = 0,
              downloads = 0,
              comments = 0,
              usersLike = emptyList(),
              tags = emptyList())

      viewModel.loadSampleResources(sample)

      val resources = viewModel.sampleResources.value[sample.id]
      Assert.assertEquals(expectedUrl, resources?.coverImageUrl)
      verify(mockStorageService, times(1)).getDownloadUrl(imagePath)

      viewModel.loadSampleResources(sample)

      verify(mockStorageService, times(1)).getDownloadUrl(imagePath)
    }
  }

  @Test
  fun loadSampleResourcesResetsLoadingStateOnError() = runBlocking {
    val audioPath = "audio/error.mp3"

    whenever(mockStorageService.getDownloadUrl(audioPath))
        .thenThrow(RuntimeException("Network Error"))

    val sample =
        Sample(
            id = "sample_error_test",
            name = "Error Test",
            description = "Desc",
            durationSeconds = 10,
            ownerId = "owner",
            storagePreviewSamplePath = audioPath,
            likes = 0,
            downloads = 0,
            comments = 0,
            usersLike = emptyList(),
            tags = emptyList())

    viewModel.loadSampleResources(sample)

    val resources = viewModel.sampleResources.value[sample.id]
    Assert.assertNotNull(resources)
    Assert.assertFalse("isLoading should be false after an error", resources!!.isLoading)
    Assert.assertNull("The audio URL should not be defined", resources.audioUrl)
  }
}
