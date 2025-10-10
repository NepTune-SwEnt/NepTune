package com.neptune.neptune.ui.sampler

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.MainActivity
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeSamplerViewModel : SamplerViewModel() {
  var isAttackUpdated = false
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
  override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SamplerViewModel::class.java)) {
      return viewModel as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

// --- TESTS D'INTERFACE UTILISATEUR ---

class SamplerScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var fakeViewModel: FakeSamplerViewModel
  private val playButtonDesc = "Play"
  private val pauseButtonDesc = "Pause"
  private val saveButtonDesc = "Sauvegarder"

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
    composeTestRule.onNodeWithTag(SamplerTestTags.PLAYHEAD_CONTROLS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.SAMPLER_TABS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.TAB_BASICS_CONTENT).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SamplerTestTags.PITCH_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.TEMPO_SELECTOR).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_DECAY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_SUSTAIN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_RELEASE).assertIsDisplayed()
  }

  @Test
  fun samplerTabs_clickEQ_switchesTabContent() {
    val eqTabTag = "${SamplerTestTags.SAMPLER_TABS}_EQ"

    composeTestRule.onNodeWithTag(eqTabTag).performClick()

    assertEquals(SamplerTab.EQ, fakeViewModel.isSelectTabCalled)

    fakeViewModel.mutableUiState.value =
        fakeViewModel.uiState.value.copy(currentTab = SamplerTab.EQ)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("EQ Settings...").assertIsDisplayed()

    composeTestRule.onNodeWithTag(SamplerTestTags.KNOB_ATTACK).assertIsNotDisplayed()
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
  fun playhead_drag_callsUpdatePlaybackPosition() {
    val waveformNode = composeTestRule.onNodeWithTag(SamplerTestTags.WAVEFORM_DISPLAY)

    waveformNode.performTouchInput {
      swipeWithVelocity(start = center, end = center + Offset(x = 50f, y = 0f), endVelocity = 0f)
    }

    assertTrue(
        "updatePlaybackPosition should have been called after dragging the playhead.",
        fakeViewModel.lastPlaybackPosition != null)
  }
}
