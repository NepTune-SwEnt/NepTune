package com.neptune.neptune

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.resources.C
import com.neptune.neptune.ui.authentification.SignInScreen
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.ui.feed.FeedScreen
import com.neptune.neptune.ui.feed.FeedType
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.main.factory
import com.neptune.neptune.ui.messages.MessagesScreen
import com.neptune.neptune.ui.messages.SelectMessagesScreen
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationActions
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.picker.ImportScreen
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.importAppRoot
import com.neptune.neptune.ui.post.PostScreen
import com.neptune.neptune.ui.profile.OtherUserProfileRoute
import com.neptune.neptune.ui.profile.SelfProfileRoute
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.search.SearchViewModel
import com.neptune.neptune.ui.search.searchScreenFactory
import com.neptune.neptune.ui.settings.SettingsAccountScreen
import com.neptune.neptune.ui.settings.SettingsCustomThemeScreen
import com.neptune.neptune.ui.settings.SettingsScreen
import com.neptune.neptune.ui.settings.SettingsThemeScreen
import com.neptune.neptune.ui.settings.SettingsViewModel
import com.neptune.neptune.ui.settings.SettingsViewModelFactory
import com.neptune.neptune.ui.settings.ThemeDataStore
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.theme.SampleAppTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

  // A handle to the DataStore instance that manages theme persistence.
  private lateinit var themeDataStore: ThemeDataStore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the ThemeDataStore using the application-level context
    themeDataStore = ThemeDataStore(applicationContext)

    setContent {
      // Create the factory required to manually inject the themeDataStore
      // into the SettingsViewModel.
      val settingsViewModelFactory = SettingsViewModelFactory(themeDataStore)
      // Get a reference to the SettingsViewModel, providing our custom factory
      // so the ViewModel instance receives the DataStore dependency.
      val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)

      // Collect the current theme setting and persisted custom Color values
      val themeSetting by settingsViewModel.theme.collectAsState()
      val customPrimary by settingsViewModel.customPrimaryColor.collectAsState()
      val customBackground by settingsViewModel.customBackgroundColor.collectAsState()
      val customOnBackground by settingsViewModel.customOnBackgroundColor.collectAsState()

      // A surface container using the 'background' color from the theme
      SampleAppTheme(
          themeSetting = themeSetting,
          customPrimary = customPrimary,
          customBackground = customBackground,
          customOnBackground = customOnBackground) {
            Surface(
                modifier =
                    Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
                color = MaterialTheme.colorScheme.background) {
                  NeptuneApp(settingsViewModel = settingsViewModel)
                }
          }
    }
  }
}

@Composable
fun NeptuneApp(
    settingsViewModel: SettingsViewModel =
        SettingsViewModel(ThemeDataStore(LocalContext.current.applicationContext)),
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SignIn.route,
) {
  val signInViewModel: SignInViewModel = viewModel()
  val searchViewModel: SearchViewModel =
      viewModel(
          factory = searchScreenFactory(LocalContext.current.applicationContext as Application))
  val navigationActions = NavigationActions(navController)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val importViewModel: ImportViewModel = viewModel(factory = importAppRoot())
  val currentScreen = navigationActions.currentScreen(currentRoute ?: startDestination)

  val mainViewModel: MainViewModel =
      viewModel(factory = factory(LocalContext.current.applicationContext as Application))

  // Media Player values
  val mediaPlayer = remember { NeptuneMediaPlayer() }

  CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
    Scaffold(
        bottomBar = {
          BottomNavigationMenu(
              navigationActions = navigationActions,
              screen = currentScreen,
              currentScreenArguments = navBackStackEntry?.arguments)
        },
        containerColor = NepTuneTheme.colors.background,
        content = { innerPadding ->
          NavHost(
              navController = navController,
              startDestination = startDestination,
              modifier = Modifier.padding(innerPadding)) {
                composable(Screen.Main.route) {
                  MainScreen(
                      navigateToProfile = { navigationActions.navigateTo(Screen.Profile) },
                      navigateToProjectList = {
                        navigationActions.navigateTo(Screen.ProjectList.createRoute("post"))
                      },
                      navigateToOtherUserProfile = { userId ->
                        navigationActions.navigateTo(Screen.OtherUserProfile.createRoute(userId))
                      },
                      navigateToSelectMessages = {
                        navigationActions.navigateTo(Screen.SelectMessages)
                      },
                      navigateToSampleList = { type ->
                        navigationActions.navigateTo(Screen.Feed.createRoute(type))
                      },
                      mainViewModel = mainViewModel)
                }
                composable(Screen.Profile.route) {
                  SelfProfileRoute(
                      settings = { navigationActions.navigateTo(Screen.Settings) },
                      goBack = { navigationActions.goBack() })
                }
                composable(
                    route = Screen.Edit.route + "/{zipFilePath}",
                    arguments =
                        listOf(
                            navArgument("zipFilePath") {
                              type = NavType.StringType
                              nullable = true
                            })) { backStackEntry ->
                      val zipFilePath = backStackEntry.arguments?.getString("zipFilePath")
                      SamplerScreen(zipFilePath = zipFilePath)
                    }

                composable(Screen.Search.route) {
                  SearchScreen(
                      navigateToProfile = { navigationActions.navigateTo(Screen.Profile) },
                      navigateToOtherUserProfile = { userId ->
                        navigationActions.navigateTo(Screen.OtherUserProfile.createRoute(userId))
                      },
                      searchViewModel = searchViewModel)
                }
                composable(
                    route = Screen.Post.route,
                    arguments =
                        listOf(
                            navArgument("projectId") {
                              type = NavType.StringType
                              nullable = true
                            })) { backStackEntry ->
                      val projectId = backStackEntry.arguments?.getString("projectId")

                      PostScreen(
                          goBack = { navigationActions.goBack() },
                          navigateToMainScreen = {
                            mainViewModel.refresh()
                            navigationActions.navigateTo(Screen.Main)
                          },
                          projectId = projectId)
                    }
                composable(Screen.ImportFile.route) {
                  ImportScreen(vm = importViewModel, goBack = { navigationActions.goBack() })
                }
                composable(Screen.SignIn.route) {
                  SignInScreen(
                      signInViewModel = signInViewModel,
                      navigateMain = {
                        mainViewModel.refresh()
                        navigationActions.navigateTo(Screen.Main)
                      })
                }
                composable(
                    route = Screen.ProjectList.route,
                    arguments =
                        listOf(
                            navArgument("purpose") {
                              type = NavType.StringType
                              defaultValue = "edit"
                            })) { backStackEntry ->
                      val purpose = backStackEntry.arguments?.getString("purpose") ?: "edit"
                      ProjectListScreen(
                          onProjectClick = { projectItem ->
                            when (purpose) {
                              "post" -> {
                                navigationActions.navigateTo(
                                    Screen.Post.createRoute(projectItem.uid))
                              }
                              else -> {
                                val pathToSend = projectItem.projectFileLocalPath ?: projectItem.uid
                                val encodedFilePath =
                                    URLEncoder.encode(pathToSend, StandardCharsets.UTF_8.name())
                                navigationActions.navigateTo(
                                    Screen.Edit.route + "/$encodedFilePath")
                              }
                            }
                          })
                    }
                composable(Screen.Settings.route) {
                  SettingsScreen(
                      goBack = { navigationActions.goBack() },
                      goTheme = { navigationActions.navigateTo(Screen.SettingsTheme) },
                      goAccount = { navigationActions.navigateTo(Screen.SettingsAccount) })
                }
                composable(Screen.SettingsTheme.route) {
                  SettingsThemeScreen(
                      settingsViewModel = settingsViewModel,
                      goBack = { navigationActions.goBack() },
                      goCustomTheme = { navigationActions.navigateTo(Screen.SettingsCustomTheme) })
                }
                composable(Screen.SettingsCustomTheme.route) {
                  SettingsCustomThemeScreen(
                      settingsViewModel = settingsViewModel,
                      goBack = { navigationActions.goBack() })
                }
                composable(Screen.SettingsAccount.route) {
                  SettingsAccountScreen(
                      goBack = { navigationActions.goBack() },
                      logout = {
                        signInViewModel.signOut()
                        navigationActions.navigateTo(Screen.SignIn)
                      })
                }
                composable(
                    route = Screen.OtherUserProfile.route,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })) {
                        backStackEntry ->
                      val userId =
                          backStackEntry.arguments?.getString("userId") ?: return@composable

                      OtherUserProfileRoute(
                          userId = userId,
                          goBack = { navigationActions.goBack() },
                      )
                    }
                composable(Screen.SelectMessages.route) {
                  SelectMessagesScreen(
                      goBack = { navigationActions.goBack() },
                      onSelectUser = { uid ->
                        navigationActions.navigateTo(Screen.Messages.createRoute(uid))
                      })
                }
                composable(
                    route = Screen.Messages.route,
                    arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
                        backStackEntry ->
                      val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                      MessagesScreen(uid = uid, goBack = { navigationActions.goBack() })
                    }
                composable(
                    route = Screen.Feed.route,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })) {
                        backStackEntry ->
                      val typeName = backStackEntry.arguments?.getString("type")
                      val feedType =
                          try {
                            if (typeName != null) FeedType.valueOf(typeName) else FeedType.DISCOVER
                          } catch (_: IllegalArgumentException) {
                            FeedType.DISCOVER // default value
                          }
                      FeedScreen(
                          mainViewModel = mainViewModel,
                          initialType = feedType,
                          goBack = { navigationActions.goBack() },
                          navigateToProfile = { navigationActions.navigateTo(Screen.Profile) },
                          navigateToOtherUserProfile = { userId ->
                            navigationActions.navigateTo(
                                Screen.OtherUserProfile.createRoute(userId))
                          })
                    }
              }
        })
  }
}
