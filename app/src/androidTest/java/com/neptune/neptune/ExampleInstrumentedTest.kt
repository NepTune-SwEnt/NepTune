package com.neptune.neptune

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.neptune.neptune.screen.MainScreen
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest : TestCase() {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun test() = run {
    step("Start Main Activity") {
      ComposeScreen.onComposeScreen<MainScreen>(composeTestRule) {
        simpleText {
          assertIsDisplayed()
          assertTextEquals("Hello Android!")
        }
      }
    }
  }
}
