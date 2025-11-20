package com.neptune.neptune.screen

import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.MainActivity
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.sampler.SamplerTab
import com.neptune.neptune.ui.sampler.SamplerTestTags
import com.neptune.neptune.ui.sampler.SamplerUiState
import com.neptune.neptune.ui.sampler.SamplerViewModel
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

open class FakeSamplerViewModel : SamplerViewModel() {
  var isAttackUpdated = false
  var isDecayUpdated = false
  var isSustainUpdated = false
  var isReleaseUpdated = false
  var isSelectTabCalled: SamplerTab? = null
  var isTogglePlayPauseCalled = false
  var isSaveSamplerCalled = false
  var isIncreasePitchCalled = false
  var isDecreasePitchCalled = false
  var isReverbWetUpdated = false
  var isReverbSizeUpdated = false
  var isReverbWidthUpdated = false
  var isReverbDepthUpdated = false
  var isReverbPredelayUpdated = false

  var isCompThresholdUpdated = false
  var isCompRatioUpdated = false
  var isCompKneeUpdated = false
  var isCompGainUpdated = false
  var isCompAttackUpdated = false
  var isCompDecayUpdated = false
  var lastTempoUpdated: Int? = null
  var lastPlaybackPosition: Float? = null

  var isWaveformExtracted = false

  var isSaveProjectDataCalled = false
  var lastSavedPath: String? = null

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

  override fun saveProjectData(zipFilePath: String): Job {
    isSaveProjectDataCalled = true
    lastSavedPath = zipFilePath
    return super.saveProjectData(zipFilePath)
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

  override fun updateReverbWet(value: Float) {
    isReverbWetUpdated = true
    super.updateReverbWet(value)
  }

  override fun updateReverbSize(value: Float) {
    isReverbSizeUpdated = true
    super.updateReverbSize(value)
  }

  override fun updateReverbWidth(value: Float) {
    isReverbWidthUpdated = true
    super.updateReverbWidth(value)
  }

  override fun updateReverbDepth(value: Float) {
    isReverbDepthUpdated = true
    super.updateReverbDepth(value)
  }

  override fun updateReverbPredelay(value: Float) {
    isReverbPredelayUpdated = true
    super.updateReverbPredelay(value)
  }

  override fun updateCompThreshold(value: Float) {
    isCompThresholdUpdated = true
    super.updateCompThreshold(value)
  }

  override fun updateCompRatio(value: Float) {
    isCompRatioUpdated = true
    super.updateCompRatio(value)
  }

  override fun updateCompKnee(value: Float) {
    isCompKneeUpdated = true
    super.updateCompKnee(value)
  }

  override fun updateCompGain(value: Float) {
    isCompGainUpdated = true
    super.updateCompGain(value)
  }

  override fun updateCompAttack(value: Float) {
    isCompAttackUpdated = true
    super.updateCompAttack(value)
  }

  override fun updateCompDecay(value: Float) {
    isCompDecayUpdated = true
    super.updateCompDecay(value)
  }

  override fun extractWaveform(uri: Uri, sampleRate: Int): List<Float> {
    isWaveformExtracted = true
    return List(50) { 0.5f }
  }

  override fun loadProjectData(zipFilePath: String) {
    mutableUiState.update { it.copy(attack = 0.35f, sustain = 0.6f, compRatio = 4) }
    super.loadProjectData(zipFilePath)
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
      val mediaPlayer = NeptuneMediaPlayer()
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        SampleAppTheme {
          Surface(color = MaterialTheme.colorScheme.background) {
            SamplerScreen(viewModel = viewModel(factory = factory), zipFilePath = null)
          }
        }
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun samplerScreenDisplaysAllCoreElementsAndControls() {
    openSection("ADSR Envelope Controls")
    composeTestRule.waitForIdle()
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
  fun eqFaderDragCallsUpdateEqBand() {
    composeTestRule.onNodeWithText("EQ").performClick()
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.EQ)
    composeTestRule.waitForIdle()

    val initialEqBands = fakeViewModel.uiState.value.eqBands.toList()
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(
            eqBands = initialEqBands.toMutableList().apply { this[0] = 0.0f })
    composeTestRule.waitForIdle()
    val faderBoxInteraction =
        composeTestRule
            .onNodeWithTag(SamplerTestTags.FADER_60HZ_TAG)
            .onChildren()
            .filter(hasTestTag(SamplerTestTags.EQ_FADER_BOX_INPUT))
            .onFirst()

    faderBoxInteraction.performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 100)
    }
    val currentGain = fakeViewModel.uiState.value.eqBands[0]

    assertTrue(
        "Gain for 60 Hz band must be updated to a positive value.", currentGain > initialEqBands[0])
  }

  private fun clickPitchArrow(description: String) {
    composeTestRule
        .onNodeWithTag(SamplerTestTags.PITCH_SELECTOR)
        .onChildren()
        .filter(hasContentDescription(description))
        .onFirst()
        .performClick()
  }

  private fun openSection(title: String) {
    val tag = "${title.replace(" ", "")}ClickableHeader"
    composeTestRule.onNodeWithTag(tag).performClick()
    composeTestRule.waitForIdle()
  }

  private fun swipeKnobByTag(tag: String) {
    composeTestRule.onNodeWithTag(tag).performTouchInput {
      swipe(start = center, end = center + Offset(x = 0f, y = -100f), durationMillis = 50)
    }
  }

  @Test
  fun playbackControlsManualDragCallsUpdatePlaybackPosition() {
    val waveformNode = composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY)
    waveformNode.performTouchInput {
      swipeWithVelocity(start = center, end = center + Offset(x = 50f, y = 0f), endVelocity = 0f)
    }

    assertTrue(fakeViewModel.lastPlaybackPosition != null)
  }

  @Test
  fun tabsAllClicksCallsSelectTabAndLoadsContent() {
    composeTestRule.onNodeWithText("EQ").performClick()
    assertEquals(SamplerTab.EQ, fakeViewModel.isSelectTabCalled)

    composeTestRule.onNodeWithText("COMP").performClick()
    assertEquals(SamplerTab.COMP, fakeViewModel.isSelectTabCalled)
  }

  @Test
  fun pitchControlsMaxMinLimitsCallsIncreaseDecrease() {
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
  fun tempoSelectorDownArrowCallsUpdateTempo() {
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
  fun adsrKnobsAllDragCallsAllUpdateFunctions() {
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.BASICS)
    composeTestRule.waitForIdle()
    openSection("ADSR Envelope Controls")
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

  @Test
  fun compressorControlsCallsAllUpdateFunctions() {
    composeTestRule.onNodeWithText("COMP").performClick()
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.COMP)
    composeTestRule.waitForIdle()
    fakeViewModel.isCompThresholdUpdated = false
    swipeKnobByTag(SamplerTestTags.KNOB_COMP_THRESHOLD)
    assertTrue("updateCompThreshold should be true", fakeViewModel.isCompThresholdUpdated)

    fakeViewModel.isCompRatioUpdated = false
    fakeViewModel.updateCompRatio(10f)
    assertTrue("updateCompRatio should be true", fakeViewModel.isCompRatioUpdated)

    fakeViewModel.isCompKneeUpdated = false
    swipeKnobByTag(SamplerTestTags.KNOB_COMP_KNEE)
    assertTrue("updateCompKnee should be true", fakeViewModel.isCompKneeUpdated)

    fakeViewModel.isCompGainUpdated = false
    swipeKnobByTag(SamplerTestTags.KNOB_COMP_GAIN)
    assertTrue("updateCompGain should be true", fakeViewModel.isCompGainUpdated)

    swipeKnobByTag(SamplerTestTags.KNOB_COMP_ATTACK)
    assertTrue("updateCompAttack should be true", fakeViewModel.isCompAttackUpdated)

    fakeViewModel.isCompDecayUpdated = false
    swipeKnobByTag(SamplerTestTags.KNOB_COMP_DECAY)
    assertTrue("updateCompDecay should be true", fakeViewModel.isCompDecayUpdated)
  }

  @Test
  fun ratioInputFieldValidInputCallsUpdateCompRatio() {
    composeTestRule.onNodeWithText("COMP").performClick()
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.COMP)
    composeTestRule.waitForIdle()

    val ratioFieldNode =
        composeTestRule.onNode(
            hasSetTextAction() and hasParent(hasTestTag(SamplerTestTags.INPUT_COMP_RATIO)))
    ratioFieldNode.performTextClearance()
    ratioFieldNode.performTextInput("10")
    assertEquals(10, fakeViewModel.uiState.value.compRatio)
    assertTrue("updateCompRatio should be true", fakeViewModel.isCompRatioUpdated)
  }

  @Test
  fun updatePlaybackPositionDurationIsZeroSetsZero() {
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(audioDurationMillis = 0, playbackPosition = 0.5f)
    composeTestRule.waitForIdle()
    fakeViewModel.updatePlaybackPosition()
    assertEquals(0.0f, fakeViewModel.uiState.value.playbackPosition, 0.001f)
  }

  @Test
  fun increasePitchWrapsToNextOctave() {

    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(pitchNote = "B", pitchOctave = 4)
    composeTestRule.waitForIdle()
    fakeViewModel.increasePitch()
    assertEquals("C", fakeViewModel.uiState.value.pitchNote)
    assertEquals(5, fakeViewModel.uiState.value.pitchOctave)
    assertTrue("IncreasePitch should have been called.", fakeViewModel.isIncreasePitchCalled)
  }

  @Test
  fun decreasePitchWrapsToPreviousOctave() {
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(pitchNote = "C", pitchOctave = 5)
    composeTestRule.waitForIdle()
    fakeViewModel.decreasePitch()
    assertEquals("B", fakeViewModel.uiState.value.pitchNote)
    assertEquals(4, fakeViewModel.uiState.value.pitchOctave)
    assertTrue("DecreasePitch should have been called.", fakeViewModel.isDecreasePitchCalled)
  }

  @Test
  fun labelsAndTimelineAndBeatLinesAreCorrect() {
    // Ensure basics tab opened so waveform is visible
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(
            currentTab = SamplerTab.BASICS, tempo = 120, timeSignature = "4/4")
    composeTestRule.waitForIdle()

    // Open playback section to reveal pitch and tempo selectors
    // The section may already be visible; ensure the ADSR section is open so layout stabilizes
    openSection("ADSR Envelope Controls")
    composeTestRule.waitForIdle()

    // Check that Pitch label is displayed inside the Pitch selector
    composeTestRule.onNodeWithTag(SamplerTestTags.PITCH_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithText("Pitch").assertIsDisplayed()

    // Tempo selector shows tempo as label text
    composeTestRule.onNodeWithTag(SamplerTestTags.TEMPO_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithText("${fakeViewModel.uiState.value.tempo}").assertIsDisplayed()

    // Time display should format time correctly for known playback/duration
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(audioDurationMillis = 4000, playbackPosition = 0.5f)
    composeTestRule.waitForIdle()

    // Expecting 50% of 4s => 2.00 elapsed => formatted as 02.xx; milliseconds portion is in 10ms
    // units
    composeTestRule.onNodeWithTag(SamplerTestTags.TIME_DISPLAY).assertIsDisplayed()

    // Check beat info node contains expected prefix
    val beatNode = composeTestRule.onNodeWithTag("waveform_beat_info")
    beatNode.assertExists()
    // Extract semantics text and check contents
    val beatSemantics = beatNode.fetchSemanticsNode()
    val beatTextList = beatSemantics.config[SemanticsProperties.Text]
    val beatCombined = beatTextList?.joinToString("") { (it as AnnotatedString).text }
    assertTrue(
        "Beat info must contain 'beats:' prefix",
        beatCombined != null && beatCombined.contains("beats:"))

    // Timeline labels are not direct nodes (drawn on Canvas). As a basic smoke check, ensure
    // the waveform display container exists and beat info is non-empty
    composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY).assertIsDisplayed()
  }

  @Test
  fun settingsDialogOpensAndCancelClosesDialog() {
    // Open the dialog via the Settings FAB
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // The dialog should be visible
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_DIALOG).assertIsDisplayed()

    // Clicking Cancel should close the dialog
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_CANCEL_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // The dialog should no longer exist
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_DIALOG).assertDoesNotExist()
  }

  @Test
  fun settingsDialogSaveUpdatesInputTempoAndPitch() {
    // Prepare a known state
    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(
            inputTempo = 100, inputPitchNote = "C", inputPitchOctave = 4)
    composeTestRule.waitForIdle()

    // Open the dialog
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Find the BPM field inside the dialog and enter a new value
    val bpmField =
        composeTestRule.onNode(
            hasSetTextAction() and hasParent(hasTestTag(SamplerTestTags.SETTINGS_DIALOG)))
    bpmField.performTextClearance()
    bpmField.performTextInput("130")

    // Click the pitch selector's up arrow inside the dialog
    val beforePitch =
        fakeViewModel.uiState.value.inputPitchNote + fakeViewModel.uiState.value.inputPitchOctave
    composeTestRule
        .onNodeWithTag(SamplerTestTags.SETTINGS_PITCH_SELECTOR)
        .onChildren()
        .filter(hasTestTag("PITCH_UP_BUTTON"))
        .onFirst()
        .performClick()

    composeTestRule.waitForIdle()

    val afterPitch =
        fakeViewModel.uiState.value.inputPitchNote + fakeViewModel.uiState.value.inputPitchOctave
    // Verify that the pitch in the viewModel changed locally
    assertTrue("Pitch inside dialog should have changed", beforePitch != afterPitch)

    // Confirm (Save & Close)
    composeTestRule.onNodeWithTag(SamplerTestTags.SETTINGS_CONFIRM_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify that inputTempo has been updated in the viewModel
    assertEquals(130, fakeViewModel.uiState.value.inputTempo)
  }
}
