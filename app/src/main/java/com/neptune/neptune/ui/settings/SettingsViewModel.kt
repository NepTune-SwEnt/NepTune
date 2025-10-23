package com.neptune.neptune.ui.settings

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
  DARK
}

/** ViewModel for the Settings screen. */
class SettingsViewModel(private val themeDataStore: ThemeDataStore) : ViewModel() {

  val theme: StateFlow<ThemeSetting> =
      themeDataStore.theme.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = ThemeSetting.SYSTEM)

  fun updateTheme(newTheme: ThemeSetting) {
    viewModelScope.launch { themeDataStore.setTheme(newTheme) }
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
