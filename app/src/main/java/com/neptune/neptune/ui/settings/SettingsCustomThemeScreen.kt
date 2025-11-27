package com.neptune.neptune.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.neptune.neptune.ui.theme.DarkExtendedColors
import com.neptune.neptune.ui.theme.LightExtendedColors
import com.neptune.neptune.ui.theme.NepTuneTheme
import kotlin.math.max
import kotlin.math.min

object CustomThemeScreenTestTags {
  const val CUSTOM_THEME_TOP_BAR = "custom_theme_top_bar"
  const val COLOR_PREVIEW_BOX_PRIMARY = "color_preview_box_primary"
  const val COLOR_PREVIEW_BOX_BACKGROUND = "color_preview_box_background"
  const val COLOR_PREVIEW_BOX_ON_BACKGROUND = "color_preview_box_on_background"
  const val COLOR_PICKER = "color_picker"
  const val APPLY_BUTTON = "apply_button"
  const val RESET_BUTTON = "reset_button"
  const val CONTRAST_WARNING_DIALOG = "contrast_warning_dialog"
}

/** Custom theme editor: save colorpicker output as custom colors and set ThemeSetting.CUSTOM. */
@Composable
fun SettingsCustomThemeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    goBack: () -> Unit = {}
) {
  // collect current stored colors
  val currentPrimary by settingsViewModel.customPrimaryColor.collectAsState()
  val currentBackground by settingsViewModel.customBackgroundColor.collectAsState()
  val currentOnBackground by settingsViewModel.customOnBackgroundColor.collectAsState()

  // which color are we editing? 0=primary,1=background,2=onBackground
  var editingIndex by remember { mutableIntStateOf(0) }

  // Local temporary colors
  var tempPrimary by remember { mutableStateOf(HsvColor.from(currentPrimary)) }
  var tempBackground by remember { mutableStateOf(HsvColor.from(currentBackground)) }
  var tempOnBackground by remember { mutableStateOf(HsvColor.from(currentOnBackground)) }

  var showContrastWarning by remember { mutableStateOf(false) }
  var contrastWarningMessage by remember { mutableStateOf("") }

  if (showContrastWarning) {
    AlertDialog(
        modifier = Modifier.testTag(CustomThemeScreenTestTags.CONTRAST_WARNING_DIALOG),
        onDismissRequest = { showContrastWarning = false },
        title = { Text("Low Contrast Warning") },
        text = { Text(contrastWarningMessage) },
        confirmButton = { Button(onClick = { showContrastWarning = false }) { Text("OK") } })
  }

  // keep temps in sync when persisted values change
  LaunchedEffect(currentPrimary, currentBackground, currentOnBackground) {
    tempPrimary = HsvColor.from(currentPrimary)
    tempBackground = HsvColor.from(currentBackground)
    tempOnBackground = HsvColor.from(currentOnBackground)
  }

  Scaffold(
      topBar = {
        SettingsTopBar(
            title = "Custom Theme",
            goBack = goBack,
            modifier = Modifier.testTag(CustomThemeScreenTestTags.CUSTOM_THEME_TOP_BAR))
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {

          // preview of all three
          Row(verticalAlignment = Alignment.CenterVertically) {
            val primaryColor = tempPrimary.toColor()
            val backgroundColor = tempBackground.toColor()
            val onBackgroundColor = tempOnBackground.toColor()

            Box(
                modifier =
                    Modifier.size(width = 90.dp, height = 40.dp)
                        .clickable { editingIndex = 0 }
                        .background(primaryColor, shape = RoundedCornerShape(6.dp))
                        .border(
                            width = if (editingIndex == 0) 2.dp else 0.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(6.dp))
                        .testTag(CustomThemeScreenTestTags.COLOR_PREVIEW_BOX_PRIMARY),
                contentAlignment = Alignment.Center) {
                  Text("Primary", color = onBackgroundColor)
                }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier =
                    Modifier.size(width = 90.dp, height = 40.dp)
                        .clickable { editingIndex = 1 }
                        .background(backgroundColor, shape = RoundedCornerShape(6.dp))
                        .border(
                            width = if (editingIndex == 1) 2.dp else 0.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(6.dp))
                        .testTag(CustomThemeScreenTestTags.COLOR_PREVIEW_BOX_BACKGROUND),
                contentAlignment = Alignment.Center) {
                  Text("Background", color = onBackgroundColor)
                }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier =
                    Modifier.size(width = 90.dp, height = 40.dp)
                        .clickable { editingIndex = 2 }
                        .background(onBackgroundColor, shape = RoundedCornerShape(6.dp))
                        .border(
                            width = if (editingIndex == 2) 2.dp else 0.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(6.dp))
                        .testTag(CustomThemeScreenTestTags.COLOR_PREVIEW_BOX_ON_BACKGROUND),
                contentAlignment = Alignment.Center) {
                  Text("Text", color = backgroundColor)
                }
          }

          Spacer(Modifier.height(12.dp))

          HarmonyColorPicker(
              modifier =
                  Modifier.size(340.dp)
                      .align(Alignment.CenterHorizontally)
                      .testTag(CustomThemeScreenTestTags.COLOR_PICKER),
              harmonyMode = ColorHarmonyMode.NONE,
              color =
                  when (editingIndex) {
                    0 -> tempPrimary
                    1 -> tempBackground
                    else -> tempOnBackground
                  },
              showBrightnessBar = true,
              onColorChanged = { color ->
                when (editingIndex) {
                  0 -> tempPrimary = color
                  1 -> tempBackground = color
                  else -> tempOnBackground = color
                }
              })

          Spacer(Modifier.height(16.dp))

          Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Button(
                modifier = Modifier.testTag(CustomThemeScreenTestTags.APPLY_BUTTON),
                onClick = {
                  val primaryColor = tempPrimary.toColor()
                  val backgroundColor = tempBackground.toColor()
                  val onBackgroundColor = tempOnBackground.toColor()

                  val onBackgroundContrast =
                      calculateContrastRatio(onBackgroundColor, backgroundColor)
                  val onPrimaryContrast =
                      calculateContrastRatio(
                          onBackgroundColor, primaryColor) // onPrimary is onBackground
                  val primaryBackgroundContrast =
                      calculateContrastRatio(primaryColor, backgroundColor)

                  val messages = mutableListOf<String>()
                  if (onBackgroundContrast < 1.2f) {
                    messages.add("The text color has low contrast with the background color.")
                  }
                  if (onPrimaryContrast < 1.2f) {
                    messages.add("The text color has low contrast with the primary color.")
                  }
                  if (primaryBackgroundContrast < 1.2f) {
                    messages.add("The primary and background colors are too similar.")
                  }

                  if (messages.isNotEmpty()) {
                    contrastWarningMessage =
                        messages.joinToString(" ") +
                            " Please adjust for better readability and distinction."
                    showContrastWarning = true
                  } else {
                    settingsViewModel.updateCustomColors(
                        primaryColor,
                        backgroundColor,
                        onBackgroundColor,
                    )
                    settingsViewModel.updateTheme(ThemeSetting.CUSTOM)
                  }
                },
            ) {
              Text("Apply")
            }
            Spacer(Modifier.width(16.dp))
            val isSystemDark = isSystemInDarkTheme()
            Button(
                modifier = Modifier.testTag(CustomThemeScreenTestTags.RESET_BUTTON),
                onClick = {
                  val defaultColors = if (isSystemDark) DarkExtendedColors else LightExtendedColors
                  settingsViewModel.updateCustomColors(
                      primary = defaultColors.accentPrimary,
                      background = defaultColors.background,
                      onBackground = defaultColors.onBackground,
                  )
                  settingsViewModel.updateTheme(ThemeSetting.CUSTOM)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                  Text("Reset", color = Color.White)
                }
          }
        }
      }
}

private fun calculateContrastRatio(color1: Color, color2: Color): Float {
  val luminance1 = color1.luminance()
  val luminance2 = color2.luminance()
  val lighterLuminance = max(luminance1, luminance2)
  val darkerLuminance = min(luminance1, luminance2)
  return (lighterLuminance + 0.05f) / (darkerLuminance + 0.05f)
}
