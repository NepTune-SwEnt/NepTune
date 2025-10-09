package com.neptune.neptune.screen

import android.app.Activity
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.ui.authentification.SignInStatus
import com.neptune.neptune.ui.authentification.SignInViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the [SignInViewModel]. This class tests the business logic of the sign-in flow
 * without relying on the Android UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  // Test dependencies that will be mocked
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockActivity: Activity

  // The class we are testing
  private lateinit var viewModel: SignInViewModel

  // A special coroutine dispatcher for tests
  private val testDispatcher = StandardTestDispatcher()

  // A fake OAuth client ID for testing purposes
  private val fakeOauthClientId = "fake-oauth-client-id-for-testing.apps.googleusercontent.com"

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockkStatic(FirebaseAuth::class)
    mockFirebaseAuth = mockk(relaxed = true)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    mockCredentialManager = mockk(relaxed = true)
    mockActivity = mockk(relaxed = true)
    viewModel = SignInViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initialize - when user is already signed in, navigates to main`() = runTest {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    val mockNavigate: () -> Unit = mockk(relaxed = true)
    viewModel.initialize(mockCredentialManager, mockNavigate, fakeOauthClientId)
    assertEquals(mockUser, viewModel.currentUser.value)
    verify(exactly = 1) { mockNavigate() }
  }

  @Test
  fun `initialize - when no user is signed in, does not navigate`() = runTest {
    every { mockFirebaseAuth.currentUser } returns null
    val mockNavigate: () -> Unit = mockk(relaxed = true)
    viewModel.initialize(mockCredentialManager, mockNavigate, fakeOauthClientId)
    assertNull(viewModel.currentUser.value)
    verify(exactly = 0) { mockNavigate() }
  }

  @Test
  fun `beginSignIn - on request, status changes to SIGN_IN_REQUESTED`() = runTest {
    viewModel.initialize(mockk(), {}, fakeOauthClientId)
    viewModel.beginSignIn(mockActivity)
    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
  }
}
