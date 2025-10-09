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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neptune.neptune.resources.C
import com.neptune.neptune.ui.mock.MockEditScreen
import com.neptune.neptune.ui.mock.MockMainScreen
import com.neptune.neptune.ui.mock.MockProfileScreen
import com.neptune.neptune.ui.navigation.BottomNavigationMenu
import com.neptune.neptune.ui.navigation.NavigationActions
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.navigation.TopBar
import com.neptune.neptune.ui.navigation.getTabForRoute
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

private val startDestination = Screen.Main.route

@Preview
@Composable
fun NeptuneApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
  val navigationActions = NavigationActions(navController)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val selectedTab = getTabForRoute(currentRoute)
  Scaffold(
      modifier = modifier,
      bottomBar = {
        BottomNavigationMenu(
            modifier = modifier, navigationActions = navigationActions, selectedTab = selectedTab)
      },
      topBar = { TopBar(modifier = modifier, currentScreen = navigationActions.currentScreen, navigationActions = navigationActions, canNavigateBack = navigationActions.currentScreen.showBackButton) },
      content = { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {
            // TODO: Replace mock screens with actual app screens
              composable(Screen.Main.route) { MockMainScreen() }
              composable(Screen.Profile.route) { MockProfileScreen() }
              composable(Screen.Edit.route) { MockEditScreen() }
            }
      })
}
