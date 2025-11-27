package com.neptune.neptune.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages saving and retrieving the user's selected theme preference using Jetpack DataStore.
 *
 * This class was created using AI.
 *
 * @param context The application context, used to access the singleton `dataStore`. It's
 *   recommended to use the application context to avoid memory leaks.
 */
class ThemeDataStore(private val context: Context) {

  private val THEME_KEY = stringPreferencesKey("theme_setting")

  /**
   * A [Flow] that emits the currently saved [ThemeSetting].
   *
   * It reads the theme's string name from DataStore and maps it back to the [ThemeSetting] enum. If
   * no theme is set (e.g., on first launch), it defaults to [ThemeSetting.SYSTEM].
   */
  val theme: Flow<ThemeSetting> =
      context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: ThemeSetting.DARK.name
        ThemeSetting.valueOf(themeName)
      }

  /**
   * Suspended function to persist the user's selected [ThemeSetting] to DataStore.
   *
   * @param theme The [ThemeSetting] to save (e.g., LIGHT, DARK, or SYSTEM). This enum's name will
   *   be stored as a string.
   */
  suspend fun setTheme(theme: ThemeSetting) {
    context.dataStore.edit { settings -> settings[THEME_KEY] = theme.name }
  }
}
