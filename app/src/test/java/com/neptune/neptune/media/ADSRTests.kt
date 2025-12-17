package com.neptune.neptune.media

import android.media.MediaPlayer
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.neptune.neptune.ui.sampler.SamplerViewModel
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SamplerViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: TestSamplerViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    viewModel = TestSamplerViewModel()

    val dataSource =
        DataSource.toDataSource(viewModel.context, Uri.parse("file:///cache/fake_preview.wav"))

    ShadowMediaPlayer.addMediaInfo(
        dataSource,
        ShadowMediaPlayer.MediaInfo(/* durationMs = */ 1000, /* preparationTimeMs = */ 0))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---------- startADSRSampleWithPitch ----------

  @Test
  fun startADSRSampleWithPitchDoesNothingWhenOriginalAudioUriIsNull() = runTest {
    viewModel.startADSRSampleWithPitch(0)
    advanceUntilIdle()

    assertFalse(viewModel.adsrPlaying)
    assertFalse(viewModel.uiState.value.previewPlaying)
  }

  @Test
  fun startADSRSampleWithPitchDoesNotRestartIfAlreadyPlaying() = runTest {
    viewModel.setOriginalUri(fakeUri())
    viewModel.adsrPlaying = true

    viewModel.startADSRSampleWithPitch(3)
    advanceUntilIdle()

    assertFalse(viewModel.processAudioCalled)
  }

  @Test
  fun startADSRSampleWithPitchStartsPreviewAndUpdatesState() = runTest {
    viewModel.setOriginalUri(fakeUri())

    viewModel.startADSRSampleWithPitch(2)
    advanceUntilIdle()

    assertTrue(viewModel.processAudioCalled)
    assertTrue(viewModel.adsrPlaying)
    assertTrue(viewModel.uiState.value.previewPlaying)
    assertNotNull(viewModel.lastPreviewUri)
  }

  // ---------- stopADSRSample ----------

  @Test
  fun stopADSRSampleResetsStateEvenWithFade() = runTest {
    viewModel.setOriginalUri(fakeUri())
    viewModel.startADSRSampleWithPitch(0)
    advanceUntilIdle()

    viewModel.stopADSRSample()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.previewPlaying)
    assertFalse(viewModel.adsrPlaying)
  }

  // ---------- helpers ----------

  private fun fakeUri(): Uri = Uri.parse("file:///android_asset/fake.wav")
}

class TestSamplerViewModel : SamplerViewModel() {

  var processAudioCalled = false
  var lastPreviewUri: Uri? = null

  init {
    dispatcherProvider =
        object : DispatcherProvider {
          override val default = Dispatchers.Main
          override val io = Dispatchers.Main
          override val main = Dispatchers.Main
        }
  }

  fun setOriginalUri(uri: Uri) {
    _uiState.update { it.copy(originalAudioUri = uri) }
  }

  override fun processAudio(
      currentAudioUri: Uri?,
      eqBands: List<Float>,
      reverbWet: Float,
      reverbSize: Float,
      reverbWidth: Float,
      reverbDepth: Float,
      reverbPredelay: Float,
      audioProcessor: AudioProcessor,
      semitones: Int,
      tempoRatio: Double,
      attack: Float,
      decay: Float,
      sustain: Float,
      release: Float,
      mode: AudioProcessMode,
      outputNameSuffix: String
  ): Uri? {
    processAudioCalled = true
    lastPreviewUri = Uri.parse("file:///cache/fake_preview.wav")
    return lastPreviewUri
  }
}

@RunWith(RobolectricTestRunner::class)
class AudioFadeUtilsTest {

  @Test
  fun fadeOutAndReleaseImmediatelyReleasesWhenReleaseMillisIsZero() {
    val player = mock(MediaPlayer::class.java)

    fadeOutAndRelease(player, releaseMillis = 0L)

    verify(player).release()
  }
}
