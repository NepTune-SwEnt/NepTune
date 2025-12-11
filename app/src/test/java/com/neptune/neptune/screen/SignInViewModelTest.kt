package com.neptune.neptune.screen

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.neptune.neptune.ui.authentification.GoogleIdOptionFactory
import com.neptune.neptune.ui.authentification.SignInStatus
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.util.NetworkConnectivityObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
 * without relying on the Android UI. The tests were done using AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockActivity: Activity
  private lateinit var mockGoogleIdOptionFactory: GoogleIdOptionFactory

  private lateinit var viewModel: SignInViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val fakeOauthClientId = "fake-oauth-client-id-for-testing.apps.googleusercontent.com"
  private lateinit var mockConnectivityObserver: NetworkConnectivityObserver

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockFirebaseAuth = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockActivity = mockk(relaxed = true)

    mockConnectivityObserver = mockk(relaxed = true)
    every { mockConnectivityObserver.isOnline } returns flowOf(true)

    mockGoogleIdOptionFactory = mockk()
    val mockGoogleIdOption: GetGoogleIdOption = mockk()
    every { mockGoogleIdOptionFactory.create(any()) } returns mockGoogleIdOption

    mockkStatic(FirebaseDatabase::class)
    val mockFirebaseDatabase: FirebaseDatabase = mockk(relaxed = true)
    every { FirebaseDatabase.getInstance() } returns mockFirebaseDatabase

    mockkConstructor(NetworkConnectivityObserver::class)
    every { anyConstructed<NetworkConnectivityObserver>().isOnline } returns flowOf(true)

    viewModel =
        SignInViewModel(mockFirebaseAuth, mockGoogleIdOptionFactory, mockConnectivityObserver)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    io.mockk.unmockkAll()
  }

  @Test
  fun initialStatusIsBeforeInitialization() {
    assertEquals(SignInStatus.BEFORE_INITIALIZATION, viewModel.signInStatus.value)
  }

  @Test
  fun initializeWhenUserIsSignedInNavigates() {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUid"
    every { mockConnectivityObserver.isOnline } returns flowOf(true)
    var hasNavigated = false
    viewModel.initialize(mockCredentialManager, { hasNavigated = true }, fakeOauthClientId)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(mockUser, viewModel.currentUser.value)
    assertTrue("Navigation should have occurred", hasNavigated)
  }

  @Test
  fun initializeWhenNoUserIsSignedInDoesNotNavigate() {
    every { mockFirebaseAuth.currentUser } returns null
    var hasNavigated = false
    viewModel.initialize(mockCredentialManager, { hasNavigated = true }, fakeOauthClientId)
    assertNull(viewModel.currentUser.value)
    assertFalse("Navigation should not have occurred", hasNavigated)
  }

  @Test
  fun beginSignInChangesStatusToRequested() {
    viewModel.initialize(mockk(), {}, fakeOauthClientId)
    viewModel.beginSignIn(mockActivity)
    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignInWhenUserCancelsChangesStatusToSignedOut() {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        GetCredentialCancellationException("User cancelled")
    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignInWhenGenericErrorChangesStatusToError() {
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    coEvery { mockCredentialManager.getCredential(context = any(), request = any()) } throws
        RuntimeException("Network error")
    viewModel.beginSignIn(mockActivity)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(SignInStatus.ERROR, viewModel.signInStatus.value)
  }

  @Test
  fun handleSignInWithNonGoogleCredentialDoesNothing() {
    every { mockFirebaseAuth.currentUser } returns null
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    testDispatcher.scheduler.advanceUntilIdle()
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
  fun signOutClearsUserAndSetsStatus() {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUid"
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()
    verify { mockFirebaseAuth.signOut() }
    assertNull(viewModel.currentUser.value)
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun beginSignInWhenAlreadySignedInStartsNewFlow() {
    val mockUser: FirebaseUser = mockk()
    every { mockFirebaseAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "testUid"
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.beginSignIn(mockActivity)
    assertEquals(SignInStatus.SIGN_IN_REQUESTED, viewModel.signInStatus.value)
  }

  @Test
  fun signOutWhenAlreadySignedOutIsSafe() {
    every { mockFirebaseAuth.currentUser } returns null
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()
    verify(exactly = 1) { mockFirebaseAuth.signOut() }
    assertEquals(SignInStatus.SIGNED_OUT, viewModel.signInStatus.value)
  }

  @Test
  fun handleSignInWithNonCustomCredentialDoesNothing() {
    every { mockFirebaseAuth.currentUser } returns null
    viewModel.initialize(mockCredentialManager, {}, fakeOauthClientId)
    testDispatcher.scheduler.advanceUntilIdle()
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
