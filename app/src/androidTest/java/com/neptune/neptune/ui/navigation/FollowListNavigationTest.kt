package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.follow.FollowListScreenTestTags
import com.neptune.neptune.ui.follow.FollowListTab
import com.neptune.neptune.ui.settings.SettingsViewModel
import com.neptune.neptune.ui.theme.SampleAppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FollowListNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var previousProfileRepository: ProfileRepository
  private var navController: NavHostController? = null

  @Before
  fun setUp() {
    previousProfileRepository = ProfileRepositoryProvider.repository
    ProfileRepositoryProvider.repository = FakeProfileRepository()

    mockkStatic(FirebaseAuth::class)
    val fakeUser =
        mockk<FirebaseUser>(relaxed = true) {
          every { isAnonymous } returns false
          every { uid } returns "nav-test-user"
        }
    val fakeAuth =
        mockk<FirebaseAuth>(relaxed = true) {
          every { currentUser } returns fakeUser
          every { addAuthStateListener(any()) } answers {}
          every { removeAuthStateListener(any()) } answers {}
        }
    every { FirebaseAuth.getInstance() } returns fakeAuth
  }

  @After
  fun tearDown() {
    ProfileRepositoryProvider.repository = previousProfileRepository
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun followListRouteUsesInitialTabArgument() {
    val mockSettingsViewModel = mockk<SettingsViewModel>(relaxed = true)
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      SampleAppTheme {
        NeptuneApp(
            navController = controller,
            startDestination = Screen.Main.route,
            settingsViewModel = mockSettingsViewModel)
      }
    }
    composeTestRule.runOnUiThread {
      navController?.navigate(Screen.FollowList.createRoute(FollowListTab.FOLLOWING))
    }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TAB_FOLLOWING, useUnmergedTree = true)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TITLE, useUnmergedTree = true)
        .assertTextContains("Following", substring = true)
  }

  @Test
  fun invalidFollowListArgumentFallsBackToFollowersTab() {
    val mockSettingsViewModel = mockk<SettingsViewModel>(relaxed = true)
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      SampleAppTheme {
        NeptuneApp(
            navController = controller,
            startDestination = Screen.Main.route,
            settingsViewModel = mockSettingsViewModel)
      }
    }
    composeTestRule.runOnUiThread { navController?.navigate("follow_list/INVALID") }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TAB_FOLLOWERS, useUnmergedTree = true)
        .assertIsSelected()
    composeTestRule
        .onNodeWithTag(FollowListScreenTestTags.TITLE, useUnmergedTree = true)
        .assertTextContains("Followers", substring = true)
  }
}
