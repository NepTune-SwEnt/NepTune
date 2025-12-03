package com.neptune.neptune.ui.offline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OfflineBanner() {
  Surface(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      color = Color.Red,
      shape = RoundedCornerShape(12.dp),
      tonalElevation = 4.dp,
      shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
              Icon(
                  imageVector = Icons.Default.WifiOff,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(16.dp))
              Text(
                  text = "No connection. Showing offline data.",
                  style =
                      TextStyle(
                          color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            }
      }
}
