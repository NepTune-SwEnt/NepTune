package com.neptune.neptune.media

import android.net.Uri
import com.neptune.neptune.ui.sampler.SamplerViewModel
import java.lang.reflect.Field
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeNeptuneMediaPlayer : NeptuneMediaPlayer() {
  var playedUri: Uri? = null
  var stopped = false
  var released = false
  var isPlayingState = false

  override fun play(uri: Uri) {
    playedUri = uri
    isPlayingState = true
  }

  override fun isPlaying(): Boolean = isPlayingState

  override fun forceStopAndRelease() {
    stopped = true
    released = true
    isPlayingState = false
    playedUri = null
  }

  override fun stopWithFade(releaseMillis: Long) {
    stopped = true
    released = true
    isPlayingState = false
    playedUri = null
  }
}

class SamplerViewModelADSRSampleTest {

  private lateinit var viewModel: SamplerViewModel
  private lateinit var fakePlayer: FakeNeptuneMediaPlayer
  private val testUri = Uri.parse("test://audio")

  @Before
  fun setup() {
    viewModel = SamplerViewModel()
    fakePlayer = FakeNeptuneMediaPlayer()

    // Inject fake media player via reflection
    val field: Field = SamplerViewModel::class.java.getDeclaredField("mediaPlayer")
    field.isAccessible = true
    field.set(viewModel, fakePlayer)
  }

  @Test
  fun testStartADSRSamplePlaysAudioAndUpdatesUiState() = runBlocking {
    // Set the original audio URI
    viewModel._uiState.value = viewModel._uiState.value.copy(originalAudioUri = testUri)

    // Call start
    viewModel.startADSRSample()

    // Verify mediaPlayer.play was called
    assertEquals(testUri, fakePlayer.playedUri)
    assertTrue(fakePlayer.isPlaying())

    // Verify adsrPlaying and previewPlaying
    assertTrue(viewModel.adsrPlaying)
    assertTrue(viewModel.uiState.first().previewPlaying)
  }

  @Test
  fun testStopADSRSampleStopsAudioAndUpdatesUiState() = runBlocking {
    // Prepare viewModel state
    viewModel._uiState.value =
        viewModel._uiState.value.copy(originalAudioUri = testUri, release = 0f)
    viewModel.adsrPlaying = true
    fakePlayer.isPlayingState = true
    fakePlayer.playedUri = testUri

    // Call stop
    viewModel.stopADSRSample()

    // Verify mediaPlayer.forceStopAndRelease called
    assertTrue(fakePlayer.stopped)
    assertTrue(fakePlayer.released)
    assertEquals(false, viewModel.adsrPlaying)
    assertEquals(false, viewModel.uiState.first().previewPlaying)
  }
}
