package com.neptune.neptune.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages saving and retrieving the user's selected theme preference using Jetpack DataStore.
 */
class ThemeDataStore(private val context: Context) {

  private val THEME_KEY = stringPreferencesKey("theme_setting")
  private val CUSTOM_PRIMARY_KEY = stringPreferencesKey("custom_primary_hex")
  private val CUSTOM_BACKGROUND_KEY = stringPreferencesKey("custom_background_hex")
  private val CUSTOM_ONBACKGROUND_KEY = stringPreferencesKey("custom_onbackground_hex")
  private val CUSTOM_ONPRIMARY_KEY = stringPreferencesKey("custom_onprimary_hex")

  companion object {
    // sensible defaults (match existing app colors)
    const val DEFAULT_PRIMARY_HEX = "#6650A4" // Purple40
    const val DEFAULT_BACKGROUND_HEX = "#F1F3FF" // GhostWhite
    const val DEFAULT_ONBACKGROUND_HEX = "#1C1F35" // DarkBlue3 (good readable)
    const val DEFAULT_ONPRIMARY_HEX = "#FFFFFFFF" // white on primary

    val DEFAULT_PRIMARY_COLOR = Color(DEFAULT_PRIMARY_HEX.toColorInt())
    val DEFAULT_BACKGROUND_COLOR = Color(DEFAULT_BACKGROUND_HEX.toColorInt())
    val DEFAULT_ONBACKGROUND_COLOR = Color(DEFAULT_ONBACKGROUND_HEX.toColorInt())
    val DEFAULT_ONPRIMARY_COLOR = Color(DEFAULT_ONPRIMARY_HEX.toColorInt())
  }

  /**
   * A [Flow] that emits the currently saved [ThemeSetting].
   */
  val theme: Flow<ThemeSetting> =
      context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: ThemeSetting.SYSTEM.name
        ThemeSetting.valueOf(themeName)
      }

  /** Flow emitting the saved custom primary Color. */
  val customPrimaryColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_PRIMARY_KEY] ?: DEFAULT_PRIMARY_HEX
        try { Color(hex.toColorInt()) } catch (_: Exception) { DEFAULT_PRIMARY_COLOR }
      }

  /** Flow emitting the saved custom background Color. */
  val customBackgroundColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_BACKGROUND_KEY] ?: DEFAULT_BACKGROUND_HEX
        try { Color(hex.toColorInt()) } catch (_: Exception) { DEFAULT_BACKGROUND_COLOR }
      }

  /** Flow emitting the saved custom onBackground Color. */
  val customOnBackgroundColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_ONBACKGROUND_KEY] ?: DEFAULT_ONBACKGROUND_HEX
        try { Color(hex.toColorInt()) } catch (_: Exception) { DEFAULT_ONBACKGROUND_COLOR }
      }

  /** Flow emitting the saved custom onPrimary Color. */
  val customOnPrimaryColor: Flow<Color> =
      context.dataStore.data.map { prefs ->
        val hex = prefs[CUSTOM_ONPRIMARY_KEY] ?: DEFAULT_ONPRIMARY_HEX
        try { Color(hex.toColorInt()) } catch (_: Exception) { DEFAULT_ONPRIMARY_COLOR }
      }

  /**
   * Suspended function to persist the user's selected [ThemeSetting] to DataStore.
   */
  suspend fun setTheme(theme: ThemeSetting) {
    context.dataStore.edit { settings -> settings[THEME_KEY] = theme.name }
  }

  /** Save custom colors as hex strings (e.g. #RRGGBB). */
  suspend fun setCustomColors(primary: Color, background: Color, onBackground: Color, onPrimary: Color) {
    val primaryHex = String.format("#%06X", 0xFFFFFF and primary.toArgb())
    val backgroundHex = String.format("#%06X", 0xFFFFFF and background.toArgb())
    val onBackgroundHex = String.format("#%06X", 0xFFFFFF and onBackground.toArgb())
    val onPrimaryHex = String.format("#%06X", 0xFFFFFF and onPrimary.toArgb())
    context.dataStore.edit { settings ->
      settings[CUSTOM_PRIMARY_KEY] = primaryHex
      settings[CUSTOM_BACKGROUND_KEY] = backgroundHex
      settings[CUSTOM_ONBACKGROUND_KEY] = onBackgroundHex
      settings[CUSTOM_ONPRIMARY_KEY] = onPrimaryHex
    }
  }
}
