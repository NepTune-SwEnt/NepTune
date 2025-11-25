package com.neptune.neptune.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.neptune.neptune.ui.theme.NepTuneTheme
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.IconButton as M3IconButton
import com.godaddy.android.colorpicker.HsvColor

/**
 * Custom theme editor: save colorpicker output as custom colors and set ThemeSetting.CUSTOM.
 */
@Composable
fun SettingsCustomThemeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    goBack: () -> Unit = {}
) {
    var harmonyMode by remember { mutableStateOf(ColorHarmonyMode.COMPLEMENTARY) }
    var expanded by remember { mutableStateOf(false) }

    // collect current stored colors
    val currentPrimary by settingsViewModel.customPrimaryColor.collectAsState()
    val currentBackground by settingsViewModel.customBackgroundColor.collectAsState()
    val currentOnBackground by settingsViewModel.customOnBackgroundColor.collectAsState()
    val currentOnPrimary by settingsViewModel.customOnPrimaryColor.collectAsState()

    // which color are we editing? 0=primary,1=background,2=onBackground,3=onPrimary
    var editingIndex by remember { mutableStateOf(0) }

    // Local temporary colors — avoid persisting on every picker move to prevent lag
    var tempPrimary by remember { mutableStateOf(currentPrimary) }
    var tempBackground by remember { mutableStateOf(currentBackground) }
    var tempOnBackground by remember { mutableStateOf(currentOnBackground) }
    var tempOnPrimary by remember { mutableStateOf(currentOnPrimary) }

    // keep temps in sync when persisted values change (e.g., on enter)
    LaunchedEffect(currentPrimary, currentBackground, currentOnBackground, currentOnPrimary) {
      tempPrimary = currentPrimary
      tempBackground = currentBackground
      tempOnBackground = currentOnBackground
      tempOnPrimary = currentOnPrimary
    }

    // Auto-apply debounce: persist temporary colors after the user stops changing them for a short delay
    val AUTO_APPLY_DELAY = 800L
    LaunchedEffect(tempPrimary, tempBackground, tempOnBackground, tempOnPrimary) {
      // If temps match persisted values, don't trigger a write
      if (tempPrimary == currentPrimary &&
          tempBackground == currentBackground &&
          tempOnBackground == currentOnBackground &&
          tempOnPrimary == currentOnPrimary) {
        return@LaunchedEffect
      }
      delay(AUTO_APPLY_DELAY)
      // Persist once after debounce
      settingsViewModel.updateCustomColors(tempPrimary, tempBackground, tempOnBackground, tempOnPrimary)
      settingsViewModel.updateTheme(ThemeSetting.CUSTOM)
    }

  Scaffold(
    topBar = {
      M3IconButton(onClick = goBack, modifier = Modifier.padding(16.dp)) {
        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = NepTuneTheme.colors.onBackground)
      }
    },
    containerColor = NepTuneTheme.colors.background
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        // Picker mode selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Harmony mode:", color = NepTuneTheme.colors.onBackground)
            Spacer(Modifier.width(12.dp))
            Button(onClick = { expanded = true }) {
                Text(harmonyMode.name.lowercase().replaceFirstChar { it.uppercase() })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ColorHarmonyMode.entries.forEach { mode ->
                    DropdownMenuItem(text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = {
                        harmonyMode = mode
                        expanded = false
                    })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Selector for which of the four colors to edit + preview of all four
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { editingIndex = 0 }) { Text("Primary", color = if (editingIndex == 0) NepTuneTheme.colors.background else NepTuneTheme.colors.onBackground) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { editingIndex = 1 }) { Text("Background", color = if (editingIndex == 1) NepTuneTheme.colors.background else NepTuneTheme.colors.onBackground) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { editingIndex = 2 }) { Text("OnBackground", color = if (editingIndex == 2) NepTuneTheme.colors.background else NepTuneTheme.colors.onBackground) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { editingIndex = 3 }) { Text("OnPrimary", color = if (editingIndex == 3) NepTuneTheme.colors.background else NepTuneTheme.colors.onBackground) }
            Spacer(Modifier.width(16.dp))
            // preview boxes for all four (show temporary edits)
            val primaryColor = tempPrimary
            val backgroundColor = tempBackground
            val onBackgroundColor = tempOnBackground
            val onPrimaryColor = tempOnPrimary
            Column {
                Row {
                    Column(modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(primaryColor)) {}
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(backgroundColor)) {}
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(onBackgroundColor)) {}
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(onPrimaryColor)) {}
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Use a slightly smaller picker and update only temp state on color changes to avoid
        // frequent DataStore writes which cause UI lag. Persist when user taps Apply.
         HarmonyColorPicker(
              modifier = Modifier.size(340.dp),
              harmonyMode = harmonyMode,
              color = HsvColor.from(
                  when (editingIndex) {
                    0 -> tempPrimary
                    1 -> tempBackground
                    2 -> tempOnBackground
                    else -> tempOnPrimary
                  }
              ),
              showBrightnessBar = true,
             onColorChanged = { color ->
                val picked = color.toColor()
                when (editingIndex) {
                  0 -> tempPrimary = picked
                  1 -> tempBackground = picked
                  2 -> tempOnBackground = picked
                  else -> tempOnPrimary = picked
                }
             })

        Spacer(Modifier.height(12.dp))

        // Apply / Cancel controls: persist only when user confirms
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                // persist current temporary palette once
                settingsViewModel.updateCustomColors(tempPrimary, tempBackground, tempOnBackground, tempOnPrimary)
                settingsViewModel.updateTheme(ThemeSetting.CUSTOM)
            }) { Text("Apply") }
            OutlinedButton(onClick = {
                // revert temporary edits to persisted values
                tempPrimary = currentPrimary
                tempBackground = currentBackground
                tempOnBackground = currentOnBackground
                tempOnPrimary = currentOnPrimary
            }) { Text("Cancel") }
        }
    }
  }
}
