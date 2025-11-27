package com.neptune.neptune.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.neptune.neptune.ui.settings.ThemeDataStore
import com.neptune.neptune.ui.settings.ThemeSetting

private val DarkColorScheme =
    darkColorScheme(primary = LightPurple, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
    lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)

private const val LUMINANCE_THRESHOLD = 0.5f
private const val DARK_THEME_CARD_ALPHA = 0.95f
private const val LIGHT_THEME_CARD_ALPHA = 0.98f
private const val DARK_THEME_SEARCH_BRIGHTNESS_FACTOR = 0.9f
private const val DARK_THEME_SEARCH_BRIGHTNESS_OFFSET = 0.1f
private const val LIGHT_THEME_SEARCH_DARKNESS_FACTOR = 0.95f

object NepTuneTheme {
  val colors: ExtendedColors
    @Composable get() = LocalExtendedColors.current
}

@Composable
fun SampleAppTheme(
    themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    // For custom themes we accept four input Colors. Any may be null and will fall
    // back to sensible defaults. These four drive creation of the complete ExtendedColors.
    customPrimary: Color? = null,
    customBackground: Color? = null,
    customAccent: Color? = null,
    customOnBackground: Color? = null,
    content: @Composable () -> Unit
) {
  val darkTheme =
      when (themeSetting) {
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.CUSTOM -> {
          // infer dark/light from the chosen background luminance
          val bg = customBackground ?: ThemeDataStore.DEFAULT_BACKGROUND_COLOR
          bg.luminance() < LUMINANCE_THRESHOLD
        }
      }

  val extendedColors =
      when (themeSetting) {
        ThemeSetting.CUSTOM -> {
          val primary = customPrimary ?: ThemeDataStore.DEFAULT_PRIMARY_COLOR
          val background = customBackground ?: ThemeDataStore.DEFAULT_BACKGROUND_COLOR
          val accentPrimary = customAccent ?: primary
          val onBackground =
              customOnBackground
                  ?: if (background.luminance() > LUMINANCE_THRESHOLD) Black else White
          val onPrimary = onBackground

          // Derive other colors from these inputs with simple heuristics
          val cardBackground =
              if (darkTheme) background.copy(alpha = DARK_THEME_CARD_ALPHA)
              else background.copy(alpha = LIGHT_THEME_CARD_ALPHA)
          val listBackground = cardBackground
          // Search bar should be slightly brighter if background is dark, slightly darker if
          // background is light
          val searchBar =
              if (darkTheme) {
                background.copy(
                    red =
                        background.red * DARK_THEME_SEARCH_BRIGHTNESS_FACTOR +
                            DARK_THEME_SEARCH_BRIGHTNESS_OFFSET,
                    green =
                        background.green * DARK_THEME_SEARCH_BRIGHTNESS_FACTOR +
                            DARK_THEME_SEARCH_BRIGHTNESS_OFFSET,
                    blue =
                        background.blue * DARK_THEME_SEARCH_BRIGHTNESS_FACTOR +
                            DARK_THEME_SEARCH_BRIGHTNESS_OFFSET)
              } else {
                background.copy(
                    red = background.red * LIGHT_THEME_SEARCH_DARKNESS_FACTOR,
                    green = background.green * LIGHT_THEME_SEARCH_DARKNESS_FACTOR,
                    blue = background.blue * LIGHT_THEME_SEARCH_DARKNESS_FACTOR)
              }
          val smallText = if (darkTheme) Black else White
          val loginText = if (accentPrimary.luminance() > LUMINANCE_THRESHOLD) Black else White
          val soundWave = accentPrimary
          val postButton = accentPrimary
          val shadow = ShadowColor
          val animation = accentPrimary

          ExtendedColors(
              background = background,
              indicatorColor = accentPrimary,
              cardBackground = cardBackground,
              listBackground = listBackground,
              searchBar = searchBar,
              accentPrimary = accentPrimary,
              onBackground = onBackground,
              onPrimary = onPrimary,
              smallText = smallText,
              loginText = loginText,
              soundWave = soundWave,
              postButton = postButton,
              shadow = shadow,
              animation = animation,
              online = Green,
              offline = Gray)
        }
        else -> if (darkTheme) DarkExtendedColors else LightExtendedColors
      }

  val colorScheme =
      if (themeSetting == ThemeSetting.CUSTOM) {
        val primary = customPrimary ?: ThemeDataStore.DEFAULT_PRIMARY_COLOR
        val onPrimary =
            customOnBackground
                ?: if ((customPrimary ?: ThemeDataStore.DEFAULT_PRIMARY_COLOR).luminance() >
                    LUMINANCE_THRESHOLD)
                    Black
                else White
        if (darkTheme) darkColorScheme(primary = primary, onPrimary = onPrimary)
        else lightColorScheme(primary = primary, onPrimary = onPrimary)
      } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
      }

  CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
    }
  }
}
