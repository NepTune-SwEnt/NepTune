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

/**
 * Represents the various states of the user authentication process.
 *
 * This enum is used to track the user's journey from not being signed in to a successful
 * authentication, including intermediate steps and error conditions.
 */
enum class SignInStatus {

  // The initial state before the ViewModel has been initialized.
  BEFORE_INITIALIZATION,
  // The state when the user is known to be signed out, or after a sign-out action.
  SIGNED_OUT,

  /**
   * The state when the user has initiated the sign-in flow and the Credential Manager UI is
   * expected to be visible.
   */
  SIGN_IN_REQUESTED,

  /**
   * The state after a credential has been successfully retrieved from the Credential Manager and
   * the app is now authenticating with Firebase.
   */
  IN_PROGRESS_FIREBASE_AUTH,

  /**
   * The final state indicating that the user has been successfully authenticated with Firebase and
   * their session is active.
   */
  SUCCESS,

  /**
   * A terminal state indicating that an unrecoverable error occurred during the sign-in process,
   * such as a network issue or an invalid token during Firebase authentication.
   */
  ERROR
}

/**
 * Factory interface to abstract the creation of GetGoogleIdOption. This allows for easy mocking in
 * unit tests.
 */
interface GoogleIdOptionFactory {
  fun create(clientId: String): GetGoogleIdOption
}

/** The default, production implementation of the factory that uses the real builder. */
class DefaultGoogleIdOptionFactory : GoogleIdOptionFactory {
  override fun create(clientId: String): GetGoogleIdOption {
    return GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .build()
  }
}

/**
 * ViewModel for the SignInScreen.
 *
 * This ViewModel orchestrates the user authentication flow using Google Sign-In with the Android
 * Credential Manager. It handles the logic for initiating the sign-in process, authenticating the
 * user with Firebase, and managing the user's session state.
 *
 * This class was made using AI assistance
 */
class SignInViewModel(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val googleIdOptionFactory: GoogleIdOptionFactory = DefaultGoogleIdOptionFactory()
) : ViewModel() {

  private lateinit var credentialManager: CredentialManager

  private val _signInStatus = MutableStateFlow<SignInStatus>(SignInStatus.BEFORE_INITIALIZATION)

  /** Exposes the currently authenticated [SignInStatus] as a read-only [StateFlow]. */
  val signInStatus: StateFlow<SignInStatus> = _signInStatus.asStateFlow()

  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)

  /** Exposes the currently authenticated [FirebaseUser] as a read-only [StateFlow]. */
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
  private var navigateMain: () -> Unit = {}
  private lateinit var clientId: String

  /**
   * Initializes the ViewModel.
   *
   * This must be called from the View to provide necessary dependencies and set the initial state.
   * It sets up the [CredentialManager], checks if a user is already signed into Firebase, and
   * triggers immediate navigation if a session exists.
   *
   * @param credential The system-provided [CredentialManager], obtained via
   *   `CredentialManager.create(context)`.
   * @param navigate The lambda function to be executed for navigating to the main screen after a
   *   successful sign-in.
   */
  fun initialize(credential: CredentialManager, navigate: () -> Unit, serverClientId: String) {
    credentialManager = credential
    navigateMain = navigate
    clientId = serverClientId

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
   * Tente de connecter l'utilisateur anonymement.
   * Utile pour le débogage ou pour permettre un accès limité sans création de compte.
   */
  fun signInAnonymouslyForDebug() {
    // On signale que l'authentification est en cours
    _signInStatus.value = SignInStatus.IN_PROGRESS_FIREBASE_AUTH

    viewModelScope.launch {
      try {
        // Appel à la méthode native de Firebase pour l'anonymat
        val authResult = firebaseAuth.signInAnonymously().await()

        // Mise à jour de l'utilisateur et du statut
        _currentUser.value = authResult.user
        _signInStatus.value = SignInStatus.SUCCESS

        // Navigation vers l'écran principal
        navigateMain()
      } catch (e: Exception) {
        // Gestion d'erreur
        _currentUser.value = null
        _signInStatus.value = SignInStatus.ERROR
        e.printStackTrace() // Utile pour voir l'erreur dans le Logcat
      }
    }
  }

  /**
   * Initiates the Google Sign-In flow using the Credential Manager.
   *
   * This function constructs a [GetCredentialRequest] configured for Google Sign-In. It then
   * launches a coroutine to call [CredentialManager.getCredential], which displays the system's
   * account selection UI. On success, the resulting credential is used to authenticate with
   * Firebase. On user cancellation, the state is reset.
   *
   * @param activity The activity context required to launch the sign-in UI.
   */
  fun beginSignIn(activity: Activity) {
    _signInStatus.value = SignInStatus.SIGN_IN_REQUESTED

    val googleIdOption: GetGoogleIdOption = googleIdOptionFactory.create(clientId)

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    viewModelScope.launch {
      try {
        val result: GetCredentialResponse =
            credentialManager.getCredential(
                context = activity,
                request = request,
            )
        handleSignIn(result.credential)

        // This exception is most often thrown when the user cancels the sign-in flow.
        // This is not a fatal error, so we reset the state to allow the user to try again.
      } catch (_: GetCredentialException) {
        _signInStatus.value = SignInStatus.SIGNED_OUT
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
