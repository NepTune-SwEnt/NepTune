package com.neptune.neptune.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.R

/**
 * Dimension holder for the profile screens.
 *
 * The values are scaled based on the screen width to keep proportions similar across devices.
 */
data class ProfileDimensions(
    val screenPadding: Dp,
    val settingsButtonSize: Dp,
    val avatarVerticalSpacing: Dp,
    val textFieldSpacing: Dp,
    val sectionVerticalSpacing: Dp,
    val topScreenPadding: Dp,
    val bioVerticalSpacing: Dp,
    val preSamplesSectionSpacing: Dp,
    val statsSpacing: Dp,
    val bottomButtonBottomPadding: Dp,
    val buttonIconSpacing: Dp,
    val topBarActionsSpacing: Dp,
    val tagsSpacing: Dp,
    val statBlockLabelSpacing: Dp,
    val inlineSpacing: Dp,
    val listBottomPadding: Dp,
)

/** CompositionLocal exposing the screen-scaled [ProfileDimensions]. */
val LocalProfileDimensions = staticCompositionLocalOf { defaultProfileDimensions(scale = 1f) }

private fun defaultProfileDimensions(scale: Float) =
    ProfileDimensions(
        screenPadding = (16 * scale).dp,
        settingsButtonSize = (30 * scale).dp,
        avatarVerticalSpacing = (15 * scale).dp,
        textFieldSpacing = (15 * scale).dp,
        sectionVerticalSpacing = (50 * scale).dp,
        topScreenPadding = (40 * scale).dp,
        bioVerticalSpacing = (30 * scale).dp,
        preSamplesSectionSpacing = (60 * scale).dp,
        statsSpacing = (20 * scale).dp,
        bottomButtonBottomPadding = (24 * scale).dp,
        buttonIconSpacing = (8 * scale).dp,
        topBarActionsSpacing = (12 * scale).dp,
        tagsSpacing = (8 * scale).dp,
        statBlockLabelSpacing = (4 * scale).dp,
        inlineSpacing = (12 * scale).dp,
        listBottomPadding = (88 * scale).dp,
    )

/**
 * Remembers the [ProfileDimensions] scaled to the current device width.
 *
 * Scaling is limited to the `[0.85, 1.25]` interval to avoid extreme jumps on tablets or small
 * phones.
 */
@Composable
fun rememberProfileDimensions(): ProfileDimensions {
  val configuration = LocalConfiguration.current
  val baseWidth = 375f
  val screenWidth = configuration.screenWidthDp.takeIf { it > 0 }?.toFloat() ?: baseWidth
  val scale = (screenWidth / baseWidth).coerceIn(0.85f, 1.25f)
  return defaultProfileDimensions(scale)
}

/** Reusable text style applied throughout the profile UI. */
fun appTextStyle(fontSize: TextUnit = 18.sp) =
    TextStyle(
        fontSize = fontSize,
        fontWeight = FontWeight(400),
        fontFamily = FontFamily(Font(R.font.markazi_text)))

/** Default font size for editable text fields on the profile screen. */
val EDIT_FIELDS_FONT_SIZE = 20.sp
