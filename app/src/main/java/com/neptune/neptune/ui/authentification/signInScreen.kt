package com.neptune.neptune.ui.authentification

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.LightTurquoise

object SignInScreenTags {
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
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
@Composable
fun SignInScreen(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    navigateMain: () -> Unit = {},
    signInViewModel: SignInViewModel = viewModel(),
) {
  LaunchedEffect(credentialManager) { signInViewModel.initialize(credentialManager, navigateMain) }
  val googleId = com.neptune.neptune.R.drawable.google_logo

  Surface(modifier = Modifier.fillMaxSize(), color = DarkBlue1) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Welcome",
              textAlign = TextAlign.Center,
              style =
                  TextStyle(
                      fontSize = 80.sp,
                      fontFamily = FontFamily(Font(com.neptune.neptune.R.font.markazi_text)),
                      fontWeight = FontWeight(400)),
              color = LightTurquoise,
              modifier = Modifier.testTag(SignInScreenTags.LOGIN_TITLE))

          Spacer(modifier = Modifier.height(200.dp))

          ElevatedButton(
              onClick = { signInViewModel.beginSignIn(context as Activity) },
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = LightTurquoise,
                      contentColor = MaterialTheme.colorScheme.onBackground),
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
                              fontFamily =
                                  FontFamily(Font(com.neptune.neptune.R.font.markazi_text)),
                              fontWeight = FontWeight(400)),
                  )
                  Spacer(modifier = Modifier.width(24.dp))
                }
              }
        }
  }
}
