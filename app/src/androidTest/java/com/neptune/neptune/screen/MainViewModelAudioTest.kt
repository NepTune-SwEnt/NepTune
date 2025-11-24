package com.neptune.neptune.screen

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.util.AudioWaveformExtractor
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

// These tests were made using AI assistance.
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MainViewModelAudioTest {

  @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Mock private lateinit var mockStorageService: StorageService
  @Mock private lateinit var mockSampleRepo: SampleRepository
  @Mock private lateinit var mockProfileRepo: ProfileRepository
  @Mock private lateinit var mockAuth: FirebaseAuth
  @Mock private lateinit var mockUser: FirebaseUser

  @Mock private lateinit var mockWaveformExtractor: AudioWaveformExtractor

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    whenever(mockProfileRepo.observeProfile()).thenReturn(flowOf(null))

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("test_uid")

    viewModel =
        MainViewModel(
            repo = mockSampleRepo,
            context = appContext,
            profileRepo = mockProfileRepo,
            storageService = mockStorageService,
            auth = mockAuth,
            waveformExtractor = mockWaveformExtractor,
            useMockData = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ----------------------------------------------------------------
  // Tests Audio URL
  // ----------------------------------------------------------------

  @Test
  fun getSampleAudioUrlReturnsNullWhenStoragePathIsEmpty() =
      runTest(testDispatcher) {
        val sample = createSample(storagePath = "")
        viewModel.loadSampleResources(sample)
        advanceUntilIdle()
        val resources = viewModel.sampleResources.value[sample.id]
        assertEquals(null, resources?.audioUrl)
      }

  @Test
  fun getSampleAudioUrlCallsStorageServiceWhenCacheIsEmpty() =
      runTest(testDispatcher) {
        val path = "samples/audio.mp3"
        val expectedUrl = "https://fake.url/audio.mp3"
        val sample = createSample(storagePath = path)

        whenever(mockStorageService.getDownloadUrl(path)).thenReturn(expectedUrl)

        viewModel.loadSampleResources(sample)
        advanceUntilIdle()
        val resources = viewModel.sampleResources.value[sample.id]

        assertEquals(expectedUrl, resources?.audioUrl)
        verify(mockStorageService, times(1)).getDownloadUrl(path)
      }

  @Test
  fun getSampleAudioUrlReturnsCachedValueWithoutCallingStorageService() =
      runTest(testDispatcher) {
        val path = "samples/cached_audio.mp3"
        val expectedUrl = "https://fake.url/cached.mp3"
        val sample = createSample(storagePath = path)

        whenever(mockStorageService.getDownloadUrl(path)).thenReturn(expectedUrl)
        viewModel.loadSampleResources(sample)
        advanceUntilIdle()
        viewModel.loadSampleResources(sample)
        advanceUntilIdle()
        val resources = viewModel.sampleResources.value[sample.id]
        assertEquals(expectedUrl, resources?.audioUrl)

        verify(mockStorageService, times(1)).getDownloadUrl(path)
      }

  // ----------------------------------------------------------------
  // Tests Waveform
  // ----------------------------------------------------------------

  @Test
  fun getSampleWaveformReturnsEmptyListWhenAudioUrlIsNull() =
      runTest(testDispatcher) {
        val sample = createSample(storagePath = "")
        viewModel.loadSampleResources(sample)
        advanceUntilIdle()

        val resources = viewModel.sampleResources.value[sample.id]

        assertTrue(resources?.waveform?.isEmpty() == true)
      }

  @Test
  fun getSampleWaveformExtractsWaveformAndCachesIt() =
      runTest(testDispatcher) {
        val path = "samples/wave.mp3"
        val audioUrl = "https://fake.url/wave.mp3"
        val sample = createSample(storagePath = path)
        val expectedWaveform = listOf(0.1f, 0.5f, 0.9f)

        val expectedUri = Uri.parse(audioUrl)

        whenever(mockStorageService.getDownloadUrl(path)).thenReturn(audioUrl)

        whenever(mockWaveformExtractor.extractWaveform(any(), eq(expectedUri), any()))
            .thenReturn(expectedWaveform)

        viewModel.loadSampleResources(sample)
        advanceUntilIdle()

        val resources = viewModel.sampleResources.value[sample.id]
        assertEquals(expectedWaveform, resources?.waveform)

        viewModel.loadSampleResources(sample)
        advanceUntilIdle()

        verify(mockWaveformExtractor, times(1)).extractWaveform(any(), eq(expectedUri), any())
      }

  @Test
  fun getSampleWaveformReturnsEmptyListOnException() =
      runTest(testDispatcher) {
        val path = "samples/error.mp3"
        val audioUrl = "http://url"
        val sample = createSample(storagePath = path)
        val expectedUri = Uri.parse(audioUrl)

        whenever(mockStorageService.getDownloadUrl(path)).thenReturn(audioUrl)

        whenever(mockWaveformExtractor.extractWaveform(any(), eq(expectedUri), any()))
            .thenThrow(RuntimeException("Extraction failed"))

        viewModel.loadSampleResources(sample)
        advanceUntilIdle()

        val resources = viewModel.sampleResources.value[sample.id]

        assertTrue(resources?.waveform?.isEmpty() == true)
      }

  private fun createSample(storagePath: String): Sample {
    return Sample(
        id = "id_${storagePath.hashCode()}",
        name = "Test Sample",
        description = "Desc",
        durationSeconds = 10,
        tags = emptyList(),
        likes = 0,
        usersLike = emptyList(),
        comments = 0,
        downloads = 0,
        ownerId = "owner",
        storagePreviewSamplePath = storagePath)
  }
}
