package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.util.NeptuneTopBar

/**
 * Displays the settings screen.
 *
 * This screen provides a top bar with a back navigation button and hosts various settings sections,
 * such as theme selection.
 *
 * @param goBack A lambda function to be invoked when the user clicks the back button, typically
 *   used for navigation.
 * @param goTheme A lambda function to be invoked when the user selects the "Theme" settings item.
 * @param goAccount A lambda function to be invoked when the user selects the "Account
 */
@Composable
fun SettingsScreen(
    goBack: () -> Unit = {},
    goTheme: () -> Unit = {},
    goAccount: () -> Unit = {},
) {
  Scaffold(
      topBar = { NeptuneTopBar(title = "Settings", goBack = goBack) },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          item {
            SettingItemCard(
                text = "Theme",
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "go to theme settings",
                onClick = goTheme)
          }
          item {
            SettingItemCard(
                text = "Account",
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "go to account settings",
                onClick = goAccount)
          }
        }
      }
}
