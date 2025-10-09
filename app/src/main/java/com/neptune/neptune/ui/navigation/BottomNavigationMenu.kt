package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

sealed class Tab(val name: String, val icon: Int, val destination: Screen, val testTag: String) {

  // TODO: Replace with Neptune icons
  object Main :
      Tab("Main", android.R.drawable.ic_menu_view, Screen.Main, NavigationTestTags.MAIN_TAB)

  object Edit :
      Tab("Edit", android.R.drawable.ic_menu_edit, Screen.Edit, NavigationTestTags.EDIT_TAB)
}

private val tabs =
    listOf(
        Tab.Main,
        Tab.Edit,
    )

fun getTabForRoute(route: String?): Tab? {
  return tabs.firstOrNull { it.destination.route == route }
}

@Preview
@Composable
fun BottomNavigationMenu(
    modifier: Modifier = Modifier,
    screen: Screen = Screen.Main,
    navigationActions: NavigationActions? = null,
) {
  // TODO: Add condition for showing/hiding bottom nav bar on certain screens
  // TODO: Change background color when Neptune theme is available
  if (!screen.showBottomBar) {
    return
  }
  NavigationBar(modifier = modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)) {
    tabs.forEach { tab ->
      NavigationBarItem(
          icon = {
            Icon(androidx.compose.ui.res.painterResource(id = tab.icon), contentDescription = null)
          },
          label = { Text(tab.name) },
          selected = tab == getTabForRoute(screen.route),
          onClick = { navigationActions?.navigateTo(tab.destination) },
          enabled = navigationActions != null,
          modifier = Modifier.clip(RoundedCornerShape(50.dp)).testTag(tab.testTag))
    }
  }
}
