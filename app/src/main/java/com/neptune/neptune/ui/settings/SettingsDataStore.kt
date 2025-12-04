package com.neptune.neptune.ui.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Manages saving and retrieving the user's selected theme preference using Jetpack DataStore. */
class ThemeDataStore(private val context: Context) {

  private val THEME_KEY = stringPreferencesKey("theme_setting")
  private val CUSTOM_PRIMARY_KEY = stringPreferencesKey("custom_primary_hex")
  private val CUSTOM_BACKGROUND_KEY = stringPreferencesKey("custom_background_hex")
  private val CUSTOM_ONBACKGROUND_KEY = stringPreferencesKey("custom_onbackground_hex")
  // New key to control whether the help button in the sampler should be disabled
  private val DISABLE_HELP_KEY = booleanPreferencesKey("disable_help_button")

  companion object {
    private const val RGB_MASK = 0xFFFFFF
    private const val HEX_FORMAT = "#%06X"
    // sensible defaults (match existing app colors)
    const val DEFAULT_PRIMARY_HEX = "#6650A4" // Purple40
    const val DEFAULT_BACKGROUND_HEX = "#F1F3FF" // GhostWhite
    const val DEFAULT_ONBACKGROUND_HEX = "#1C1F35" // DarkBlue3 (good readable)
    val DEFAULT_PRIMARY_COLOR = Color(DEFAULT_PRIMARY_HEX.toColorInt())
    val DEFAULT_BACKGROUND_COLOR = Color(DEFAULT_BACKGROUND_HEX.toColorInt())
    val DEFAULT_ONBACKGROUND_COLOR = Color(DEFAULT_ONBACKGROUND_HEX.toColorInt())
  }

  /** A [Flow] that emits the currently saved [ThemeSetting]. */
  val theme: Flow<ThemeSetting> =
      context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: ThemeSetting.DARK.name
        ThemeSetting.valueOf(themeName)
      }

  /** Flow emitting the saved custom primary Color. */
  val customPrimaryColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_PRIMARY_KEY] ?: DEFAULT_PRIMARY_HEX
        try {
          Color(hex.toColorInt())
        } catch (_: Exception) {
          DEFAULT_PRIMARY_COLOR
        }
      }

  /** Flow emitting the saved custom background Color. */
  val customBackgroundColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_BACKGROUND_KEY] ?: DEFAULT_BACKGROUND_HEX
        try {
          Color(hex.toColorInt())
        } catch (_: Exception) {
          DEFAULT_BACKGROUND_COLOR
        }
      }

  /** Flow emitting the saved custom onBackground Color. */
  val customOnBackgroundColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_ONBACKGROUND_KEY] ?: DEFAULT_ONBACKGROUND_HEX
        try {
          Color(hex.toColorInt())
        } catch (_: Exception) {
          DEFAULT_ONBACKGROUND_COLOR
        }
      }

  /** Flow emitting whether the sampler help button should be disabled. Defaults to false. */
  val disableHelp: Flow<Boolean> =
      context.dataStore.data.map { prefs -> prefs[DISABLE_HELP_KEY] ?: false }

  /**
   * Suspended function to persist the user's selected [ThemeSetting] to DataStore.
   *
   * @param theme The [ThemeSetting] to save (e.g., LIGHT, DARK, or SYSTEM). This enum's name will
   *   be stored as a string.
   */
  suspend fun setTheme(theme: ThemeSetting) {
    context.dataStore.edit { settings -> settings[THEME_KEY] = theme.name }
  }

  /** Persist whether the sampler help button should be disabled. */
  suspend fun setDisableHelp(disabled: Boolean) {
    context.dataStore.edit { settings -> settings[DISABLE_HELP_KEY] = disabled }
  }

  /** Save custom colors as hex strings (e.g. #RRGGBB). */
  suspend fun setCustomColors(primary: Color, background: Color, onBackground: Color) {
    val primaryHex = String.format(HEX_FORMAT, RGB_MASK and primary.toArgb())
    val backgroundHex = String.format(HEX_FORMAT, RGB_MASK and background.toArgb())
    val onBackgroundHex = String.format(HEX_FORMAT, RGB_MASK and onBackground.toArgb())
    context.dataStore.edit { settings ->
      settings[CUSTOM_PRIMARY_KEY] = primaryHex
      settings[CUSTOM_BACKGROUND_KEY] = backgroundHex
      settings[CUSTOM_ONBACKGROUND_KEY] = onBackgroundHex
    }
  }

  /** Reset all custom colors to their default values. */
  suspend fun resetCustomColors() {
    context.dataStore.edit { settings ->
      settings.remove(CUSTOM_PRIMARY_KEY)
      settings.remove(CUSTOM_BACKGROUND_KEY)
      settings.remove(CUSTOM_ONBACKGROUND_KEY)
    }
  }
}
