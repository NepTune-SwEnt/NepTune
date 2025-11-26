package com.neptune.neptune.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.neptune.neptune.ui.theme.NepTuneTheme
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
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
    // collect current stored colors
    val currentPrimary by settingsViewModel.customPrimaryColor.collectAsState()
    val currentBackground by settingsViewModel.customBackgroundColor.collectAsState()
    val currentOnBackground by settingsViewModel.customOnBackgroundColor.collectAsState()
    val currentOnPrimary by settingsViewModel.customOnPrimaryColor.collectAsState()

    // which color are we editing? 0=primary,1=background,2=onBackground,3=onPrimary
    var editingIndex by remember { mutableIntStateOf(0) }

    // Local temporary colors — avoid persisting on every picker move to prevent lag
    var tempPrimary by remember { mutableStateOf(HsvColor.from(currentPrimary)) }
    var tempBackground by remember { mutableStateOf(HsvColor.from(currentBackground)) }
    var tempOnBackground by remember { mutableStateOf(HsvColor.from(currentOnBackground)) }
    var tempOnPrimary by remember { mutableStateOf(HsvColor.from(currentOnPrimary)) }

    // keep temps in sync when persisted values change (e.g., on enter)
    LaunchedEffect(currentPrimary, currentBackground, currentOnBackground, currentOnPrimary) {
      tempPrimary = HsvColor.from(currentPrimary)
      tempBackground = HsvColor.from(currentBackground)
      tempOnBackground = HsvColor.from(currentOnBackground)
      tempOnPrimary = HsvColor.from(currentOnPrimary)
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

        var expanded by remember { mutableStateOf(false) }
        val items = listOf("Primary", "Background", "OnBackground", "OnPrimary")
        // Selector for which of the four colors to edit
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Editing color:", color = NepTuneTheme.colors.onBackground)
            Spacer(Modifier.width(12.dp))
            Button(onClick = { expanded = true }) {
                Text(items[editingIndex])
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEachIndexed { index, text ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        editingIndex = index
                        expanded = false
                    })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // preview of all four
        Row(verticalAlignment = Alignment.CenterVertically) {
            val primaryColor = tempPrimary.toColor()
            val backgroundColor = tempBackground.toColor()
            val onBackgroundColor = tempOnBackground.toColor()
            val onPrimaryColor = tempOnPrimary.toColor()
            Column(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(primaryColor)
            ) {}
            Spacer(Modifier.width(6.dp))
            Column(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
            ) {}
            Spacer(Modifier.width(6.dp))
            Column(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(onBackgroundColor)
            ) {}
            Spacer(Modifier.width(6.dp))
            Column(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(onPrimaryColor)
            ) {}
        }

        Spacer(Modifier.height(12.dp))

        // Use a slightly smaller picker and update only temp state on color changes to avoid
        // frequent DataStore writes which cause UI lag. Persist when user taps Apply.
         HarmonyColorPicker(
              modifier = Modifier.size(340.dp).align(Alignment.CenterHorizontally),
              harmonyMode = ColorHarmonyMode.NONE,
              color = when (editingIndex) {
                    0 -> tempPrimary
                    1 -> tempBackground
                    2 -> tempOnBackground
                    else -> tempOnPrimary
              },
              showBrightnessBar = true,
             onColorChanged = { color ->
                when (editingIndex) {
                  0 -> tempPrimary = color
                  1 -> tempBackground = color
                  2 -> tempOnBackground = color
                  else -> tempOnPrimary = color
                }
             })

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                settingsViewModel.updateCustomColors(
                    tempPrimary.toColor(),
                    tempBackground.toColor(),
                    tempOnBackground.toColor(),
                    tempOnPrimary.toColor()
                )
                settingsViewModel.updateTheme(ThemeSetting.CUSTOM)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Apply")
        }
     }
   }
 }
