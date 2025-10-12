package com.neptune.neptune.ui.mock

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MockProfileScreen() {
  Scaffold(content = { pd -> Text("Profile Screen", Modifier.padding(pd)) })
}
