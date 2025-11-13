package com.neptune.neptune.ui.sampler

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.screen.FakeMediaPlayer
import com.neptune.neptune.screen.FakeSamplerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SamplerViewModelFactory(
    private val viewModel: FakeSamplerViewModel,
    private val application: Application
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SamplerViewModel::class.java)) {
      return viewModel as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
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

    viewModel.updateInputTempo(300) // Should clamp to 200
    assertEquals(200, viewModel.uiState.first().inputTempo)

    viewModel.updateInputTempo(30) // Should clamp to 50
    assertEquals(50, viewModel.uiState.first().inputTempo)
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
