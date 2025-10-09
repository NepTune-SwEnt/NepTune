package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.DarkBlue2
import com.neptune.neptune.ui.theme.LightPurpleBlue
import com.neptune.neptune.ui.theme.LightTurquoise

sealed class Tab(val name: String, val icon: Int, val destination: Screen, val testTag: String) {

  object Main : Tab("Home", R.drawable.home_planet, Screen.Main, NavigationTestTags.MAIN_TAB)

  object Edit : Tab("Sampler", R.drawable.music_note, Screen.Edit, NavigationTestTags.EDIT_TAB)
}

private val tabs =
    listOf(
        Tab.Main,
        Tab.Edit,
    )

fun getTabForRoute(route: String?): Tab? {
  return tabs.firstOrNull { it.destination.route == route }
}

@Composable
fun BottomNavigationMenu(
    screen: Screen = Screen.Main,
    navigationActions: NavigationActions? = null,
) {
  if (!screen.showBottomBar) {
    return
  }
  NavigationBar(
      modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = DarkBlue1) {
        tabs.forEach { tab ->
          NavigationBarItem(
              icon = {
                Icon(
                    painter = painterResource(id = tab.icon),
                    contentDescription = tab.name,
                    modifier = Modifier.size(33.dp))
              },
              alwaysShowLabel = false,
              label = { Text(tab.name) },
              selected = tab == getTabForRoute(screen.route),
              onClick = { navigationActions?.navigateTo(tab.destination) },
              enabled = navigationActions != null,
              modifier = Modifier.testTag(tab.testTag),
              colors =
                  NavigationBarItemDefaults.colors(
                      selectedTextColor = LightPurpleBlue,
                      selectedIconColor = LightPurpleBlue,
                      unselectedIconColor = LightTurquoise,
                      indicatorColor = DarkBlue2))
        }
      }
}
