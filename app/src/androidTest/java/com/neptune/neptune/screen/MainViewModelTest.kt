package com.neptune.neptune.screen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.main.SampleUiActions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

  private class RecordingActions :
      SampleUiActions(
          repo = mock(),
          storageService = mock(),
          downloadsFolder = File("/tmp"),
          context = mock(),
          downloadProgress = MutableStateFlow(null),
          profileRepo = mock()) {
    var zippedCalls = 0
    var processedCalls = 0
    var lastZippedSample: Sample? = null
    var lastProcessedSample: Sample? = null

    override suspend fun onDownloadZippedClicked(sample: Sample) {
      zippedCalls++
      lastZippedSample = sample
    }

    override suspend fun onDownloadProcessedClicked(sample: Sample) {
      processedCalls++
      lastProcessedSample = sample
    }
  }

  private class TestMainViewModel(
      sampleRepo: FakeSampleRepository,
      profileRepo: FakeProfileRepository,
      storageService: StorageService,
      auth: FirebaseAuth,
      private val injectedActions: SampleUiActions
  ) :
      MainViewModel(
          sampleRepo = sampleRepo,
          profileRepo = profileRepo,
          storageService = storageService,
          useMockData = false,
          auth = auth) {
    override val actions: SampleUiActions = injectedActions
  }

  @Test
  fun onDownloadZippedSampleDelegatesToActions() = runTest {
    val actions = RecordingActions()

    val vm =
        TestMainViewModel(
            sampleRepo = fakeRepository,
            profileRepo = fakeProfileRepository,
            storageService = mockStorageService,
            auth = mockAuth,
            injectedActions = actions)

    val sample =
        Sample(
            id = "s1",
            name = "Zip",
            description = "",
            durationSeconds = 0,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path")

    vm.onDownloadZippedSample(sample)

    Assert.assertEquals(1, actions.zippedCalls)
    Assert.assertTrue(actions.lastZippedSample == sample)
  }

  @Test
  fun onDownloadProcessedSampleDelegatesToActions() = runTest {
    val actions = RecordingActions()

    val vm =
        TestMainViewModel(
            sampleRepo = fakeRepository,
            profileRepo = fakeProfileRepository,
            storageService = mockStorageService,
            auth = mockAuth,
            injectedActions = actions)

    val sample =
        Sample(
            id = "s2",
            name = "Processed",
            description = "",
            durationSeconds = 0,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path",
            storageProcessedSamplePath = "processed/path.wav")

    vm.onDownloadProcessedSample(sample)

    Assert.assertEquals(1, actions.processedCalls)
    Assert.assertTrue(actions.lastProcessedSample == sample)
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

  // -------------------------
  // Helpers for NEW tests only
  // -------------------------

  private fun makeSample(id: String, likes: Int = 0): Sample =
      Sample(
          id = id,
          name = "n$id",
          description = "d$id",
          durationSeconds = 1,
          tags = emptyList(),
          likes = likes,
          usersLike = emptyList(),
          comments = 0,
          downloads = 0,
          ownerId = "u$id",
          isPublic = true,
          storagePreviewSamplePath = "not_blank")

  private fun emitSamples(fakeRepo: FakeSampleRepository, samples: List<Sample>) {
    // Update FakeSampleRepository internal list + flow (test-only reflection).
    val listField = FakeSampleRepository::class.java.getDeclaredField("samples")
    listField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val backingList = listField.get(fakeRepo) as MutableList<Sample>
    backingList.clear()
    backingList.addAll(samples)

    val flowField = FakeSampleRepository::class.java.getDeclaredField("_samples")
    flowField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val flow = flowField.get(fakeRepo) as MutableStateFlow<List<Sample>>
    flow.value = backingList.toList()
  }

  // ------------------------------------
  // NEW TEST: recommended objects refresh
  // ------------------------------------
  @Test
  fun observeSamplesRefreshesRecommendedObjectsWithoutReordering() = runTest {
    // IMPORTANT: create a VM that actually observes sampleRepo.observeSamples()
    val observingVm =
        MainViewModel(
            sampleRepo = fakeRepository,
            profileRepo = fakeProfileRepository,
            useMockData = false, // <-- must be false so loadSamplesFromFirebase() runs
            auth = mockAuth,
            storageService = mockStorageService)

    val a1 = makeSample("A", likes = 1)
    val b1 = makeSample("B", likes = 10)

    // recommended is B then A
    observingVm.setRecommendedSamplesForTest(listOf(b1, a1))

    // initial snapshot
    emitSamples(fakeRepository, listOf(a1, b1))
    advanceUntilIdle()

    // snapshot updates likes of B
    val a2 = a1.copy(likes = 1)
    val b2 = b1.copy(likes = 999)
    emitSamples(fakeRepository, listOf(a2, b2))
    advanceUntilIdle()

    val rec = observingVm.recommendedSamples.value
    Assert.assertEquals(listOf("B", "A"), rec.map { it.id }) // order stable
    Assert.assertEquals(999, rec.first { it.id == "B" }.likes) // data refreshed
  }
}
