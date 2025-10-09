package com.neptune.neptune.ui.authentification

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val OAUTH_CLIENT_ID =
    "854532932499-43svmeqbqdc7pfp2nbu7eqq2o3ahjtg9.apps.googleusercontent.com"

private enum class SignInStatus {
  BEFORE_INITIALIZATION,
  SIGNED_OUT,
  SIGN_IN_REQUESTED,
  IN_PROGRESS_FIREBASE_AUTH,
  SUCCESS,
  ERROR
}

/**
 * ViewModel for the SignInScreen.
 *
 * This ViewModel orchestrates the user authentication flow using Google Sign-In with the Android
 * Credential Manager. It handles the logic for initiating the sign-in process, authenticating the
 * user with Firebase, and managing the user's session state.
 */
class SignInViewModel : ViewModel() {

  private lateinit var credentialManager: CredentialManager
  private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _signInStatus = MutableStateFlow<SignInStatus>(SignInStatus.BEFORE_INITIALIZATION)
  private val signInStatus: StateFlow<SignInStatus> = _signInStatus.asStateFlow()

  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
  var navigateMain: () -> Unit = {}

  /**
   * Should be called from the View to initialize the CredentialManager and the initial state of the
   * ViewModel.
   */
  fun initialize(credential: CredentialManager, navigate: () -> Unit) {
    credentialManager = credential
    navigateMain = navigate

    val initialUser = firebaseAuth.currentUser
    _currentUser.value = initialUser
    if (initialUser != null) {
      _signInStatus.value = SignInStatus.SUCCESS
      navigateMain()
    } else {
      _signInStatus.value = SignInStatus.SIGNED_OUT
    }
  }

  /**
   * Initiates the Google Sign-In flow using the Credential Manager.
   *
   * This function constructs a [GetCredentialRequest] configured for Google Sign-In. It then
   * launches a coroutine to call [CredentialManager.getCredential], which displays the system's
   * account selection UI. On success, the resulting credential is used to authenticate with
   * Firebase. If authentication is successful, it triggers the navigation to the overview screen.
   *
   * @param activity The activity context required to launch the sign-in UI.
   */
  fun beginSignIn(activity: Activity) {
    _signInStatus.value = SignInStatus.SIGN_IN_REQUESTED

    val googleIdOption: GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(OAUTH_CLIENT_ID)
            .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    viewModelScope.launch {
      try {
        val result: GetCredentialResponse =
            credentialManager.getCredential(
                context = activity,
                request = request,
            )
        handleSignIn(result.credential)
      } catch (_: GetCredentialException) {
        _signInStatus.value = SignInStatus.ERROR
      } catch (_: Exception) {
        _signInStatus.value = SignInStatus.ERROR
      }
    }
  }

  private fun handleSignIn(credential: Credential) {
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
    }
  }

  private fun firebaseAuthWithGoogle(idToken: String) {
    _signInStatus.value = SignInStatus.IN_PROGRESS_FIREBASE_AUTH
    val credential = GoogleAuthProvider.getCredential(idToken, null)

    viewModelScope.launch {
      try {
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        _currentUser.value = authResult.user
        _signInStatus.value = SignInStatus.SUCCESS
        navigateMain()
      } catch (_: Exception) {
        _currentUser.value = null
        _signInStatus.value = SignInStatus.ERROR
      }
    }
  }

  /**
   * Signs the current user out of Firebase.
   *
   * This function clears the user's session from Firebase Authentication. It then nullifies the
   * [_currentUser] state and updates the [_signInStatus] to [SignInStatus.SIGNED_OUT], effectively
   * resetting the authentication state of the application.
   */
  fun signOut() {
    viewModelScope.launch {
      firebaseAuth.signOut()
      _currentUser.value = null
      _signInStatus.value = SignInStatus.SIGNED_OUT
    }
  }
}
