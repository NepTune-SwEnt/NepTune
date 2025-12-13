/* This test class was partially implemented with AI assistance */
package com.neptune.neptune.ui.follow

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.theme.SampleAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FollowListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val followers =
      listOf(
          FollowListUserItem(uid = "u1", username = "Alice", isFollowedByCurrentUser = true),
          FollowListUserItem(uid = "u2", username = "Bob"))

  private val following = listOf(FollowListUserItem(uid = "u3", username = "Carol"))

  @Test
  fun followListDisplaysUsersAndButtonsForFollowersTab() {
    composeTestRule.setContent {
      SampleAppTheme {
        FollowListScreen(
            state =
                FollowListUiState(
                    activeTab = FollowListTab.FOLLOWERS,
                    followers = followers,
                    following = following,
                    isCurrentUserAnonymous = false),
            onBack = {},
            onTabSelected = {},
            onRefresh = {},
            onToggleFollow = { _, _ -> },
            navigateToOtherUserProfile = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.ROOT, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TOP_BAR, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TAB_ROW, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TAB_FOLLOWERS, useUnmergedTree = true)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${FollowListScreenTestTags.ITEM}/u1", useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("${FollowListScreenTestTags.AVATAR}/u1", useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
    composeTestRule.onNodeWithText("Follow back").assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(FollowListScreenTestTags.FOLLOW_BUTTON)[0].assertIsEnabled()
  }

  @Test
  fun clickingFollowingTabNotifiesCallback() {
    var selectedTab: FollowListTab? = null

    composeTestRule.setContent {
      SampleAppTheme {
        FollowListScreen(
            state = FollowListUiState(activeTab = FollowListTab.FOLLOWERS),
            onBack = {},
            onTabSelected = { selectedTab = it },
            onRefresh = {},
            onToggleFollow = { _, _ -> },
            navigateToOtherUserProfile = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TAB_FOLLOWING, useUnmergedTree = true)
        .performClick()

    assertEquals(FollowListTab.FOLLOWING, selectedTab)
  }

  @Test
  fun emptyStateShownWhenNoUsersAndNotLoading() {
    var refreshed = false

    composeTestRule.setContent {
      SampleAppTheme {
        FollowListScreen(
            state = FollowListUiState(activeTab = FollowListTab.FOLLOWERS),
            onBack = {},
            onTabSelected = {},
            onRefresh = { refreshed = true },
            onToggleFollow = { _, _ -> },
            navigateToOtherUserProfile = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST_EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST_EMPTY_REFRESH).performClick()

    composeTestRule.runOnIdle { assertTrue(refreshed) }
  }

  @Test
  fun loadingStateShowsProgressIndicator() {
    composeTestRule.setContent {
      SampleAppTheme {
        FollowListScreen(
            state =
                FollowListUiState(activeTab = FollowListTab.FOLLOWING, isLoadingFollowing = true),
            onBack = {},
            onTabSelected = {},
            onRefresh = {},
            onToggleFollow = { _, _ -> },
            navigateToOtherUserProfile = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.LIST_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.LIST_LOADING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun followButtonDisablesForAnonymousOrWhenActionInProgress() {
    composeTestRule.setContent {
      SampleAppTheme {
        FollowListScreen(
            state =
                FollowListUiState(
                    activeTab = FollowListTab.FOLLOWERS,
                    followers =
                        listOf(
                            FollowListUserItem(uid = "u1", username = "Anon target"),
                            FollowListUserItem(
                                uid = "u2", username = "Loading", isActionInProgress = true)),
                    isCurrentUserAnonymous = true),
            onBack = {},
            onTabSelected = {},
            onRefresh = {},
            onToggleFollow = { _, _ -> },
            navigateToOtherUserProfile = {})
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onAllNodesWithTag(FollowListScreenTestTags.FOLLOW_BUTTON)
        .get(0)
        .assertIsNotEnabled()
    composeTestRule
        .onAllNodesWithTag(FollowListScreenTestTags.FOLLOW_BUTTON)[1]
        .assertIsNotEnabled()
    composeTestRule.onNodeWithTag(FollowListScreenTestTags.LIST).assertIsDisplayed()
  }
}
