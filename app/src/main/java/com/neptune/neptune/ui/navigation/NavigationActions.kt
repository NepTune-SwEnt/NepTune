package com.neptune.neptune.ui.navigation

import androidx.navigation.NavHostController

/**
 * Screens used in the app. Each screen is a destination in the navigation graph. Bottom bar and
 * profile icon are shown by default, back button is hidden by default.
 *
 * @param route The route of the screen
 * @param showBottomBar Whether to show the bottom navigation bar
 */
sealed class Screen(val route: String, val showBottomBar: Boolean = true) {
  object Main : Screen(route = "main")

  object Edit : Screen(route = "edit")

  object Search : Screen(route = "search")

  object Post : Screen(route = "post")

  object ProjectList : Screen(route = "project_list")

  object Profile : Screen(route = "profile", showBottomBar = false)

  object SignIn :
      Screen(route = "signIn", name = "Neptune", showBottomBar = false, showBackButton = false)

  object ImportFile :
      Screen(
          route = "import_file",
          name = "Import Project",
          showBottomBar = false,
          showBackButton = true)
}

/**
 * Class that handles navigation actions in the app.
 *
 * @param navController The NavHostController used for navigation
 */
open class NavigationActions(
    private val navController: NavHostController,
) {

  /**
   * Get the current screen based on the route.
   *
   * @param route The current route
   * @return The current screen
   */
  fun currentScreen(route: String?): Screen {
    return when (route) {
      Screen.Main.route -> Screen.Main
      Screen.Edit.route -> Screen.Edit
      Screen.Profile.route -> Screen.Profile
      Screen.Search.route -> Screen.Search
      Screen.Post.route -> Screen.Post
      Screen.SignIn.route -> Screen.SignIn
      Screen.ProjectList.route -> Screen.ProjectList
      Screen.ImportFile.route -> Screen.ImportFile
      else -> Screen.SignIn
    }
  }
  /**
   * Navigate to a specific screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    // If the user is already on the destination, do nothing
    if (currentRoute() != screen.route) {
      navController.navigate(screen.route) {
        if (screen.route == Screen.Main.route || screen.route == Screen.SignIn.route) {
          popUpTo(navController.graph.id) { inclusive = true }
        }
        launchSingleTop = true
        restoreState = screen.route != Screen.SignIn.route
      }
    }
  }

  /** Navigate back to the previous screen in the back stack. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
