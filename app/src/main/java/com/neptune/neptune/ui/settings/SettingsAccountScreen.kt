package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme

/**
 * Displays the account settings screen.
 *
 * This screen provides options related to the user's account, such as logging out.
 *
 * @param goBack A lambda function to be invoked when the user clicks the back button.
 * @param logout A lambda function to log the user out.
 */
@Composable
fun SettingsAccountScreen(
    goBack: () -> Unit = {},
    logout: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = goBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Go Back",
                    tint = NepTuneTheme.colors.onBackground)
              }
            }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {
          item { AccountSettingsSection(logout) }
        }
      }
}

@Composable
private fun AccountSettingsSection(signOut: () -> Unit) {
  Column {
    Text(
        text = "Account",
        style =
            TextStyle(
                fontSize = 37.sp,
                fontFamily = FontFamily(Font(R.font.markazi_text)),
                fontWeight = FontWeight(400),
                color = NepTuneTheme.colors.onBackground,
            ),
        modifier = Modifier.padding(bottom = 8.dp))
    SettingItemCard(
        text = "Log Out",
        imageVector = Icons.AutoMirrored.Filled.Logout,
        contentDescription = "log out",
        onClick = signOut)
  }
}
