package com.neptune.neptune.ui.authentification

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme

object SignInScreenTags {
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
  // New tags for email/password auth
  const val EMAIL_FIELD = "emailField"
  const val PASSWORD_FIELD = "passwordField"
  const val CONFIRM_PASSWORD_FIELD = "confirmPasswordField"
  const val EMAIL_ERROR = "emailError"
  const val PASSWORD_ERROR = "passwordError"
  const val CONFIRM_PASSWORD_ERROR = "confirmPasswordError"
  const val GENERAL_ERROR = "generalAuthError"
  const val TOGGLE_REGISTER = "toggleRegisterButton"
  const val SUBMIT_EMAIL = "submitEmailButton"
  const val ANONYMOUS_BUTTON = "anonymousSignInButton"
  const val MODE_SWITCH = "modeSwitchButton"

  // Top Bar
  const val TOP_BAR = "topBar"
  const val TOP_BAR_TITLE = "topBarTitle"
}

/**
 * A composable function that displays the sign-in screen for the application.
 *
 * This screen presents a welcome message and a "Sign in with Google" button. It delegates the
 * sign-in logic to the [SignInViewModel] and handles the UI state based on the authentication flow.
 *
 * @param context The Android [Context], used to get the current [Activity] for the sign-in process.
 *   Defaults to `LocalContext.current`.
 * @param credentialManager An instance of [CredentialManager] used to initiate the sign-in request.
 *   Defaults to a new instance created with the current context.
 * @param navigateMain A lambda function to be called to navigate to the main part of the
 *   application after a successful sign-in.
 * @param signInViewModel The [SignInViewModel] instance that manages the authentication logic and
 *   state. Defaults to a new ViewModel instance provided by `viewModel()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navigateMain: () -> Unit = {},
    signInViewModel: SignInViewModel = viewModel(),
) {

  val signInStatus by signInViewModel.signInStatus.collectAsState()
  val emailState by signInViewModel.emailAuthUiState.collectAsState()

  LaunchedEffect(credentialManager) {
    signInViewModel.initialize(
        credentialManager, navigateMain, context.getString(R.string.oauth_client_id))
  }
  val googleId = R.drawable.google_logo

  // local UI mode state: whether we show Google button or email form. Simple derived logic - show
  // both.
  Scaffold(
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.fillMaxWidth().height(112.dp).testTag(SignInScreenTags.TOP_BAR),
              title = {
                Text(
                    text = "NepTune",
                    style =
                        TextStyle(
                            fontSize = 45.sp,
                            fontFamily = FontFamily(Font(R.font.lily_script_one)),
                            fontWeight = FontWeight(149),
                            color = NepTuneTheme.colors.onBackground,
                        ),
                    modifier = Modifier.padding(25.dp).testTag(SignInScreenTags.TOP_BAR_TITLE),
                    textAlign = TextAlign.Center)
              },
              colors =
                  TopAppBarDefaults.centerAlignedTopAppBarColors(
                      containerColor = NepTuneTheme.colors.background))
          HorizontalDivider(
              modifier = Modifier.fillMaxWidth(),
              thickness = 0.75.dp,
              color = NepTuneTheme.colors.onBackground)
        }
      },
      containerColor = NepTuneTheme.colors.background,
  ) { innerPadding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Welcome",
              textAlign = TextAlign.Center,
              style =
                  TextStyle(
                      fontSize = 80.sp,
                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                      fontWeight = FontWeight(400)),
              color = NepTuneTheme.colors.onBackground,
              modifier = Modifier.testTag(SignInScreenTags.LOGIN_TITLE))

          Spacer(modifier = Modifier.height(60.dp))

          // Google Sign-In Button ------------------------------------------------------------
          ElevatedButton(
              onClick = { signInViewModel.beginSignIn(context as Activity) },
              enabled =
                  signInStatus != SignInStatus.SIGN_IN_REQUESTED &&
                      signInStatus != SignInStatus.IN_PROGRESS_FIREBASE_AUTH &&
                      emailState.loading.not(),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = NepTuneTheme.colors.onBackground,
                      contentColor = NepTuneTheme.colors.loginText),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag(SignInScreenTags.LOGIN_BUTTON)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Image(
                      painter = painterResource(id = googleId),
                      contentDescription = "Google Logo",
                      modifier = Modifier.size(30.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                      text = "Sign in with Google",
                      style =
                          TextStyle(
                              fontSize = 24.sp,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(400)),
                  )
                }
              }

          Spacer(modifier = Modifier.height(32.dp))

          // Email / Password Form -----------------------------------------------------------
          Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = emailState.email,
                onValueChange = { signInViewModel.setEmail(it) },
                label = { Text("Email") },
                isError = emailState.emailError != null,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag(SignInScreenTags.EMAIL_FIELD))
            if (emailState.emailError != null) {
              Text(
                  text = emailState.emailError ?: "",
                  color = Color.Red,
                  fontSize = 12.sp,
                  modifier = Modifier.testTag(SignInScreenTags.EMAIL_ERROR))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = emailState.password,
                onValueChange = { signInViewModel.setPassword(it) },
                label = { Text("Password") },
                isError = emailState.passwordError != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag(SignInScreenTags.PASSWORD_FIELD))
            if (emailState.passwordError != null) {
              Text(
                  text = emailState.passwordError ?: "",
                  color = Color.Red,
                  fontSize = 12.sp,
                  modifier = Modifier.testTag(SignInScreenTags.PASSWORD_ERROR))
            }

            if (emailState.registerMode) {
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedTextField(
                  value = emailState.confirmPassword,
                  onValueChange = { signInViewModel.setConfirmPassword(it) },
                  label = { Text("Confirm Password") },
                  isError = emailState.confirmPasswordError != null,
                  visualTransformation = PasswordVisualTransformation(),
                  keyboardOptions =
                      KeyboardOptions(
                          keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                  modifier =
                      Modifier.fillMaxWidth().testTag(SignInScreenTags.CONFIRM_PASSWORD_FIELD))
              if (emailState.confirmPasswordError != null) {
                Text(
                    text = emailState.confirmPasswordError ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag(SignInScreenTags.CONFIRM_PASSWORD_ERROR))
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                  ElevatedButton(
                      onClick = { signInViewModel.toggleRegisterMode() },
                      enabled = !emailState.loading,
                      modifier = Modifier.testTag(SignInScreenTags.TOGGLE_REGISTER)) {
                        Text(
                            if (emailState.registerMode) "Switch to Login"
                            else "Switch to Register")
                      }

                  ElevatedButton(
                      onClick = { signInViewModel.submitEmailAuth() },
                      enabled = emailState.canSubmit && !emailState.loading,
                      modifier = Modifier.testTag(SignInScreenTags.SUBMIT_EMAIL)) {
                        Text(if (emailState.registerMode) "Create Account" else "Sign In")
                      }
                }

            Spacer(modifier = Modifier.height(12.dp))

            ElevatedButton(
                onClick = { signInViewModel.signInAnonymously() },
                enabled = !emailState.loading,
                modifier = Modifier.fillMaxWidth().testTag(SignInScreenTags.ANONYMOUS_BUTTON)) {
                  Text("Continue as Guest")
                }

            if (emailState.generalError != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                  text = emailState.generalError ?: "",
                  color = Color.Red,
                  modifier = Modifier.testTag(SignInScreenTags.GENERAL_ERROR))
            }
          }
        }
  }
}
