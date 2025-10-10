package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentScreen: Screen?,
    navigationActions: NavigationActions?,
    canNavigateBack: Boolean,
) {
  CenterAlignedTopAppBar(
      modifier = Modifier.fillMaxWidth().height(112.dp).testTag(NavigationTestTags.TOP_BAR),
      title = {
        Text(
            text = currentScreen?.name ?: "",
            style =
                TextStyle(
                    fontSize = 45.sp,
                    fontFamily = FontFamily(Font(R.font.lily_script_one)),
                    fontWeight = FontWeight(149),
                    color = LightTurquoise,
                ),
            modifier = Modifier.padding(25.dp).testTag(NavigationTestTags.TOP_BAR_TITLE),
            textAlign = TextAlign.Center)
      },
      navigationIcon = {
        if (canNavigateBack && navigationActions != null) {
          IconButton(
              onClick = { navigationActions.goBack() },
              modifier =
                  Modifier.padding(vertical = 25.dp, horizontal = 17.dp)
                      .size(57.dp)
                      .testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_revert),
                    contentDescription = "Go Back",
                    tint = LightTurquoise)
              }
        }
      },
      actions = {
        if (currentScreen?.showProfile == true && navigationActions != null) {
          IconButton(
              onClick = { navigationActions.navigateTo(Screen.Profile) },
              modifier =
                  Modifier.padding(vertical = 25.dp, horizontal = 17.dp)
                      .size(57.dp)
                      .testTag(NavigationTestTags.PROFILE_BUTTON)) {
                Icon(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    tint = Color.Unspecified)
              }
        }
      },
      colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = DarkBlue1 // sets TopAppBar background
              ))
}
