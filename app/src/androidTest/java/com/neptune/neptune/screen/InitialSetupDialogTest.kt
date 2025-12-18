package com.neptune.neptune.ui.sampler

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.screen.FakeMediaPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InitialSetupDialogTest {

  @get:Rule val composeRule = createComposeRule()

  lateinit var viewModel: SamplerViewModel

  @Before
  fun setup() {
    viewModel = SamplerViewModel()

    viewModel._uiState.value =
        viewModel._uiState.value.copy(
            inputTempo = 120,
            inputPitchNote = "C",
            inputPitchOctave = 4,
            showInitialSetupDialog = true)

    composeRule.setContent { InitialSetupDialog(viewModel) }
  }

  @Test
  fun initialSetupDialogTempoInputUpdatesViewModel() {

    composeRule.onNodeWithTag(SamplerTestTags.INIT_TEMPO_SELECTOR).performTextInput("150")

    composeRule.waitForIdle()
    assertEquals(150, viewModel.uiState.value.inputTempo)
  }

  @Test
  fun initialSetupDialogConfirmButtonAppliesChanges() {
    composeRule.onNodeWithTag("PITCH_UP_BUTTON").performClick()
    composeRule.onNodeWithTag(SamplerTestTags.INIT_TEMPO_SELECTOR).performTextInput("140")
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(SamplerTestTags.INIT_CONFIRM_BUTTON).performClick()
    composeRule.waitForIdle()

    assertEquals(140, viewModel.uiState.value.tempo)
    assertEquals(false, viewModel.uiState.value.showInitialSetupDialog)
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

  @Test
  fun tapTempoButtonUpdatesTempo() {
    val initialTempo = viewModel.uiState.value.inputTempo

    repeat(3) { composeRule.onNodeWithTag(SamplerTestTags.TAP_TEMPO_BUTTON).performClick() }

    val newTempo = viewModel.uiState.value.inputTempo
    assertNotEquals(initialTempo, newTempo)
    assertTrue(newTempo > 0)
  }
}
