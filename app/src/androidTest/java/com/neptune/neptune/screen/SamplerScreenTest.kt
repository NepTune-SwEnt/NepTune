package com.neptune.neptune.screen

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.MainActivity
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.sampler.SamplerTab
import com.neptune.neptune.ui.sampler.SamplerTestTags
import com.neptune.neptune.ui.sampler.SamplerUiState
import com.neptune.neptune.ui.sampler.SamplerViewModel
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeSamplerViewModel : SamplerViewModel() {
  var isAttackUpdated = false
  var isDecayUpdated = false
  var isSustainUpdated = false
  var isReleaseUpdated = false
  var isSelectTabCalled: SamplerTab? = null
  var isTogglePlayPauseCalled = false
  var isSaveSamplerCalled = false
  var isIncreasePitchCalled = false
  var isDecreasePitchCalled = false
  var lastTempoUpdated: Int? = null
  var lastPlaybackPosition: Float? = null

  val mutableUiState: MutableStateFlow<SamplerUiState> = _uiState

  override fun updateAttack(value: Float) {
    isAttackUpdated = true
    super.updateAttack(value)
  }

  override fun updateDecay(value: Float) {
    isDecayUpdated = true
    super.updateDecay(value)
  }

  override fun updateSustain(value: Float) {
    isSustainUpdated = true
    super.updateSustain(value)
  }

  override fun updateRelease(value: Float) {
    isReleaseUpdated = true
    super.updateRelease(value)
  }

  override fun selectTab(tab: SamplerTab) {
    isSelectTabCalled = tab
    super.selectTab(tab)
  }

  override fun togglePlayPause() {
    isTogglePlayPauseCalled = true
    super.togglePlayPause()
  }

  override fun saveSampler() {
    isSaveSamplerCalled = true
  }

  override fun increasePitch() {
    isIncreasePitchCalled = true
    super.increasePitch()
  }

  override fun decreasePitch() {
    isDecreasePitchCalled = true
    super.decreasePitch()
  }

  override fun updateTempo(newTempo: Int) {
    lastTempoUpdated = newTempo
    super.updateTempo(newTempo)
  }

  override fun updatePlaybackPosition(position: Float) {
    lastPlaybackPosition = position
    super.updatePlaybackPosition(position)
  }
}

class SamplerViewModelFactory(private val viewModel: FakeSamplerViewModel) :
    ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SamplerViewModel::class.java)) {
      return viewModel as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

class SamplerScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
  private lateinit var fakeViewModel: FakeSamplerViewModel
  private val playButtonDesc = "Play"
  private val saveButtonDesc = "Save"

  @Before
  fun setup() {
    fakeViewModel = FakeSamplerViewModel()
    val factory = SamplerViewModelFactory(fakeViewModel)
    composeTestRule.activity.setContent {
      SampleAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
          SamplerScreen(viewModel = viewModel(factory = factory))
        }
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun samplerScreen_displaysAllCoreElementsAndControls() {
    composeTestRule.onNodeWithTag(SamplerTestTags.SCREEN_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_DECAY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_SUSTAIN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_RELEASE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.PITCH_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.TEMPO_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.SAMPLER_TABS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY).assertIsDisplayed()
  }

  @Test
  fun adsrKnobs_callsAllUpdateFunctions() {
    fakeViewModel.updateAttack(1.5f)
    assertTrue("updateAttack should be true", fakeViewModel.isAttackUpdated)

    fakeViewModel.updateDecay(1.5f)
    assertTrue("updateDecay should be true", fakeViewModel.isDecayUpdated)

    fakeViewModel.updateSustain(1.5f)
    assertTrue("updateSustain should be true", fakeViewModel.isSustainUpdated)

    fakeViewModel.updateRelease(1.5f)
    assertTrue("updateRelease should be true", fakeViewModel.isReleaseUpdated)
  }

  private fun clickPitchArrow(description: String) {
    composeTestRule
        .onNodeWithTag(SamplerTestTags.PITCH_SELECTOR)
        .onChildren()
        .filter(hasContentDescription(description))
        .onFirst()
        .performClick()
  }

  @Test
  fun playbackControls_manualDrag_callsUpdatePlaybackPosition() {
    val waveformNode = composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY)
    waveformNode.performTouchInput {
      swipeWithVelocity(start = center, end = center + Offset(x = 50f, y = 0f), endVelocity = 0f)
    }

    assertTrue(fakeViewModel.lastPlaybackPosition != null)
  }

  @Test
  fun tabs_allClicks_callsSelectTabAndLoadsContent() {
    composeTestRule.onNodeWithText("EQ").performClick()
    assertEquals(SamplerTab.EQ, fakeViewModel.isSelectTabCalled)

    composeTestRule.onNodeWithText("COMP").performClick()
    assertEquals(SamplerTab.COMP, fakeViewModel.isSelectTabCalled)
    composeTestRule.onNodeWithText("TEMP").performClick()
    assertEquals(SamplerTab.TEMP, fakeViewModel.isSelectTabCalled)
  }

  @Test
  fun pitchControls_maxMinLimits_callsIncreaseDecrease() {
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(pitchNote = "C", pitchOctave = 1)
    composeTestRule.waitForIdle()
    val initialPitch = fakeViewModel.uiState.value.fullPitch

    clickPitchArrow("Decrease")
    assertTrue("DecreasePitch should have been called.", fakeViewModel.isDecreasePitchCalled)
    assertEquals(initialPitch, fakeViewModel.uiState.value.fullPitch)

    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(pitchNote = "B", pitchOctave = 7)
    composeTestRule.waitForIdle()
    val finalPitch = fakeViewModel.uiState.value.fullPitch

    clickPitchArrow("Increase")

    assertTrue("IncreasePitch should have been called.", fakeViewModel.isIncreasePitchCalled)
    assertEquals(finalPitch, fakeViewModel.uiState.value.fullPitch)
  }

  @Test
  fun saveButton_callsSaveSampler() {
    composeTestRule
        .onNodeWithContentDescription(saveButtonDesc)
        .assertHasClickAction()
        .performClick()
    assertTrue("saveSampler should have been called.", fakeViewModel.isSaveSamplerCalled)
  }

  @Test
  fun tempoSelector_downArrow_callsUpdateTempo() {
    fakeViewModel.mutableUiState.value = fakeViewModel.uiState.value.copy(tempo = 100)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(SamplerTestTags.TEMPO_SELECTOR)
        .onChildren()
        .filter(hasContentDescription("Decrease"))
        .onFirst()
        .performClick()

    assertEquals(99, fakeViewModel.lastTempoUpdated)
  }

  @Test
  fun playbackControls_playPauseLogic_coversAnimationBranches() {
    composeTestRule.onNodeWithContentDescription(playButtonDesc).performClick()
    fakeViewModel.mutableUiState.value = fakeViewModel.uiState.value.copy(isPlaying = true)
    composeTestRule.waitForIdle()
    fakeViewModel.updatePlaybackPosition(1.0f)
    assertTrue(!fakeViewModel.uiState.value.isPlaying)
  }

  @Test
  fun adsrKnobs_allDrag_callsAllUpdateFunctions() {
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.BASICS)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }
    assertTrue("updateAttack should be true", fakeViewModel.isAttackUpdated)

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_DECAY).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }
    assertTrue("updateDecay should be true", fakeViewModel.isDecayUpdated)

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_SUSTAIN).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }
    assertTrue("updateSustain should be true", fakeViewModel.isSustainUpdated)

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_RELEASE).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }
    assertTrue("updateRelease should be true", fakeViewModel.isReleaseUpdated)
  }
}
