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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.resources.C
import com.neptune.neptune.ui.authentification.SignInScreen
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.ui.feed.FeedScreen
import com.neptune.neptune.ui.feed.FeedType
import com.neptune.neptune.ui.follow.FollowListRoute
import com.neptune.neptune.ui.follow.FollowListTab
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.messages.MessagesRoute
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
import com.neptune.neptune.ui.projectlist.ProjectListViewModel
import com.neptune.neptune.ui.projectlist.ProjectListViewModelFactory
import com.neptune.neptune.ui.sampler.SamplerScreen
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.search.SearchViewModel
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
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.SignIn.route,
) {
  val signInViewModel: SignInViewModel = viewModel()
  val searchViewModel: SearchViewModel = viewModel()
  val mainViewModel: MainViewModel = viewModel()
  val importViewModel: ImportViewModel = viewModel(factory = importAppRoot())

  val navigationActions = NavigationActions(navController)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val currentScreen = navigationActions.currentScreen(currentRoute ?: startDestination)

  // Media Player values
  val mediaPlayer = remember { NeptuneMediaPlayer() }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        mediaPlayer.stopWithFade(300L)
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  DisposableEffect(navController) {
    val listener =
        NavController.OnDestinationChangedListener { _, _, _ -> mediaPlayer.stopWithFade(300L) }
    navController.addOnDestinationChangedListener(listener)

    onDispose { navController.removeOnDestinationChangedListener(listener) }
  }

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
                authGraph(signInViewModel, mainViewModel, navigationActions)
                mainContentGraph(mainViewModel, searchViewModel, navigationActions)
                profileGraph(navigationActions)
                editorGraph(mainViewModel, importViewModel, navigationActions)
                settingsGraph(settingsViewModel, signInViewModel, navigationActions)
                messagingGraph(signInViewModel, navigationActions)
              }
        })
  }
}

// --- Navigation Graph Extensions ---

private fun NavGraphBuilder.authGraph(
    signInViewModel: SignInViewModel,
    mainViewModel: MainViewModel,
    nav: NavigationActions
) {
  composable(Screen.SignIn.route) {
    SignInScreen(
        signInViewModel = signInViewModel,
        navigateMain = {
          mainViewModel.refresh()
          nav.navigateTo(Screen.Main)
        })
  }
}

private fun NavGraphBuilder.mainContentGraph(
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    nav: NavigationActions
) {
  composable(Screen.Main.route) {
    MainScreen(
        navigateToProfile = { nav.navigateTo(Screen.Profile) },
        navigateToProjectList = { nav.navigateTo(Screen.ProjectList.createRoute("post")) },
        navigateToOtherUserProfile = { userId ->
          nav.navigateTo(Screen.OtherUserProfile.createRoute(userId))
        },
        navigateToSampleList = { type -> nav.navigateTo(Screen.Feed.createRoute(type)) },
        mainViewModel = mainViewModel)
  }

  composable(Screen.Search.route) {
    SearchScreen(
        navigateToProfile = { nav.navigateTo(Screen.Profile) },
        navigateToOtherUserProfile = { userId ->
          nav.navigateTo(Screen.OtherUserProfile.createRoute(userId))
        },
        searchViewModel = searchViewModel)
  }

  composable(
      route = Screen.Feed.route,
      arguments = listOf(navArgument("type") { type = NavType.StringType })) { backStackEntry ->
        val typeName = backStackEntry.arguments?.getString("type")
        val feedType =
            try {
              if (typeName != null) FeedType.valueOf(typeName) else FeedType.DISCOVER
            } catch (_: IllegalArgumentException) {
              FeedType.DISCOVER
            }
        FeedScreen(
            mainViewModel = mainViewModel,
            initialType = feedType,
            goBack = { nav.goBack() },
            navigateToProfile = { nav.navigateTo(Screen.Profile) },
            navigateToOtherUserProfile = { userId ->
              nav.navigateTo(Screen.OtherUserProfile.createRoute(userId))
            })
      }
}

private fun NavGraphBuilder.profileGraph(nav: NavigationActions) {
  composable(Screen.Profile.route) {
    SelfProfileRoute(
        settings = { nav.navigateTo(Screen.Settings) },
        goBack = { nav.goBack() },
        onFollowersClick = {
          nav.navigateTo(Screen.FollowList.createRoute(FollowListTab.FOLLOWERS))
        },
        onFollowingClick = {
          nav.navigateTo(Screen.FollowList.createRoute(FollowListTab.FOLLOWING))
        },
    )
  }

  composable(
      route = Screen.OtherUserProfile.route,
      arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        OtherUserProfileRoute(userId = userId, goBack = { nav.goBack() })
      }

  composable(
      route = Screen.FollowList.route,
      arguments = listOf(navArgument("initialTab") { type = NavType.StringType })) { backStackEntry
        ->
        val tabName = backStackEntry.arguments?.getString("initialTab")
        val initialTab =
            try {
              if (tabName != null) FollowListTab.valueOf(tabName) else FollowListTab.FOLLOWERS
            } catch (_: IllegalArgumentException) {
              FollowListTab.FOLLOWERS
            }
        FollowListRoute(
            initialTab = initialTab,
            goBack = { nav.goBack() },
            navigateToOtherUserProfile = { userId ->
              nav.navigateTo(Screen.OtherUserProfile.createRoute(userId))
            })
      }
}

private fun NavGraphBuilder.editorGraph(
    mainViewModel: MainViewModel,
    importViewModel: ImportViewModel,
    nav: NavigationActions
) {
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
            goBack = { nav.goBack() },
            navigateToMainScreen = {
              mainViewModel.refresh()
              nav.navigateTo(Screen.Main)
            },
            projectId = projectId)
      }

  composable(Screen.ImportFile.route) {
    ImportScreen(vm = importViewModel, goBack = { nav.goBack() })
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
        val db = remember {
          Room.databaseBuilder(NepTuneApplication.appContext, MediaDb::class.java, "media.db")
              .build()
        }
        val mediaRepo = remember { MediaRepositoryImpl(db.mediaDao()) }
        val getLibraryUseCase = remember { GetLibraryUseCase(mediaRepo) }
        val vm: ProjectListViewModel =
            viewModel(
                factory =
                    ProjectListViewModelFactory(
                        getLibraryUseCase = getLibraryUseCase, mediaRepository = mediaRepo))
        ProjectListScreen(
            projectListViewModel = vm,
            onProjectClick = { projectItem ->
              if (purpose == "post") {
                nav.navigateTo(Screen.Post.createRoute(projectItem.uid))
              } else {
                val pathToSend = projectItem.projectFileLocalPath ?: projectItem.uid
                val encodedFilePath = URLEncoder.encode(pathToSend, StandardCharsets.UTF_8.name())
                nav.navigateTo(Screen.Edit.route + "/$encodedFilePath")
              }
            },
            importViewModel = importViewModel)
      }
}

private fun NavGraphBuilder.settingsGraph(
    settingsViewModel: SettingsViewModel,
    signInViewModel: SignInViewModel,
    nav: NavigationActions
) {
  composable(Screen.Settings.route) {
    SettingsScreen(
        goBack = { nav.goBack() },
        goTheme = { nav.navigateTo(Screen.SettingsTheme) },
        goAccount = { nav.navigateTo(Screen.SettingsAccount) })
  }
  composable(Screen.SettingsTheme.route) {
    SettingsThemeScreen(
        settingsViewModel = settingsViewModel,
        goBack = { nav.goBack() },
        goCustomTheme = { nav.navigateTo(Screen.SettingsCustomTheme) })
  }
  composable(Screen.SettingsCustomTheme.route) {
    SettingsCustomThemeScreen(settingsViewModel = settingsViewModel, goBack = { nav.goBack() })
  }
  composable(Screen.SettingsAccount.route) {
    SettingsAccountScreen(
        goBack = { nav.goBack() },
        logout = {
          signInViewModel.signOut()
          nav.navigateTo(Screen.SignIn)
        })
  }
}

private fun NavGraphBuilder.messagingGraph(
    signInViewModel: SignInViewModel,
    nav: NavigationActions
) {
  composable(Screen.SelectMessages.route) {
    val firebaseUser by signInViewModel.currentUser.collectAsState()
    val currentUid = firebaseUser?.uid ?: return@composable // prevent crash
    SelectMessagesScreen(
        goBack = { nav.goBack() },
        onSelectUser = { uid -> nav.navigateTo(Screen.Messages.createRoute(uid)) },
        currentUid = currentUid)
  }
  composable(
      route = Screen.Messages.route,
      arguments = listOf(navArgument("uid") { type = NavType.StringType })) { backStackEntry ->
        val otherUserId = backStackEntry.arguments?.getString("uid") ?: return@composable
        MessagesRoute(
            otherUserId = otherUserId, signInViewModel = signInViewModel, goBack = { nav.goBack() })
      }
}
