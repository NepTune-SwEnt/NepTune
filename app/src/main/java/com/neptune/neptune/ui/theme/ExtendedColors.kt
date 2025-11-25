package com.neptune.neptune.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val background: Color,      // main background color
    val indicatorColor: Color,  // buttons and selected tab indicator, slightly different from background
    val cardBackground: Color,  // cards background color
    val listBackground: Color,  // background of selected project in lists
    val searchBar: Color,       // search bar background color
    val accentPrimary: Color,   // primary accent color for highlights
    val onBackground: Color,    // text, icons and cards color on background
    val onPrimary: Color,       // text color used on primary (buttons)
    val smallText: Color,       // secondary text color
    val loginText: Color,       // text color for login screen
    val soundWave: Color,       // sound wave color in the player
    val postButton: Color,      // post button background color
    val shadow: Color,          // shadow color for elevated elements
    val animation: Color,       // color used in animations
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
        onPrimary = White,
        smallText = LightLavender,
        loginText = Black,
        soundWave = LightSkyBlue,
        postButton = PurpleBlue,
        shadow = ShadowColor,
        animation = DarkPurple)

val LightExtendedColors =
    ExtendedColors(
        background = GhostWhite,
        indicatorColor = Purple,
        cardBackground = LightLavenderBlue,
        listBackground = DarkBlue3,
        searchBar = LightLavenderBlue,
        accentPrimary = LightPurpleBlue,
        onBackground = DarkBlue3,
        onPrimary = White,
        smallText = Black,
        loginText = White,
        soundWave = DarkBlue3,
        postButton = LightPurple,
        shadow = ShadowColor,
        animation = Purple)

val LocalExtendedColors = staticCompositionLocalOf {
  // default value
  DarkExtendedColors
}
