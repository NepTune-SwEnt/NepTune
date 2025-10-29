package com.neptune.neptune

import android.content.Context
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
import com.google.firebase.Timestamp
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import com.neptune.neptune.model.project.ProjectItemsRepositoryVar
import com.neptune.neptune.resources.C
import com.neptune.neptune.ui.authentification.SignInScreen
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.mock.MockImportScreen
import com.neptune.neptune.ui.mock.MockSearchScreen
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationActions
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.picker.ImportViewModel
import com.neptune.neptune.ui.picker.importAppRoot
import com.neptune.neptune.ui.profile.ProfileRoute
import com.neptune.neptune.ui.projectlist.ProjectListScreen
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

private const val ASSET_ZIP_PATH = "fakeProject.zip"
private const val TARGET_PROJECT_ID = "42"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SampleAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background) {
                NeptuneApp()
            }
      }
    }
  }
}


@Composable
fun NeptuneApp(
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
  val context = LocalContext.current.applicationContext
  val mediaPlayer = remember { NeptuneMediaPlayer(context) }

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
                  MainScreen(navigateToProfile = { navigationActions.navigateTo(Screen.Profile) })
                }
                composable(Screen.Profile.route) {
                  ProfileRoute(
                      logout = {
                        signInViewModel.signOut()
                        navigationActions.navigateTo(Screen.SignIn)
                      },
                      goBack = { navigationActions.goBack() })
                }
                composable(
                    route = Screen.Edit.route,
                    arguments = listOf(
                        navArgument("zipFilePath") { type = NavType.StringType; nullable = true }
                    )
                  ) { backStackEntry ->
                      val zipFilePath = backStackEntry.arguments?.getString("zipFilePath")
                      SamplerScreen(zipFilePath = zipFilePath)
                  }
                composable(Screen.Search.route) { MockSearchScreen() }
                composable(Screen.ImportFile.route) { MockImportScreen(importViewModel) }
                composable(Screen.SignIn.route) {
                  SignInScreen(
                      signInViewModel = signInViewModel,
                      navigateMain = { navigationActions.navigateTo(Screen.Main) })
                }
              composable(Screen.ProjectList.route) {
                  ProjectListScreen(
                      navigateToSampler = { filePath -> navigationActions.navigateTo(Screen.Edit.createRoute(filePath)) }
                  )
              }
              }
        })
  }
}
