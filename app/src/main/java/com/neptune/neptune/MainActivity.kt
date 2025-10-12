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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neptune.neptune.resources.C
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.mock.MockEditScreen
import com.neptune.neptune.ui.mock.MockPostScreen
import com.neptune.neptune.ui.mock.MockProfileScreen
import com.neptune.neptune.ui.mock.MockSearchScreen
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationActions
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.navigation.TopBar
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      SampleAppTheme {
        // A surface container using the 'background' color from the theme
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
  val navigationActions = NavigationActions(navController)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val currentScreen = navigationActions.currentScreen(currentRoute ?: startDestination)
  Scaffold(
      bottomBar = {
        BottomNavigationMenu(navigationActions = navigationActions, screen = currentScreen)
      },
      topBar = {
        TopBar(
            currentScreen = currentScreen,
            navigationActions = navigationActions,
            canNavigateBack = currentScreen.showBackButton)
      },
      containerColor = DarkBlue1,
      content = { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {
              // TODO: Replace mock screens with actual app screens
              composable(Screen.Main.route) { MainScreen() }
              composable(Screen.Profile.route) { MockProfileScreen() }
              composable(Screen.Edit.route) { SamplerScreen() }
              composable(Screen.Search.route) { MockSearchScreen() }
              composable(Screen.Post.route) { MockPostScreen() }
              composable(Screen.SignIn.route) {
                MockProfileScreen() /* SignInScreen(navigationActions) */
              }
            }
      })
}
