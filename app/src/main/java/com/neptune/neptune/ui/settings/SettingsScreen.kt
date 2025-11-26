package com.neptune.neptune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.NepTuneTheme

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    goBack: () -> Unit = {},
    goTheme: () -> Unit = {},
    goAccount: () -> Unit = {},
) {
  Scaffold(
      topBar = {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
          CenterAlignedTopAppBar(
              title = {
                Text(
                    text = "Settings",
                    style =
                        TextStyle(
                            fontSize = 45.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(149),
                            color = NepTuneTheme.colors.onBackground,
                        ),
                    modifier = Modifier.padding(25.dp),
                    textAlign = TextAlign.Center)
              },
              navigationIcon = {
                IconButton(onClick = goBack, modifier = Modifier.padding(horizontal = 12.dp)) {
                  Icon(
                      imageVector = Icons.Default.ArrowBackIosNew,
                      contentDescription = "Go Back",
                      tint = NepTuneTheme.colors.onBackground)
                }
              },
              colors =
                  TopAppBarDefaults.centerAlignedTopAppBarColors(
                      containerColor = NepTuneTheme.colors.background))
          HorizontalDivider(
              modifier = Modifier.fillMaxWidth(),
              thickness = 0.75.dp,
              color = NepTuneTheme.colors.onBackground)
        }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
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
        }
      }
}
