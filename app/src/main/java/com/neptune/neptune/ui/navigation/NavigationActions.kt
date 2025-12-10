package com.neptune.neptune.ui.navigation

import androidx.navigation.NavHostController
import com.neptune.neptune.ui.feed.FeedType
import com.neptune.neptune.ui.follow.FollowListTab

/**
 * Screens used in the app. Each screen is a destination in the navigation graph. Bottom bar and
 * profile icon are shown by default, back button is hidden by default.
 *
 * @param route The route of the screen
 * @param showBottomBar Whether to show the bottom navigation bar
 */
sealed class Screen(val route: String, val showBottomBar: Boolean = true) {
  object Main : Screen(route = "main")

  object Edit : Screen("edit_screen/{zipFilePath}") {
    fun createRoute(encodedZipFilePath: String): String {
      return "edit_screen/$encodedZipFilePath"
    }
  }

  object Feed : Screen("feed/{type}") {
    fun createRoute(type: FeedType): String = "feed/${type.name}"
  }

  object Search : Screen(route = "search")

  object Post : Screen(route = "post/{projectId}") {
    fun createRoute(projectId: String): String = "post/$projectId"
  }

  object ProjectList : Screen(route = "project_list/{purpose}") {
    fun createRoute(purpose: String): String = "project_list/$purpose"
  }

  object Profile : Screen(route = "profile", showBottomBar = false)

  object OtherUserProfile : Screen("other_user_profile/{userId}", showBottomBar = false) {
    fun createRoute(userId: String) = "other_user_profile/$userId"
  }

  object Messages : Screen("messages/{uid}", showBottomBar = false) {
    fun createRoute(uid: String) = "messages/$uid"
  }

  object SignIn : Screen(route = "signIn", showBottomBar = false)

  object Settings : Screen(route = "setting", showBottomBar = false)

  object SettingsTheme : Screen(route = "settings_theme", showBottomBar = false)

  object SettingsAccount : Screen(route = "settings_account", showBottomBar = false)

  object SettingsCustomTheme : Screen(route = "settings_custom_theme", showBottomBar = false)

  object ImportFile : Screen(route = "import_file")

  object SelectMessages : Screen(route = "select_messages", showBottomBar = false)

  object FollowList : Screen(route = "follow_list/{initialTab}", showBottomBar = false) {
    fun createRoute(initialTab: FollowListTab): String = "follow_list/${initialTab.name}"
  }
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
    return when {
      route == null -> Screen.SignIn
      route.startsWith("follow_list/") -> Screen.FollowList
      route.startsWith("edit_screen/") -> Screen.Edit
      route.startsWith("post/") -> Screen.Post
      route.startsWith("project_list/") -> Screen.ProjectList
      route.startsWith("messages/") -> Screen.Messages
      route == Screen.Main.route -> Screen.Main
      route == Screen.Profile.route -> Screen.Profile
      route == Screen.Search.route -> Screen.Search
      route == Screen.SignIn.route -> Screen.SignIn
      route == Screen.Settings.route -> Screen.Settings
      route == Screen.SettingsTheme.route -> Screen.SettingsTheme
      route == Screen.SettingsCustomTheme.route -> Screen.SettingsCustomTheme
      route == Screen.SettingsAccount.route -> Screen.SettingsAccount
      route == Screen.ImportFile.route -> Screen.ImportFile
      route == Screen.OtherUserProfile.route -> Screen.OtherUserProfile
      route == Screen.SelectMessages.route -> Screen.SelectMessages
      else -> Screen.SignIn
    }
  }
  /**
   * Navigate to a specific screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    val current = currentRoute()
    if (current == screen.route) {
      return
    }
    if (screen.route == Screen.SignIn.route ||
        (current == Screen.SignIn.route && screen.route == Screen.Main.route)) {
      navController.navigate(screen.route) { popUpTo(navController.graph.id) { inclusive = true } }
      return
    }
    if (screen.route == Screen.Main.route) {
      val success = navController.popBackStack(Screen.Main.route, inclusive = false)
      if (!success) {
        navController.navigate(screen.route) { launchSingleTop = true }
      }
      return
    }
    navController.navigate(screen.route) {
      launchSingleTop = true
      restoreState = true
    }
  }

  open fun navigateTo(route: String) {
    if (currentRoute() != route) {
      navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
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
