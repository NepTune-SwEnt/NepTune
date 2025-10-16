package com.neptune.neptune.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val background: Color,
    val indicatorColor: Color,
    val cardBackground: Color,
    val listBackground: Color,
    val searchBar: Color,
    val accentPrimary: Color,
    val onBackground: Color,
    val smallText: Color,
    val loginText: Color,
    val soundWave: Color
)

val DarkExtendedColors =
    ExtendedColors(
        background = DarkBlue1,
        indicatorColor = DarkBlue2,
        cardBackground = DarkBlueGray,
        listBackground = DarkBlueGray,
        searchBar = FadedDarkBlue,
        accentPrimary = LightPurpleBlue,
        onBackground = LightTurquoise,
        smallText = LightLavender,
        loginText = Black,
        soundWave = LightSkyBlue)

val LightExtendedColors = DarkExtendedColors

val LocalExtendedColors = staticCompositionLocalOf {
  // default value
  DarkExtendedColors
}
