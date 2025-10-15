package com.neptune.neptune.ui.navigation

import androidx.navigation.NavHostController

/**
 * Screens used in the app. Each screen is a destination in the navigation graph. Bottom bar and
 * profile icon are shown by default, back button is hidden by default.
 *
 * @param route The route of the screen
 * @param name The name of the screen
 * @param showBottomBar Whether to show the bottom navigation bar
 * @param showBackButton Whether to show the back button in the top app bar
 * @param showProfile Whether to show the profile icon in the top app bar
 */
sealed class Screen(
    val route: String,
    val name: String,
    val showBottomBar: Boolean = true,
    val showProfile: Boolean = true,
    val showBackButton: Boolean = false,
) {
  object Main : Screen(route = "main", name = "Neptune")

  object Edit : Screen(route = "edit", name = "Edit")

  object Search : Screen(route = "search", name = "Search")

  object Post : Screen(route = "post", name = "Post")

  object Profile :
      Screen(route = "profile", name = "My Profile", showBottomBar = false, showBackButton = true)

  object SignIn :
      Screen(route = "signIn", name = "Neptune", showBottomBar = false, showBackButton = false)
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
          popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
        restoreState = true
        launchSingleTop = true
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
