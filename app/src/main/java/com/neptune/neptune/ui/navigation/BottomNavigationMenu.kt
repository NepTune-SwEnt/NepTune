package com.neptune.neptune.ui.navigation

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
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
sealed class Tab(
    val name: String,
    val icon: Int,
    val destination: Screen,
    val testTag: String,
    val iconSize: Dp = 25.dp
) {

  object Main :
      Tab(
          "Home",
          R.drawable.home_planet,
          Screen.Main,
          NavigationTestTags.MAIN_TAB,
          iconSize = 25.dp)

  object ProjectList :
      Tab(
          "Select Project",
          // Credits:
          // https://www.flaticon.com/free-icon-font/list-music_10742896
          R.drawable.list_projects_unselected,
          Screen.ProjectList,
          NavigationTestTags.PROJECTLIST_TAB)

  object Search :
      Tab(
          "Search",
          // Credits:
          // https://www.flaticon.com/free-icon-font/search_3917754
          R.drawable.search_unselected,
          Screen.Search,
          NavigationTestTags.SEARCH_TAB)

  object Messages :
      Tab(
          "Messages",
          R.drawable.messageicon,
          Screen.SelectMessages,
          NavigationTestTags.MESSAGE_BUTTON)
}

private val tabs =
    listOf(
        Tab.Main,
        Tab.Search,
        Tab.ProjectList,
        Tab.Messages,
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
        modifier =
            Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
                .height(55.dp)
                .padding(horizontal = 7.dp),
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
                    tab.destination.route == screen.route
                  }
                }
            NavigationBarItem(
                modifier = Modifier.size(25.dp).testTag(tab.testTag),
                icon = {
                  Icon(
                      painter = painterResource(id = tab.icon),
                      contentDescription = tab.name,
                      modifier = Modifier.size(tab.iconSize),
                      tint = NepTuneTheme.colors.onBackground)
                },
                alwaysShowLabel = false,
                selected = isSelected,
                onClick = {
                  if (tab.destination == Screen.ProjectList) {
                    navigationActions?.navigateTo(Screen.ProjectList.createRoute("edit"))
                  } else {
                    navigationActions?.navigateTo(tab.destination)
                  }
                },
                enabled = navigationActions != null,
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
