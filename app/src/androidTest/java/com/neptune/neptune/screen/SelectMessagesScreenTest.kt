package com.neptune.neptune.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.neptune.neptune.ui.messages.SelectMessagesScreen
import com.neptune.neptune.ui.messages.SelectMessagesScreenTestTags
import com.neptune.neptune.ui.messages.SelectMessagesViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the SelectMessagesScreen.This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class SelectMessagesScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: SelectMessagesViewModel

  @Before
  fun setup() {
    viewModel = SelectMessagesViewModel()
  }

  private fun setContent(goBack: () -> Unit = {}, onSelectUser: (String) -> Unit = {}) {
    composeTestRule.setContent {
      SelectMessagesScreen(
          goBack = goBack, onSelectUser = onSelectUser, selectMessagesViewModel = viewModel)
    }
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule
        .onNodeWithTag(SelectMessagesScreenTestTags.SELECT_MESSAGE_SCREEN)
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.LAST_MESSAGE, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.MESSAGES_TITLE).assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.USERNAME, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.TIMESTAMP, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.ONLINE_INDICATOR, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.AVATAR, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.USER_ROW, useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.USER_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.TOP_DIVIDER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  /** Tests that clicking on the Back Button correctly trigger a callback */
  @Test
  fun testBackButtonTriggersCallback() {
    var backClicked = false
    setContent(goBack = { backClicked = true })
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.BACK_BUTTON).performClick()
    assert(backClicked)
  }

  /** Tests that clicking on a User correctly trigger a callback */
  @Test
  fun testClickingUserTriggersCallback() {
    var selectedUserId: String? = null
    setContent(onSelectUser = { selectedUserId = it })
    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.USER_ROW, useUnmergedTree = true)
        .onFirst()
        .performClick()
    assert(selectedUserId != null)
  }

  /** Tests that when we don't have any conversations the text correctly display */
  @Test
  fun testNoConversationsTextWhenEmpty() {
    val emptyViewModel = SelectMessagesViewModel(initialUsers = emptyList())
    composeTestRule.setContent {
      SelectMessagesScreen(goBack = {}, onSelectUser = {}, selectMessagesViewModel = emptyViewModel)
    }

    composeTestRule
        .onNodeWithTag(SelectMessagesScreenTestTags.NO_CONVERSATIONS_TEXT)
        .assertIsDisplayed()
  }
}
