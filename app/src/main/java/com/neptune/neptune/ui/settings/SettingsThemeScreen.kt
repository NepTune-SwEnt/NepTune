package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme

/**
 * Displays the settings screen.
 *
 * This screen provides a top bar with a back navigation button and hosts various settings sections,
 * such as theme selection.
 *
 * @param settingsViewModel The [SettingsViewModel] instance used to observe and update settings
 *   state, such as the current theme. Defaults to the ViewModel provided by `viewModel()`.
 * @param goBack A lambda function to be invoked when the user clicks the back button, typically
 *   used for navigation.
 * @param goCustomTheme A lambda invoked when the user wants to open the custom theme editor.
 */
@Composable
fun SettingsThemeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    goBack: () -> Unit = {},
    goCustomTheme: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        Column {
          Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 16.dp),
              horizontalArrangement = Arrangement.Start,
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = goBack) {
                  Icon(
                      imageVector = Icons.Default.ArrowBackIosNew,
                      contentDescription = "Go Back",
                      tint = NepTuneTheme.colors.onBackground)
                }
              }
        }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        ) {
          item { ThemeSettingsSection(settingsViewModel, goCustomTheme) }
        }
      }
}

@Composable
private fun ThemeSettingsSection(settingsViewModel: SettingsViewModel, goCustomTheme: () -> Unit) {
  val selectedTheme by settingsViewModel.theme.collectAsState()

  Column {
    Text(
        text = "Theme",
        style =
            TextStyle(
                fontSize = 37.sp,
                fontFamily = FontFamily(Font(R.font.markazi_text)),
                fontWeight = FontWeight(400),
                color = NepTuneTheme.colors.onBackground,
            ),
        modifier = Modifier.padding(bottom = 8.dp))

    Column(Modifier.selectableGroup()) {
      ThemeSetting.entries.forEach { theme ->
        Row(
            Modifier.fillMaxWidth()
                .selectable(
                    selected = (selectedTheme == theme),
                    onClick = {
                      settingsViewModel.updateTheme(theme)
                      if (theme == ThemeSetting.CUSTOM) {
                        goCustomTheme()
                      }
                    },
                    role = Role.RadioButton)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                  selected = (selectedTheme == theme),
                  onClick = null // Null for accessibility, as Row handles the click
                  )
              Text(
                  text =
                      {
                        when (theme) {
                          ThemeSetting.SYSTEM -> "System Default"
                          ThemeSetting.LIGHT -> "Light"
                          ThemeSetting.DARK -> "Dark"
                          ThemeSetting.CUSTOM -> "Custom"
                        }
                      }(),
                  style =
                      TextStyle(
                          fontSize = 24.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(400),
                          color = NepTuneTheme.colors.onBackground,
                      ),
                  color = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.padding(start = 16.dp))
            }
      }
    }
  }
}
