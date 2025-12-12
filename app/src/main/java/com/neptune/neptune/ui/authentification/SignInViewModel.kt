package com.neptune.neptune.ui.authentification

import android.app.Activity
import android.util.Log
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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.neptune.neptune.util.NetworkConnectivityObserver
import com.neptune.neptune.util.RealtimeDatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

  /** Email/password authentication in progress (sign in or sign up). */
  IN_PROGRESS_EMAIL_AUTH,

  /** Anonymous (guest) authentication in progress. */
  IN_PROGRESS_ANONYMOUS_AUTH,

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

/** Email/password authentication UI state. */
data class EmailAuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val registerMode: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val loading: Boolean = false,
) {
  val canSubmit: Boolean =
      !loading &&
          email.isNotBlank() &&
          password.isNotBlank() &&
          (!registerMode || confirmPassword.isNotBlank())
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
    private val googleIdOptionFactory: GoogleIdOptionFactory = DefaultGoogleIdOptionFactory(),
    private val connectivityObserver: NetworkConnectivityObserver = NetworkConnectivityObserver()
) : ViewModel() {

  private lateinit var credentialManager: CredentialManager

  private val _signInStatus = MutableStateFlow(SignInStatus.BEFORE_INITIALIZATION)

  /** Exposes the currently authenticated [SignInStatus] as a read-only [StateFlow]. */
  val signInStatus: StateFlow<SignInStatus> = _signInStatus.asStateFlow()

  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)

  /** Exposes the currently authenticated [FirebaseUser] as a read-only [StateFlow]. */
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
  private var navigateMain: () -> Unit = {}
  private lateinit var clientId: String

  /** Email/password form UI state. */
  private val _emailAuthUiState = MutableStateFlow(EmailAuthUiState())
  val emailAuthUiState: StateFlow<EmailAuthUiState> = _emailAuthUiState.asStateFlow()
  private val _isOnline = MutableStateFlow(true)
  val isOnline = _isOnline.asStateFlow()

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

    viewModelScope.launch {
      val isNetworkAvailable =
          try {
            connectivityObserver.isOnline.first()
          } catch (_: Exception) {
            false
          }
      val initialUser = firebaseAuth.currentUser
      _currentUser.value = initialUser
      if (initialUser != null) {
        if (isNetworkAvailable) {
          // logged and network
          _signInStatus.value = SignInStatus.SUCCESS
          setupPresence(initialUser.uid)
          navigateMain()
        } else {
          // logged but no network
          signOut()
        }
      } else {
        // not logged
        _signInStatus.value = SignInStatus.SIGNED_OUT
      }
      viewModelScope.launch {
        connectivityObserver.isOnline.collect { status -> _isOnline.value = status }
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
        setupPresence(authResult.user!!.uid)
        _signInStatus.value = SignInStatus.SUCCESS
        navigateMain()
      } catch (_: Exception) {
        _currentUser.value = null
        _signInStatus.value = SignInStatus.ERROR
      }
    }
  }

  // region Email/Password Authentication -------------------------------------------------------
  fun setEmail(value: String) {
    _emailAuthUiState.value = _emailAuthUiState.value.copy(email = value.trim())
  }

  fun setPassword(value: String) {
    _emailAuthUiState.value = _emailAuthUiState.value.copy(password = value)
  }

  fun setConfirmPassword(value: String) {
    _emailAuthUiState.value = _emailAuthUiState.value.copy(confirmPassword = value)
  }

  fun signInOffline() {
    _signInStatus.value = SignInStatus.SUCCESS
    navigateMain()
  }

  fun toggleRegisterMode() {
    val current = _emailAuthUiState.value
    _emailAuthUiState.value =
        current.copy(
            registerMode = !current.registerMode,
            // clear errors & confirm when switching modes
            confirmPassword = if (!current.registerMode) "" else current.confirmPassword,
            passwordError = null,
            emailError = null,
            confirmPasswordError = null,
            generalError = null)
  }

  private fun validateEmailPassword(): Boolean {
    val state = _emailAuthUiState.value
    var emailError: String? = null
    var passwordError: String? = null
    var confirmError: String? = null

    if (state.email.isBlank()) emailError = "Email required"
    else if (!state.email.matches(EMAIL_REGEX)) emailError = "Invalid email"

    if (state.password.length < 6) passwordError = "Min 6 characters"

    if (state.registerMode) {
      if (state.confirmPassword.isBlank()) confirmError = "Confirm password"
      else if (state.password != state.confirmPassword) confirmError = "Passwords don't match"
    }

    _emailAuthUiState.value =
        state.copy(
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmError,
            generalError = null)
    return emailError == null && passwordError == null && confirmError == null
  }

  fun submitEmailAuth() {
    if (!validateEmailPassword()) return
    if (_emailAuthUiState.value.registerMode) registerWithEmailPassword()
    else signInWithEmailPassword()
  }

  private fun signInWithEmailPassword() {
    val state = _emailAuthUiState.value
    _signInStatus.value = SignInStatus.IN_PROGRESS_EMAIL_AUTH
    _emailAuthUiState.value = state.copy(loading = true, generalError = null)
    viewModelScope.launch {
      try {
        val result = firebaseAuth.signInWithEmailAndPassword(state.email, state.password).await()
        _currentUser.value = result.user
        setupPresence(result.user!!.uid)
        _signInStatus.value = SignInStatus.SUCCESS
        _emailAuthUiState.value = EmailAuthUiState() // reset form
        navigateMain()
      } catch (e: Exception) {
        handleEmailAuthException(e)
      }
    }
  }

  private fun registerWithEmailPassword() {
    val state = _emailAuthUiState.value
    _signInStatus.value = SignInStatus.IN_PROGRESS_EMAIL_AUTH
    _emailAuthUiState.value = state.copy(loading = true, generalError = null)
    viewModelScope.launch {
      try {
        val result =
            firebaseAuth.createUserWithEmailAndPassword(state.email, state.password).await()
        _currentUser.value = result.user
        setupPresence(result.user!!.uid)
        _signInStatus.value = SignInStatus.SUCCESS
        _emailAuthUiState.value = EmailAuthUiState()
        navigateMain()
      } catch (e: Exception) {
        handleEmailAuthException(e)
      }
    }
  }

  fun signInAnonymously() {
    _signInStatus.value = SignInStatus.IN_PROGRESS_ANONYMOUS_AUTH
    _emailAuthUiState.value = _emailAuthUiState.value.copy(loading = true, generalError = null)
    viewModelScope.launch {
      try {
        val result = firebaseAuth.signInAnonymously().await()
        _currentUser.value = result.user
        _signInStatus.value = SignInStatus.SUCCESS
        _emailAuthUiState.value = EmailAuthUiState()
        navigateMain()
      } catch (e: Exception) {
        handleEmailAuthException(e)
      }
    }
  }

  private fun handleEmailAuthException(e: Exception) {
    val msg =
        when (e) {
          is FirebaseAuthInvalidUserException -> "User not found"
          is FirebaseAuthInvalidCredentialsException -> "Invalid credentials"
          is FirebaseAuthUserCollisionException -> "Email already in use"
          is FirebaseNetworkException -> "Network error"
          else -> "Authentication error"
        }
    _signInStatus.value = SignInStatus.ERROR
    _emailAuthUiState.value = _emailAuthUiState.value.copy(loading = false, generalError = msg)
  }
  // endregion -------------------------------------------------------------------------------

  /**
   * Functions that changes the state to online when a user is on the app and to offline when a user
   * log out. This has been written with the help of LLMs.
   *
   * @author Angéline Bignens
   */
  private fun setupPresence(userId: String) {
    val db =
        try {
          RealtimeDatabaseProvider.getDatabase()
        } catch (_: IllegalStateException) {
          // Not initialize in unit tests
          return
        }
    val userStatusRef = db.getReference("/status/$userId")

    val onlineStatus = mapOf("state" to "online", "lastChanged" to ServerValue.TIMESTAMP)

    val offlineStatus = mapOf("state" to "offline", "lastChanged" to ServerValue.TIMESTAMP)

    // When connection changes
    val connectedRef = db.getReference(".info/connected")

    connectedRef.addValueEventListener(
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val connected = snapshot.getValue(Boolean::class.java) ?: false
            if (!connected) return

            // On reconnect, set online AND schedule offline on disconnect
            userStatusRef.onDisconnect().setValue(offlineStatus)
            userStatusRef.setValue(onlineStatus)
          }

          // Not needed in our app
          override fun onCancelled(error: DatabaseError) {

            Log.w("SignInViewModel", "Listener was cancelled: ${error.message}")
          }
        })
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
      firebaseAuth.currentUser?.uid?.let { uid ->
        val db = RealtimeDatabaseProvider.getDatabase()
        db.getReference("status/$uid")
            .setValue(mapOf("state" to "offline", "lastChanged" to ServerValue.TIMESTAMP))
      }

      firebaseAuth.signOut()
      _currentUser.value = null
      _signInStatus.value = SignInStatus.SIGNED_OUT
      _emailAuthUiState.value = EmailAuthUiState()
    }
  }

  companion object {
    private val EMAIL_REGEX =
        Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
  }
}
