package com.neptune.neptune

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
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.onClickFunctions
import com.neptune.neptune.ui.mock.MockImportScreen
import com.neptune.neptune.ui.mock.MockProfileScreen
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationActions
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.importAppRoot
import com.neptune.neptune.ui.post.PostScreen
import com.neptune.neptune.ui.profile.ProfileRoute
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.settings.SettingsAccountScreen
import com.neptune.neptune.ui.settings.SettingsScreen
import com.neptune.neptune.ui.settings.SettingsThemeScreen
import com.neptune.neptune.ui.settings.SettingsViewModel
import com.neptune.neptune.ui.settings.SettingsViewModelFactory
import com.neptune.neptune.ui.settings.ThemeDataStore
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.theme.SampleAppTheme

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

      // Collect the current theme setting (SYSTEM, LIGHT, or DARK) as a Composable state.
      val themeSetting by settingsViewModel.theme.collectAsState()
      // A surface container using the 'background' color from the theme
      SampleAppTheme(themeSetting = themeSetting) {
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
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
  val navigationActions = NavigationActions(navController)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val importViewModel: ImportViewModel = viewModel(factory = importAppRoot())
  val currentScreen = navigationActions.currentScreen(currentRoute ?: startDestination)

  // Media Player values
  val mediaPlayer = remember { NeptuneMediaPlayer() }

  CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
    Scaffold(
        bottomBar = {
          BottomNavigationMenu(navigationActions = navigationActions, screen = currentScreen)
        },
        containerColor = NepTuneTheme.colors.background,
        content = { innerPadding ->
          NavHost(
              navController = navController,
              startDestination = startDestination,
              modifier = Modifier.padding(innerPadding)) {
                // TODO: Replace mock screens with actual app screens
                composable(Screen.Main.route) {
                  MainScreen(
                      navigateToProfile = { navigationActions.navigateTo(Screen.Profile) },
                      // TODO: Change back to ProjectList when navigation from
                      // Main->ProjectList->PostScreen is implemented
                      navigateToProjectList = { navigationActions.navigateTo(Screen.Post) })
                }
                composable(Screen.Profile.route) {
                  ProfileRoute(
                      settings = { navigationActions.navigateTo(Screen.Settings) },
                      goBack = { navigationActions.goBack() })
                }
                composable(
                    route = Screen.Edit.route,
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
                  SearchScreen()
                }
                composable(Screen.Post.route) {
                  PostScreen(
                      goBack = { navigationActions.goBack() },
                      navigateToMainScreen = { navigationActions.navigateTo(Screen.Main) })
                }
                composable(Screen.ImportFile.route) { MockImportScreen(importViewModel) }
                composable(Screen.SignIn.route) {
                  SignInScreen(
                      signInViewModel = signInViewModel,
                      navigateMain = { navigationActions.navigateTo(Screen.Main) })
                }
                composable(Screen.ProjectList.route) {
                  ProjectListScreen(
                      navigateToSampler = { filePath ->
                        navigationActions.navigateTo(Screen.Edit.createRoute(filePath))
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
                composable(Screen.OtherUserProfile.route) { MockProfileScreen() }
              }
        })
  }
}
