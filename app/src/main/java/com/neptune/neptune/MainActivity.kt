package com.neptune.neptune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.neptune.neptune.ui.picker.AppRoot
import com.neptune.neptune.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SampleAppTheme {
        AppRoot() // AppRoot wires DB → repo → importer → use cases → VM → UI
      }
    }
  }
}
