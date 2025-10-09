package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    currentScreen: Screen? = Screen.Main,
    navigationActions: NavigationActions? = null,
    canNavigateBack: Boolean = true
) {
  // TODO: Change background color when Neptune theme is available
  TopAppBar(
      title = { Text(currentScreen?.name ?: "") },
      navigationIcon = {
        if (canNavigateBack && navigationActions != null) {
          Icon(
              androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_revert),
              contentDescription = null,
              modifier =
                  Modifier.clip(RoundedCornerShape(50)).clickable {
                    navigationActions.goBack()
                  })
        }
      },
      actions = {
          if (!canNavigateBack && navigationActions != null) {
              Icon(
                  androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_myplaces),
                  contentDescription = null,
                  modifier =
                      Modifier.clip(RoundedCornerShape(50)).clickable {
                          navigationActions.navigateTo(Screen.Profile)
                      })
          }
      }
  )
}
