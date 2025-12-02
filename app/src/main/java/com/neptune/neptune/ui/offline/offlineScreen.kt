package com.neptune.neptune.ui.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

@Composable
fun OfflineScreen() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = "Offline",
            tint = NepTuneTheme.colors.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Offline Mode Active",
            style =
                TextStyle(
                    fontSize = 32.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight.Bold,
                    color = NepTuneTheme.colors.onBackground),
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text =
                "You can't see the feed right now, but you can still create and modify your local projects.",
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight.Normal,
                    color = NepTuneTheme.colors.onBackground.copy(alpha = 0.8f)),
            textAlign = TextAlign.Center)
      }
}
