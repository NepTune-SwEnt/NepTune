package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.ui.theme.NepTuneTheme

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    goBack: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        Column {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
              horizontalArrangement = Arrangement.Start,
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = goBack,
                ) {
                  Icon(
                      painter = painterResource(id = android.R.drawable.ic_menu_revert),
                      contentDescription = "Go Back",
                      tint = NepTuneTheme.colors.onBackground)
                }
              }
          HorizontalDivider(color = NepTuneTheme.colors.onBackground, thickness = 0.5.dp)
        }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
              ThemeSettingsSection(settingsViewModel)
            }
      }
}

@Composable
private fun ThemeSettingsSection(settingsViewModel: SettingsViewModel) {

  Column(modifier = Modifier.padding(vertical = 16.dp)) {
    Text(
        text = "Theme",
        style = MaterialTheme.typography.titleLarge,
        color = NepTuneTheme.colors.onBackground,
        modifier = Modifier.padding(bottom = 8.dp))

    Column(Modifier.selectableGroup()) {
      ThemeSetting.entries.forEach { theme ->
        Row(
            Modifier.fillMaxWidth()
                .selectable(
                    selected = (settingsViewModel.selectedTheme == theme),
                    onClick = { settingsViewModel.selectedTheme = theme },
                    role = Role.RadioButton)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                  selected = (settingsViewModel.selectedTheme == theme),
                  onClick = null // Null for accessibility, as Row handles the click
                  )
              Text(
                  text =
                      when (theme) {
                        ThemeSetting.SYSTEM -> "System Default"
                        ThemeSetting.LIGHT -> "Light"
                        ThemeSetting.DARK -> "Dark"
                      },
                  style = MaterialTheme.typography.bodyLarge,
                  color = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.padding(start = 16.dp))
            }
      }
    }
  }
}
