package com.neptune.neptune.screen

import android.net.Uri
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.sampler.SamplerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeMediaPlayer : NeptuneMediaPlayer() {
  var fakeUri: Uri? = null
  var isPlayingState = false
  var preparedListener: (() -> Unit)? = null
  var currentPositionMillis = 0
  var durationMillis = 4000

  override fun isPlaying(): Boolean = isPlayingState

  override fun getCurrentPosition(): Int = currentPositionMillis

  override fun getDuration(): Int = durationMillis

  override fun play(uri: Uri) {
    fakeUri = uri
    isPlayingState = true
    preparedListener?.invoke()
  }

  override fun pause() {
    isPlayingState = false
  }

  override fun resume() {
    isPlayingState = true
  }

  override fun goTo(positionMillis: Int) {
    currentPositionMillis = positionMillis
  }

  override fun togglePlay(uri: Uri) {
    isPlayingState = !isPlayingState
  }

  override fun setOnPreparedListener(listener: () -> Unit) {
    preparedListener = listener
  }
}

class SamplerViewModelInitialSetupTest {

  private lateinit var viewModel: SamplerViewModel
  private lateinit var fakePlayer: FakeMediaPlayer

  @Before
  fun setup() {
    fakePlayer = FakeMediaPlayer()
    viewModel = SamplerViewModel()

    val mediaPlayerField = SamplerViewModel::class.java.getDeclaredField("mediaPlayer")
    mediaPlayerField.isAccessible = true
    mediaPlayerField.set(viewModel, fakePlayer)

    viewModel._uiState.value =
        viewModel._uiState.value.copy(
            inputTempo = 120,
            inputPitchNote = "C",
            inputPitchOctave = 4,
            showInitialSetupDialog = true)
  }

  @Test
  fun testUpdateInputTempoSetsTempoWithinBounds() = runBlocking {
    viewModel.updateInputTempo(150)
    val state = viewModel.uiState.first()
    assertEquals(150, state.inputTempo)

    viewModel.updateInputTempo(300)
    assertEquals(300, viewModel.uiState.first().inputTempo)

    viewModel.updateInputTempo(30)
    assertEquals(30, viewModel.uiState.first().inputTempo)
  }

  @Test
  fun testIncreaseInputPitchCyclesCorrectly() = runBlocking {
    viewModel._uiState.value =
        viewModel._uiState.value.copy(inputPitchNote = "C", inputPitchOctave = 4)
    viewModel.increaseInputPitch()
    val state = viewModel.uiState.first()
    assertEquals("C#", state.inputPitchNote)
    assertEquals(4, state.inputPitchOctave)
  }

  @Test
  fun testDecreaseInputPitchCyclesCorrectly() = runBlocking {
    viewModel._uiState.value =
        viewModel._uiState.value.copy(inputPitchNote = "C", inputPitchOctave = 4)
    viewModel.decreaseInputPitch()
    val state = viewModel.uiState.first()
    assertEquals("B", state.inputPitchNote)
    assertEquals(3, state.inputPitchOctave)
  }

  @Test
  fun testConfirmInitialSetupUpdatesPitchAndTempo() = runBlocking {
    viewModel._uiState.value =
        viewModel._uiState.value.copy(
            inputTempo = 123,
            inputPitchNote = "D",
            inputPitchOctave = 5,
            showInitialSetupDialog = true)

    viewModel.confirmInitialSetup()
    val state = viewModel.uiState.first()
    assertEquals(123, state.tempo)
    assertEquals("D", state.pitchNote)
    assertEquals(5, state.pitchOctave)
    assertEquals(false, state.showInitialSetupDialog)
  }
}
