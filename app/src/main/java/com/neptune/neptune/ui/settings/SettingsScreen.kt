package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.ui.settings.SettingsScreenTestTags.DISABLE_HELP_SWITCH
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.util.NeptuneTopBar
import kotlinx.coroutines.launch

object SettingsScreenTestTags {
  const val DISABLE_HELP_SWITCH = "disableHelpSwitch"
}

/**
 * Displays the settings screen.
 *
 * This screen provides a top bar with a back navigation button and hosts various settings sections,
 * such as theme selection.
 *
 * @param goBack A lambda function to be invoked when the user clicks the back button, typically
 *   used for navigation.
 * @param goTheme A lambda function to be invoked when the user selects the "Theme" settings item.
 * @param goAccount A lambda function to be invoked when the user selects the "Account
 */
@Composable
fun SettingsScreen(
    goBack: () -> Unit = {},
    goTheme: () -> Unit = {},
    goAccount: () -> Unit = {},
) {
  Scaffold(
      topBar = { NeptuneTopBar(title = "Settings", goBack = goBack) },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        val context = NepTuneApplication.appContext
        val dataStore = remember { ThemeDataStore(context) }
        val disabled by dataStore.disableHelp.collectAsState(initial = false)
        val coroutineScope = rememberCoroutineScope()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          item {
            SettingItemCard(
                text = "Theme",
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "go to theme settings",
                onClick = goTheme)
          }
          item {
            SettingItemCard(
                text = "Account",
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "go to account settings",
                onClick = goAccount)
          }
          item {
            Card(
                onClick = { coroutineScope.launch { dataStore.setDisableHelp(!disabled) } },
                modifier = Modifier.fillMaxWidth().testTag(DISABLE_HELP_SWITCH),
                colors =
                    CardDefaults.cardColors(containerColor = NepTuneTheme.colors.cardBackground),
            ) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Disable sampler help button",
                        style =
                            TextStyle(
                                fontSize = 22.sp,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight.Normal,
                                color = NepTuneTheme.colors.onBackground,
                            ))
                    Switch(
                        checked = disabled,
                        onCheckedChange = { checked ->
                          coroutineScope.launch { dataStore.setDisableHelp(checked) }
                        },
                        colors =
                            SwitchDefaults.colors(
                                uncheckedThumbColor = NepTuneTheme.colors.onBackground))
                  }
            }
          }
        }
      }
}
