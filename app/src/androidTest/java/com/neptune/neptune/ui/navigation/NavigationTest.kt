package com.neptune.neptune.ui.navigation

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.messages.UserMessagePreview
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepositoryFirebase
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.messages.MessagesScreenTestTags
import com.neptune.neptune.ui.messages.SelectMessagesScreen
import com.neptune.neptune.ui.messages.SelectMessagesScreenTestTags
import com.neptune.neptune.ui.messages.SelectMessagesViewModel
import com.neptune.neptune.ui.picker.ImportScreenTestTags
import com.neptune.neptune.ui.post.PostScreen
import com.neptune.neptune.ui.post.PostScreenTestTags
import com.neptune.neptune.ui.post.PostUiState
import com.neptune.neptune.ui.post.PostViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Creates a mocked [SelectMessagesViewModel] populated with test users for UI testing. This has
 * been written with the help of LLMs.
 *
 * @return a mocked instance of [SelectMessagesViewModel].
 * @author Angéline Bignens
 */
private fun mockSelectMessagesViewModel(): SelectMessagesViewModel {
  val testUsers =
      listOf(
          UserMessagePreview(
              profile = Profile(uid = "uid1", username = "TestUser1", avatarUrl = ""),
              lastMessage = "Hello",
              lastTimestamp = Timestamp.now(),
              isOnline = true),
          UserMessagePreview(
              profile = Profile(uid = "uid2", username = "TestUser2", avatarUrl = ""),
              lastMessage = "Hi there",
              lastTimestamp = Timestamp.now(),
              isOnline = false))

  val mockViewModel = mockk<SelectMessagesViewModel>(relaxed = true)
  every { mockViewModel.users } returns MutableStateFlow(testUsers)
  return mockViewModel
}

/**
 * UI test for Navigation. This has been written with the help of LLMs.
 *
 * REQUIREMENTS:
 * - Firestore emulator on port 8080
 * - Auth emulator on port 9099
 * - Start them: firebase emulators:start --only firestore,auth
 *
 * On Android emulator, host is 10.0.2.2
 *
 * @author Angéline Bignens
 */
class NavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  private val host = "10.0.2.2"
  private val firestorePort = 8080
  private val authPort = 9099

  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var repo: ProfileRepositoryFirebase

  private fun setContent() {
    runBlocking {
      if (auth.currentUser == null) {
        auth.signInAnonymously().await()
      }
    }
    composeTestRule.setContent { NeptuneApp(startDestination = Screen.Main.route) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()
  }

  @Before
  fun setUp() {
    runBlocking {
      db = FirebaseFirestore.getInstance()
      auth = FirebaseAuth.getInstance()

      try {
        db.useEmulator(host, firestorePort)
      } catch (e: IllegalStateException) {
        Log.e("TestSetup", "Database emulator not running?", e)
      }

      try {
        auth.useEmulator(host, authPort)
      } catch (e: IllegalStateException) {
        Log.e("TestSetup", "Auth emulator not running?", e)
      }

      runCatching { auth.signOut() }
      auth.signInAnonymously().await()

      repo = ProfileRepositoryFirebase(db)
    }
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MESSAGE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
  }

  @Test
  fun selectingUserNavigatesToMessagesScreen() {
    var selectedUserId: String? = null

    composeTestRule.setContent {
      SelectMessagesScreen(
          goBack = {},
          onSelectUser = { selectedUserId = it },
          currentUid = "uid0",
          selectMessagesViewModel = mockSelectMessagesViewModel())
    }

    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.USER_ROW)
        .onFirst()
        .performClick()

    assert(selectedUserId == "uid1")
  }

  @Test
  fun backButtonNavigatesBackToSelectMessages() {
    composeTestRule.setContent {
      SelectMessagesScreen(
          goBack = {},
          onSelectUser = {},
          currentUid = "uid0",
          selectMessagesViewModel = mockSelectMessagesViewModel())
    }

    composeTestRule
        .onAllNodesWithTag(SelectMessagesScreenTestTags.USER_ROW)
        .onFirst()
        .performClick()

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.BACK_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(SelectMessagesScreenTestTags.SELECT_MESSAGE_SCREEN)
        .assertIsDisplayed()
  }

  @Test
  fun goBackFromProfileToMain() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
  }

  @Test
  fun bottomBarIsHiddenOnProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun mainTabIsSelectedByDefault() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomBarIsVisibleOnImportScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun editTabIsSelectedAfterClick() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun mainTabIsSelectedAfterNavigatingBackFromEdit() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun navigationToSearchTabShowsSearchScreen() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
  }

  @Test
  fun searchTabIsSelectedAfterClick() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun goBackFromProfileToSearch() {

    composeTestRule.setContent {
      NeptuneApp(navController = rememberNavController(), startDestination = Screen.Main.route)
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
  }

  @Test
  fun postButton_triggersNavigationToProjectList() {
    val navigateToProjectListMock = mockk<() -> Unit>(relaxed = true)

    val mockViewModel = mockk<MainViewModel>(relaxed = true)

    every { mockViewModel.discoverSamples } returns MutableStateFlow(emptyList())
    every { mockViewModel.followedSamples } returns MutableStateFlow(emptyList())
    every { mockViewModel.userAvatar } returns MutableStateFlow(null)
    every { mockViewModel.likedSamples } returns MutableStateFlow(emptyMap())
    every { mockViewModel.comments } returns MutableStateFlow(emptyList())
    every { mockViewModel.downloadProgress } returns MutableStateFlow<Int?>(null)
    every { mockViewModel.sampleResources } returns MutableStateFlow(emptyMap())
    every { mockViewModel.isRefreshing } returns MutableStateFlow(false)
    every { mockViewModel.activeCommentSampleId } returns MutableStateFlow(null)
    every { mockViewModel.usernames } returns MutableStateFlow(emptyMap())
    every { mockViewModel.isAnonymous } returns MutableStateFlow(false)
    every { mockViewModel.recommendedSamples } returns MutableStateFlow(emptyList())

    composeTestRule.setContent {
      MainScreen(navigateToProjectList = navigateToProjectListMock, mainViewModel = mockViewModel)
    }

    composeTestRule.onNodeWithTag(MainScreenTestTags.POST_BUTTON).performClick()

    verify(exactly = 1) { navigateToProjectListMock() }
  }

  @Test
  fun postButtonNavigateToMainScreen() {
    val navigateToMainMock = mockk<() -> Unit>(relaxed = true)
    val mockMediaPlayer = mockk<NeptuneMediaPlayer>(relaxed = true)
    val mockViewModel = mockk<PostViewModel>(relaxed = true)

    val uiStateFlow = MutableStateFlow(PostUiState())
    val imageUriFlow = MutableStateFlow<Uri?>(null)

    every { mockViewModel.uiState } returns uiStateFlow
    every { mockViewModel.localImageUri } returns imageUriFlow

    every { mockViewModel.updateTitle(any<String>()) } answers
        {
          val newTitle = firstArg<String>()
          uiStateFlow.update { it.copy(sample = it.sample.copy(name = newTitle)) }
        }
    every { mockViewModel.audioExist() } returns true

    every { mockViewModel.submitPost() } answers
        {
          uiStateFlow.update { it.copy(postComplete = true) }
        }

    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mockMediaPlayer) {
        PostScreen(navigateToMainScreen = navigateToMainMock, postViewModel = mockViewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Sweetie Banana")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_BUTTON).performScrollTo().performClick()
    composeTestRule.waitForIdle()
    verify(exactly = 1) { navigateToMainMock() }
  }

  /** Test that the bottom bar has all the button displayed */
  @Test
  fun mainScreenBottomNavigationBarHasAllButton() {
    setContent()
    // Original order: MAIN, SEARCH, PROJECTLIST, POST (now IMPORT_FILE)
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
    // The changed/new tab
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsDisplayed()
  }

  /** Test that we can click on all of the bottom bar buttons */
  @Test
  fun mainScreenBottomNavigationBarCanClickAllButtons() {
    setContent()
    listOf(
            NavigationTestTags.MAIN_TAB,
            NavigationTestTags.SEARCH_TAB,
            NavigationTestTags.PROJECTLIST_TAB, // Retained original position (3rd)
            NavigationTestTags.IMPORT_FILE_TAB) // Replaces POST_TAB (4th)
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertHasClickAction().performClick() }
  }

  /** Test that when clicking on th Import Bottom Nav it navigates correctly to the ImportScreen */
  @Test
  fun mainScreenBottomNavigationImportGoToImportScreen() {
    setContent()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB)
        .assertHasClickAction()
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ImportScreenTestTags.IMPORT_SCREEN).assertIsDisplayed()
  }
}
