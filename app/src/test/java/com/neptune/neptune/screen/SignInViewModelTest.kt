package com.neptune.neptune.screen

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.ui.authentification.SignInStatus
import com.neptune.neptune.ui.authentification.SignInViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the [SignInViewModel]. This class tests the business logic of the sign-in flow
 * without relying on the Android UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockActivity: Activity
  private lateinit var viewModel: SignInViewModel
  private val testDispatcher = StandardTestDispatcher()
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
    io.mockk.unmockkAll()
  }

  @Test
  fun initial_status_is_before_initialization() {
    assertEquals(SignInStatus.BEFORE_INITIALIZATION, viewModel.signInStatus.value)
  }

  @Test
  fun initialize_when_user_is_signed_in_navigates() = runTest {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    var hasNavigated = false
    viewModel.initialize(mockCredentialManager, { hasNavigated = true }, fakeOauthClientId)
    assertEquals(mockUser, viewModel.currentUser.value)
    assertTrue("Navigation should have occurred", hasNavigated)
  }

  @Test
  fun initialize_when_no_user_is_signed_in_does_not_navigate() = runTest {
    every { mockFirebaseAuth.currentUser } returns null
    var hasNavigated = false
    viewModel.initialize(mockCredentialManager, { hasNavigated = true }, fakeOauthClientId)
    assertNull(viewModel.currentUser.value)
    assertFalse("Navigation should not have occurred", hasNavigated)
  }

  @Test
  fun beginSignIn_changes_status_to_requested() = runTest {
    viewModel.initialize(mockk(), {}, fakeOauthClientId)
    viewModel.beginSignIn(mockActivity)
    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignIn_when_user_cancels_changes_status_to_signed_out() = runTest {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        GetCredentialCancellationException("User cancelled")
    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignIn_when_generic_error_changes_status_to_error() = runTest {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        RuntimeException("Network error")
    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(SignInStatus.ERROR, viewModel.signInStatus.value)
  }

  @Test
  fun handleSignIn_with_non_google_credential_does_nothing() = runTest {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    val mockCredentialResponse = mockk<GetCredentialResponse>()
    val mockCustomCredential = mockk<CustomCredential>()
    every { mockCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns "some.other.credential.type"
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } returns
        mockCredentialResponse

    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
    coVerify(exactly = 0) { mockFirebaseAuth.signInWithCredential(any()) }
  }

  @Test
  fun signOut_clears_user_and_sets_status() = runTest {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()
    verify { mockFirebaseAuth.signOut() }
    assertNull(viewModel.currentUser.value)
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignIn_when_already_signed_in_starts_new_flow() = runTest {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.beginSignIn(mockActivity)
    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
  }

  @Test
  fun signOut_when_already_signed_out_is_safe() = runTest {
    every { mockFirebaseAuth.currentUser } returns null
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(exactly = 1) { mockFirebaseAuth.signOut() }
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun handleSignIn_with_non_custom_credential_does_nothing() = runTest {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    val mockCredentialResponse = mockk<GetCredentialResponse>()
    val mockPasswordCredential = mockk<PasswordCredential>()
    every { mockCredentialResponse.credential } returns mockPasswordCredential
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } returns
        mockCredentialResponse

    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
    coVerify(exactly = 0) { mockFirebaseAuth.signInWithCredential(any()) }
  }
}
