package com.neptune.neptune.ui.mock

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.neptune.neptune.ui.navigation.NavigationTestTags

@Composable
fun MockProfileScreen() {
  Scaffold(
      content = { pd ->
        Text(
            "Someone else's Profile Screen",
            Modifier.padding(pd).testTag(NavigationTestTags.OTHER_USER))
      })
}
