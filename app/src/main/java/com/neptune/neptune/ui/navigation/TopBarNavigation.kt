package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.LightTurquoise

object TopBarTags {
  const val TOP_BAR = "topBar"
  const val TOP_BAR_TITLE = "topBarTitle"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarNavigation() {
  Column {
    CenterAlignedTopAppBar(
        modifier = Modifier.fillMaxWidth().height(112.dp).testTag(TopBarTags.TOP_BAR),
        title = {
          Text(
              text = "NepTune",
              style =
                  TextStyle(
                      fontSize = 45.sp,
                      fontFamily = FontFamily(Font(R.font.lily_script_one)),
                      fontWeight = FontWeight(149),
                      color = LightTurquoise,
                  ),
              modifier = Modifier.padding(25.dp).testTag(TopBarTags.TOP_BAR_TITLE),
              textAlign = TextAlign.Center)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBlue1))
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(), thickness = 0.75.dp, color = LightTurquoise)
  }
}
