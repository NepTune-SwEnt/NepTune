// filepath:
// c:\Users\uri\Documents\epfl\ba5\swent\NepTune\app\src\androidTest\java\com\neptune\neptune\ui\sampler\HelpPagesTest.kt
package com.neptune.neptune.ui.sampler

import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.MainActivity
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.theme.SampleAppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpPagesTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Before
  fun setUp() {
    // Use a real media player (harmless in tests) and set a simple composition root
    val mediaPlayer = NeptuneMediaPlayer()
    composeRule.activity.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        SampleAppTheme {
          Surface {
            // empty root; each test will set its own content when needed
          }
        }
      }
    }
    composeRule.waitForIdle()
  }

  @Test
  fun helpADSRDisplaysTitleAndBody() {
    val title = composeRule.activity.getString(com.neptune.neptune.R.string.tab_adsr)
    val body = composeRule.activity.getString(com.neptune.neptune.R.string.help_adsr_text)

    composeRule.activity.setContent {
      SampleAppTheme { Surface { HelpDialog(selectedTab = 2, onTabSelected = {}, onClose = {}) } }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.onNodeWithText(body).assertIsDisplayed()
  }

  @Test
  fun helpReverbDisplaysTitleAndBody() {
    val title = composeRule.activity.getString(com.neptune.neptune.R.string.tab_reverb)
    val body = composeRule.activity.getString(com.neptune.neptune.R.string.help_reverb_text)

    composeRule.activity.setContent {
      SampleAppTheme { Surface { HelpDialog(selectedTab = 3, onTabSelected = {}, onClose = {}) } }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.onNodeWithText(body).assertIsDisplayed()
  }

  @Test
  fun helpEqualizerDisplaysTitleAndBody() {
    val title = composeRule.activity.getString(com.neptune.neptune.R.string.tab_equalizer)
    val body = composeRule.activity.getString(com.neptune.neptune.R.string.help_equalizer_text)

    composeRule.activity.setContent {
      SampleAppTheme { Surface { HelpDialog(selectedTab = 4, onTabSelected = {}, onClose = {}) } }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.onNodeWithText(body).assertIsDisplayed()
  }

  @Test
  fun helpCompressorDisplaysTitleAndBody() {
    val title = composeRule.activity.getString(com.neptune.neptune.R.string.tab_compressor)
    val body = composeRule.activity.getString(com.neptune.neptune.R.string.help_compressor_text)

    composeRule.activity.setContent {
      SampleAppTheme { Surface { HelpDialog(selectedTab = 5, onTabSelected = {}, onClose = {}) } }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.onNodeWithText(body).assertIsDisplayed()
  }
}
