package com.neptune.neptune.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
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

/**
 * Tabs used in the bottom navigation bar.
 *
 * @param name The name of the tab
 * @param icon The icon of the tab
 * @param destination The destination screen of the tab
 * @param testTag The test tag of the tab
 */
sealed class Tab(val name: String, val icon: Int, val destination: Screen, val testTag: String) {

  object Main : Tab("Home", R.drawable.home_planet, Screen.Main, NavigationTestTags.MAIN_TAB)

  object Edit :
      Tab("Select Project", R.drawable.music_note, Screen.ProjectList, NavigationTestTags.EDIT_TAB)

  object Search :
      Tab(
          "Search",
          android.R.drawable.ic_menu_search,
          Screen.Search,
          NavigationTestTags.SEARCH_TAB)

  // TODO update post tag
  object New :
      Tab("New", android.R.drawable.ic_menu_add, Screen.ImportFile, NavigationTestTags.POST_TAB)
}

private val tabs =
    listOf(
        Tab.Main,
        Tab.Search,
        Tab.Edit,
        Tab.New,
    )

/**
 * Get the tab for the given route.
 *
 * @param route The route of the screen
 * @return The tab corresponding to the route, or null if no tab matches
 */
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
  Column {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(), thickness = 0.75.dp, color = LightTurquoise)
    NavigationBar(
        modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
        containerColor = DarkBlue1) {
          tabs.forEach { tab ->
            NavigationBarItem(
                icon = {
                  Icon(
                      painter = painterResource(id = tab.icon),
                      contentDescription = tab.name,
                      modifier = Modifier.size(33.dp),
                      tint = LightTurquoise)
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
}
