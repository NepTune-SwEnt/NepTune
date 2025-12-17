@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)

package com.neptune.neptune.ui.sampler

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@RunWith(AndroidJUnit4::class)
class ScrollablePianoComposeTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeViewModel =
      object : SamplerViewModel() {
        init {
          _uiState.value = _uiState.value.copy(inputPitchNote = "C", inputPitchOctave = 4)
        }

        override fun startADSRSampleWithPitch(semitoneOffset: Int) {}

        override fun stopADSRSample() {}
      }

  @Test
  fun adsrButton_toggle_changesTextAndIcon() {
    val isVisible = mutableStateOf(false)

    composeTestRule.setContent {
      SampleAppTheme {
        ADSRTestButton(
            isPianoVisible = isVisible.value,
            onTogglePiano = { isVisible.value = !isVisible.value })
      }
    }

    composeTestRule.onNodeWithText("Open Keyboard").assertIsDisplayed()

    composeTestRule.onNodeWithTag("ADSR_TEST_BUTTON").performClick()

    composeTestRule.onNodeWithText("Close Keyboard").assertIsDisplayed()

    composeTestRule.onNodeWithTag("ADSR_TEST_BUTTON").performClick()
    composeTestRule.onNodeWithText("Open Keyboard").assertIsDisplayed()
  }

  @Test
  fun scrollablePiano_displaysCorrectNumberOfOctaves() {
    val listState = LazyListState()

    composeTestRule.setContent { ScrollablePiano(viewModel = fakeViewModel, listState = listState) }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { runBlocking { listState.scrollToItem(0) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("C3").assertIsDisplayed()

    composeTestRule.runOnIdle { runBlocking { listState.scrollToItem(1) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("C4").assertIsDisplayed()

    composeTestRule.runOnIdle { runBlocking { listState.scrollToItem(2) } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("C5").assertIsDisplayed()
  }
}
