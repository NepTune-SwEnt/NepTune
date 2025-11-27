package com.neptune.neptune.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Represents the available theme options for the user. */
enum class ThemeSetting {
  SYSTEM,
  LIGHT,
  DARK,
  CUSTOM
}

private const val STOP_TIMEOUT_MILLIS = 5000L

/** ViewModel for the Settings screen. */
class SettingsViewModel(private val themeDataStore: ThemeDataStore) : ViewModel() {

  val theme: StateFlow<ThemeSetting> =
      themeDataStore.theme.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = ThemeSetting.DARK)

  // Expose custom color saved values as StateFlow<Color>
  val customPrimaryColor: StateFlow<Color> =
      themeDataStore.customPrimaryColor.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = ThemeDataStore.DEFAULT_PRIMARY_COLOR)

  val customBackgroundColor: StateFlow<Color> =
      themeDataStore.customBackgroundColor.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = ThemeDataStore.DEFAULT_BACKGROUND_COLOR)

  val customOnBackgroundColor: StateFlow<Color> =
      themeDataStore.customOnBackgroundColor.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
          initialValue = ThemeDataStore.DEFAULT_ONBACKGROUND_COLOR)

  fun updateTheme(newTheme: ThemeSetting) {
    viewModelScope.launch { themeDataStore.setTheme(newTheme) }
  }

  fun updateCustomColors(primary: Color, background: Color, onBackground: Color) {
    viewModelScope.launch { themeDataStore.setCustomColors(primary, background, onBackground) }
  }

  fun resetCustomColors() {
    viewModelScope.launch { themeDataStore.resetCustomColors() }
  }
}

// This class was created using AI
class SettingsViewModelFactory(private val themeDataStore: ThemeDataStore) :
    ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return SettingsViewModel(themeDataStore) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
