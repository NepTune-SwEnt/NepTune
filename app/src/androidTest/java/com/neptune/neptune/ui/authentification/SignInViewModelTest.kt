package com.neptune.neptune.ui.authentification

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
  @get:Rule val composeRule = createComposeRule()

  private fun buildViewModel(firebaseAuth: FirebaseAuth): SignInViewModel {
    return SignInViewModel(
        firebaseAuth = firebaseAuth, googleIdOptionFactory = mockk(relaxed = true))
  }

  private fun initialize(viewModel: SignInViewModel) {
    val credentialManager = mockk<CredentialManager>(relaxed = true)
    viewModel.initialize(credentialManager, navigate = {}, serverClientId = "client")
  }

  @Test
  fun validateEmailPasswordInvalidEmailSetsError() = runTest {
    val auth = mockk<FirebaseAuth>(relaxed = true)
    every { auth.currentUser } returns null
    val vm = buildViewModel(auth)
    initialize(vm)

    vm.setEmail("invalid")
    vm.setPassword("123456")
    vm.submitEmailAuth() // sign in mode

    composeRule.waitForIdle()
    val state = vm.emailAuthUiState.first()
    Assert.assertEquals("Invalid email", state.emailError)
    Assert.assertEquals(SignInStatus.SIGNED_OUT, vm.signInStatus.first())
  }

  @Test
  fun validateEmailPasswordShortPasswordSetsError() = runTest {
    val auth = mockk<FirebaseAuth>(relaxed = true)
    every { auth.currentUser } returns null
    val vm = buildViewModel(auth)
    initialize(vm)

    vm.setEmail("user@example.com")
    vm.setPassword("123")
    vm.submitEmailAuth()

    composeRule.waitForIdle()
    val state = vm.emailAuthUiState.first()
    Assert.assertEquals("Min 6 characters", state.passwordError)
  }

  @Test
  fun validateEmailPasswordRegisterModeMismatchSetsError() = runTest {
    val auth = mockk<FirebaseAuth>(relaxed = true)
    every { auth.currentUser } returns null
    val vm = buildViewModel(auth)
    initialize(vm)

    vm.toggleRegisterMode()
    vm.setEmail("user@example.com")
    vm.setPassword("123456")
    vm.setConfirmPassword("different")
    vm.submitEmailAuth()

    composeRule.waitForIdle()
    val state = vm.emailAuthUiState.first()
    Assert.assertEquals("Passwords don't match", state.confirmPasswordError)
  }

  @Test
  fun signInWithEmailSuccessUpdatesState() = runTest {
    val firebaseUser = mockk<FirebaseUser>(relaxed = true)
    val authResult = mockk<AuthResult>(relaxed = true)
    every { authResult.user } returns firebaseUser
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
        }
    val vm = buildViewModel(auth)
    initialize(vm)
    vm.setEmail("user@example.com")
    vm.setPassword("123456")

    vm.submitEmailAuth()

    composeRule.waitForIdle()
    Assert.assertEquals(SignInStatus.SUCCESS, vm.signInStatus.first())
    Assert.assertTrue(vm.emailAuthUiState.first().email.isEmpty())
    Assert.assertNotNull(vm.currentUser.first())
  }

  @Test
  fun registerWithEmailSuccessUpdatesState() = runTest {
    val firebaseUser = mockk<FirebaseUser>(relaxed = true)
    val authResult = mockk<AuthResult>(relaxed = true)
    every { authResult.user } returns firebaseUser
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(authResult)
        }
    val vm = buildViewModel(auth)
    initialize(vm)
    vm.toggleRegisterMode()
    vm.setEmail("user@example.com")
    vm.setPassword("123456")
    vm.setConfirmPassword("123456")

    vm.submitEmailAuth()

    composeRule.waitForIdle()
    Assert.assertEquals(SignInStatus.SUCCESS, vm.signInStatus.first())
    Assert.assertTrue(vm.emailAuthUiState.first().email.isEmpty())
    Assert.assertNotNull(vm.currentUser.first())
  }

  @Test
  fun signInWithEmailInvalidCredentialsMapsError() = runTest {
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { signInWithEmailAndPassword(any(), any()) } returns
              Tasks.forException(FirebaseAuthInvalidCredentialsException("code", "bad"))
        }
    val vm = buildViewModel(auth)
    initialize(vm)
    vm.setEmail("user@example.com")
    vm.setPassword("123456")

    vm.submitEmailAuth()

    composeRule.waitForIdle()
    Assert.assertEquals(SignInStatus.ERROR, vm.signInStatus.first())
    Assert.assertEquals("Invalid credentials", vm.emailAuthUiState.first().generalError)
  }

  @Test
  fun registerEmailAlreadyInUseMapsError() = runTest {
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { createUserWithEmailAndPassword(any(), any()) } returns
              Tasks.forException(FirebaseAuthUserCollisionException("code", "exists"))
        }
    val vm = buildViewModel(auth)
    initialize(vm)
    vm.toggleRegisterMode()
    vm.setEmail("user@example.com")
    vm.setPassword("123456")
    vm.setConfirmPassword("123456")

    vm.submitEmailAuth()

    composeRule.waitForIdle()
    Assert.assertEquals("Email already in use", vm.emailAuthUiState.first().generalError)
  }

  @Test
  fun signInAnonymouslySuccessSetsSuccess() = runTest {
    val firebaseUser = mockk<FirebaseUser>(relaxed = true)
    val authResult = mockk<AuthResult>(relaxed = true)
    every { authResult.user } returns firebaseUser
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { signInAnonymously() } returns Tasks.forResult(authResult)
        }
    val vm = buildViewModel(auth)
    initialize(vm)

    vm.signInAnonymously()

    composeRule.waitForIdle()
    Assert.assertEquals(SignInStatus.SUCCESS, vm.signInStatus.first())
    Assert.assertNotNull(vm.currentUser.first())
  }

  @Test
  fun signInAnonymouslyNetworkErrorMapsError() = runTest {
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns null
          every { signInAnonymously() } returns Tasks.forException(FirebaseNetworkException("net"))
        }
    val vm = buildViewModel(auth)
    initialize(vm)

    vm.signInAnonymously()

    composeRule.waitForIdle()
    Assert.assertEquals(SignInStatus.ERROR, vm.signInStatus.first())
    Assert.assertEquals("Network error", vm.emailAuthUiState.first().generalError)
  }

  @Test
  fun toggleRegisterModeClearsErrors() = runTest {
    val auth = mockk<FirebaseAuth>(relaxed = true)
    every { auth.currentUser } returns null
    val vm = buildViewModel(auth)
    initialize(vm)
    vm.setEmail("invalid")
    vm.setPassword("123")
    vm.submitEmailAuth()
    composeRule.waitForIdle()
    Assert.assertEquals("Invalid email", vm.emailAuthUiState.first().emailError)
    vm.toggleRegisterMode()
    val state = vm.emailAuthUiState.first()
    Assert.assertEquals(null, state.emailError)
    Assert.assertEquals(state.registerMode, true)
  }
}
