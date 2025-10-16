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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

  LaunchedEffect(credentialManager) {
    signInViewModel.initialize(
        credentialManager, navigateMain, context.getString(R.string.oauth_client_id))
  }
  val googleId = R.drawable.google_logo

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

          Spacer(modifier = Modifier.height(200.dp))

          ElevatedButton(
              onClick = { signInViewModel.beginSignIn(context as Activity) },
              enabled =
                  signInStatus != SignInStatus.SIGN_IN_REQUESTED &&
                      signInStatus != SignInStatus.IN_PROGRESS_FIREBASE_AUTH,
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = NepTuneTheme.colors.onBackground,
                      contentColor = NepTuneTheme.colors.loginText),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag(SignInScreenTags.LOGIN_BUTTON)) {
                Row {
                  Image(
                      painter = painterResource(id = googleId),
                      contentDescription = "Google Logo",
                      modifier = Modifier.size(30.dp).align(Alignment.CenterVertically))
                  Spacer(modifier = Modifier.width(24.dp))
                  Text(
                      text = "Sign in with Google",
                      style =
                          TextStyle(
                              fontSize = 30.sp,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(400)),
                  )
                  Spacer(modifier = Modifier.width(24.dp))
                }
              }
        }
  }
}
