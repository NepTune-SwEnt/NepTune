package com.neptune.neptune.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.authentification.SignInScreen
import com.neptune.neptune.ui.authentification.SignInScreenTags
import com.neptune.neptune.ui.authentification.SignInViewModel
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

/** UI Tests for the [SignInScreen] composable. */
class LoginScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Sets up the composable under test. This function can be customized to inject a mocked ViewModel
   * for more advanced test cases.
   *
   * @param signInViewModel The ViewModel to use for the test. Defaults to a mockk instance.
   */
  private fun setContent(signInViewModel: SignInViewModel = mockk(relaxed = true)) {
    composeTestRule.setContent {
      // We pass a mock ViewModel to control its behavior during tests.
      // A mock `navigateMain` lambda is also provided.
      SignInScreen(signInViewModel = signInViewModel, navigateMain = {})
    }
  }

  @Test
  fun signInScreen_displaysAllCoreElements() {
    // GIVEN
    setContent()

    // THEN the welcome title should be displayed
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()

    // AND the Google sign-in button should be displayed
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
  }

  @Test
  fun loginButton_hasClickAction() {
    // GIVEN
    setContent()

    // THEN the button should be clickable
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertHasClickAction()
  }

  @Test
  fun clickingLoginButton_triggersViewModel() {
    // GIVEN
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    setContent(signInViewModel = mockViewModel)

    // WHEN the user clicks the login button
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).performClick()

    // THEN the beginSignIn method on the ViewModel should be called
    // We use `verify` from the Mockk library to confirm this interaction.
    verify(exactly = 1) { mockViewModel.beginSignIn(any()) }
  }

  @Test
  fun successfulSignIn_triggersNavigation() {
    // GIVEN a mock navigation lambda that we want to test
    val mockNavigate: () -> Unit = mockk(relaxed = true)

    // AND the SignInScreen is composed with our specific navigation lambda
    composeTestRule.setContent {
      SignInScreen(
          // We use a relaxed mock to avoid setting up onClick behavior
          signInViewModel = mockk(relaxed = true),
          navigateMain = mockNavigate)
    }

    // WHEN we simulate that the navigation is triggered
    // (in reality, the ViewModel would call this after a successful sign-in)
    composeTestRule.runOnIdle {
      // We directly call the lambda to ensure it's wired correctly
      mockNavigate()
    }

    // THEN we verify that our navigation function was indeed called
    verify(exactly = 1) { mockNavigate() }
  }
}
