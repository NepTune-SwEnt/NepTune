package com.neptune.neptune.ui.navigation

import androidx.navigation.NavHostController

/**
 * Screens used in the app. Each screen is a destination in the navigation graph.
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
      Screen(route = "profile", name = "My Profile", showBackButton = true, showBottomBar = false)

  object SignIn :
      Screen(route = "sign_in", name = "Neptune", showBottomBar = false, showProfile = false)
}

open class NavigationActions(
    private val navController: NavHostController,
) {

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
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    // If the user is already on the destination, do nothing
    if (currentRoute() == screen.route) {
      return
    }
    navController.navigate(screen.route) { restoreState = true }
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
