package com.neptune.neptune.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Represents the available theme options for the user. */
enum class ThemeSetting {
  SYSTEM,
  LIGHT,
  DARK
}

/** ViewModel for the Settings screen. */
class SettingsViewModel : ViewModel() {

  /** The currently selected theme setting. */
  var selectedTheme by mutableStateOf(ThemeSetting.SYSTEM)
}
