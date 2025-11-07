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
    val soundWave: Color,
    val profileIcon: Color,
    val postButton: Color,
    val shadow: Color
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
        soundWave = LightSkyBlue,
        profileIcon = Color.Unspecified,
        postButton = PurpleBlue,
        shadow = ShadowColor)

val LightExtendedColors =
    ExtendedColors(
        background = GhostWhite,
        indicatorColor = purple,
        cardBackground = LightLavenderBlue,
        listBackground = DarkBlue3,
        searchBar = LightLavenderBlue,
        accentPrimary = LightPurpleBlue,
        onBackground = DarkBlue3,
        smallText = Black,
        loginText = white,
        soundWave = DarkBlue3,
        profileIcon = DarkBlue3,
        postButton = LightPurple,
        shadow = ShadowColor)

val LocalExtendedColors = staticCompositionLocalOf {
  // default value
  DarkExtendedColors
}
