package com.neptune.neptune.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.authentification.EmailAuthUiState
import com.neptune.neptune.ui.authentification.SignInScreen
import com.neptune.neptune.ui.authentification.SignInScreenTags
import com.neptune.neptune.ui.authentification.SignInStatus
import com.neptune.neptune.ui.authentification.SignInViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/** UI Tests for the [SignInScreen] composable that match the actual implementation. */
class SignInScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Helper function to set up the Composable with a mocked ViewModel. */
  private fun setContent(signInViewModel: SignInViewModel, navigateMain: () -> Unit = {}) {
    if (signInViewModel.isOnline !is MutableStateFlow) {
      every { signInViewModel.isOnline } returns MutableStateFlow(true)
    }
    composeTestRule.setContent {
      SignInScreen(signInViewModel = signInViewModel, navigateMain = navigateMain)
    }
  }

  /** Tests that the initial UI displays all elements and the button is enabled by default. */
  @Test
  fun signInScreenDisplaysCoreElementsAndButtonIsEnabledWhenSignedOut() {
    // GIVEN: The ViewModel is in the SIGNED_OUT state (default)
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns MutableStateFlow(SignInStatus.SIGNED_OUT)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    setContent(signInViewModel = mockViewModel)

    // THEN: The welcome title and login button should be displayed
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsDisplayed()

    // AND: The button should be enabled
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsEnabled()
  }

  /** Tests that clicking the enabled login button triggers the ViewModel. */
  @Test
  fun clickingLoginButtonTriggersViewModelWhenEnabled() {
    // GIVEN: The button is enabled
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns MutableStateFlow(SignInStatus.SIGNED_OUT)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    setContent(signInViewModel = mockViewModel)

    // WHEN: The user clicks the login button
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).performClick()

    // THEN: The beginSignIn method on the ViewModel should be called
    verify(exactly = 1) { mockViewModel.beginSignIn(any()) }
  }

  /** Tests that the login button is correctly disabled when sign-in is requested. */
  @Test
  fun loginButtonIsDisabledWhenSignInIsRequested() {
    // GIVEN: The ViewModel is in the SIGN_IN_REQUESTED state
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns MutableStateFlow(SignInStatus.SIGN_IN_REQUESTED)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    // WHEN: The UI is rendered
    setContent(signInViewModel = mockViewModel)

    // THEN: The button is displayed but is NOT enabled
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsNotEnabled()
  }

  /** Tests that the login button is also disabled during Firebase authentication. */
  @Test
  fun loginButtonIsDisabledWhenInProgressFirebaseAuth() {
    // GIVEN: The ViewModel is authenticating with Firebase
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns
        MutableStateFlow(SignInStatus.IN_PROGRESS_FIREBASE_AUTH)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    // WHEN: The UI is rendered
    setContent(signInViewModel = mockViewModel)

    // THEN: The button still exists but is NOT enabled
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsNotEnabled()
  }

  /** Tests that the button becomes enabled again after an error, allowing the user to retry. */
  @Test
  fun loginButtonIsEnabledAfterError() {
    // GIVEN: The ViewModel is in the ERROR state
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns MutableStateFlow(SignInStatus.ERROR)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    // WHEN: The UI is rendered
    setContent(signInViewModel = mockViewModel)

    // THEN: The button is displayed and is enabled
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).assertIsEnabled()
  }
  /** Ensures that a click is ignored when the button is disabled. */
  @Test
  fun clickingDisabledLoginButtonDoesNotTriggerViewModel() {
    // GIVEN: The ViewModel is in a state where the button is disabled
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.signInStatus } returns MutableStateFlow(SignInStatus.SIGN_IN_REQUESTED)
    every { mockViewModel.emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())

    setContent(signInViewModel = mockViewModel)

    // WHEN: A click is attempted on the (disabled) button
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_BUTTON).performClick()

    // THEN: The beginSignIn method should NOT have been called
    verify(exactly = 0) { mockViewModel.beginSignIn(any()) }
  }

  /**
   * Verifies the navigation lambda passed to the Composable is correctly wired to the ViewModel's
   * initialization process.
   */
  @Test
  fun successfulNavigationIsCalledFromViewModelInteraction() {
    val navigateLambdaSlot = slot<() -> Unit>()

    val mockViewModel =
        mockk<SignInViewModel> {
          every { initialize(any(), capture(navigateLambdaSlot), any()) } returns Unit
          every { signInStatus } returns MutableStateFlow(SignInStatus.SIGNED_OUT)
          every { emailAuthUiState } returns MutableStateFlow(EmailAuthUiState())
          every { isOnline } returns MutableStateFlow(true)
        }

    val mockNavigateMain: () -> Unit = mockk(relaxed = true)

    // WHEN
    setContent(signInViewModel = mockViewModel, navigateMain = mockNavigateMain)

    // AND: Execute the lambda that the ViewModel captured
    navigateLambdaSlot.captured.invoke()

    // THEN
    verify(exactly = 1) { mockNavigateMain() }
  }
}
