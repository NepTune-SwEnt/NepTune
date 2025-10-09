package com.neptune.neptune.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val showBottomBar: Boolean = true,
    val showBackButton: Boolean = false,
) {

  object Main : Screen(route = "main", name = "Neptune", showBottomBar = true, showBackButton = false)

  object Edit : Screen(route = "edit", name = "Edit", showBottomBar = true, showBackButton = false)

  object Profile : Screen(route = "profile", name = "My Profile", showBottomBar = false, showBackButton = true)
}

open class NavigationActions(
    private val navController: NavHostController,
) {

  val currentScreen : Screen
    get() {
      return when (currentRoute()) {
        Screen.Main.route -> Screen.Main
        Screen.Edit.route -> Screen.Edit
        Screen.Profile.route -> Screen.Profile
        else -> Screen.Main
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
