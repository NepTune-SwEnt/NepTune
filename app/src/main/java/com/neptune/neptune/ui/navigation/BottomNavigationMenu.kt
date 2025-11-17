package com.neptune.neptune.ui.navigation

import android.os.Bundle
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
import com.neptune.neptune.ui.theme.NepTuneTheme

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

  object ProjectList :
      Tab(
          "Select Project",
          R.drawable.music_note,
          Screen.ProjectList,
          NavigationTestTags.PROJECTLIST_TAB)

  object Search :
      Tab(
          "Search",
          android.R.drawable.ic_menu_search,
          Screen.Search,
          NavigationTestTags.SEARCH_TAB)

  object New :
      Tab("New", android.R.drawable.ic_menu_add, Screen.ImportFile, NavigationTestTags.IMPORT_FILE)
}

private val tabs =
    listOf(
        Tab.Main,
        Tab.Search,
        Tab.ProjectList,
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
    currentScreenArguments: Bundle? = null
) {
  if (!screen.showBottomBar) {
    return
  }
  Column {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.75.dp,
        color = NepTuneTheme.colors.onBackground)
    NavigationBar(
        modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
        containerColor = NepTuneTheme.colors.background) {
          tabs.forEach { tab ->
            val isSelected: Boolean =
                when (tab.destination) {
                  Screen.ProjectList -> {
                    val onProjectListScreen = (screen == Screen.ProjectList)
                    val purposeIsEdit = (currentScreenArguments?.getString("purpose") == "edit")

                    onProjectListScreen && purposeIsEdit
                  }
                  else -> {
                    tab == getTabForRoute(screen.route)
                  }
                }
            NavigationBarItem(
                icon = {
                  Icon(
                      painter = painterResource(id = tab.icon),
                      contentDescription = tab.name,
                      modifier = Modifier.size(33.dp),
                      tint = NepTuneTheme.colors.onBackground)
                },
                alwaysShowLabel = false,
                label = { Text(tab.name) },
                selected = isSelected,
                onClick = { navigationActions?.navigateTo(tab.destination) },
                enabled = navigationActions != null,
                modifier = Modifier.testTag(tab.testTag),
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedTextColor = NepTuneTheme.colors.accentPrimary,
                        selectedIconColor = NepTuneTheme.colors.accentPrimary,
                        unselectedIconColor = NepTuneTheme.colors.onBackground,
                        indicatorColor = NepTuneTheme.colors.indicatorColor))
          }
        }
  }
}
