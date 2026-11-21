package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.VaultAudioEngine
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.GameContainerScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.VaultStudioScreen
import com.example.ui.theme.VaultPlayTheme
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

const val ROUTE_LIBRARY = "library"
const val ROUTE_ACHIEVEMENTS = "achievements"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_STUDIO = "studio"
const val ROUTE_PLAYER = "player/{gameId}"

private data class NavTab(
  val route: String,
  val labelRes: Int,
  val activeIcon: ImageVector,
  val inactiveIcon: ImageVector,
  val tag: String,
)

private val NAV_TABS =
  listOf(
    NavTab(ROUTE_LIBRARY, R.string.nav_library, Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports, "nav_library"),
    NavTab(ROUTE_ACHIEVEMENTS, R.string.nav_badges, Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_badges"),
    NavTab(ROUTE_LEADERBOARD, R.string.nav_ranks, Icons.Rounded.Leaderboard, Icons.Outlined.Leaderboard, "nav_ranks"),
    NavTab(ROUTE_STUDIO, R.string.nav_studio, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_studio"),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: VaultViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      VaultPlayTheme(
        darkTheme = uiState.preferences.isDarkMode,
        themePreset = uiState.preferences.themePreset,
      ) {
        VaultPlayApp(viewModel = viewModel, uiState = uiState)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPlayApp(
  viewModel: VaultViewModel,
  uiState: com.example.ui.viewmodel.VaultUiState,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_LIBRARY
  val isInsidePlayer = currentRoute.startsWith("player/")
  val activeGameId = if (isInsidePlayer) backStackEntry?.arguments?.getString("gameId") ?: "neon_hop" else null
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> VaultAudioEngine.onAppForegroundChanged(true)
        Lifecycle.Event.ON_STOP -> VaultAudioEngine.onAppForegroundChanged(false)
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(activeGameId, uiState.preferences.musicEnabled, uiState.preferences.lobbySfxEnabled) {
    VaultAudioEngine.syncSettings(
      musicOn = uiState.preferences.musicEnabled,
      lobbySfxOn = uiState.preferences.lobbySfxEnabled,
      isLobbyScreen = !isInsidePlayer,
    )
    VaultAudioEngine.setTrack(activeGameId ?: "lobby")
  }

  LaunchedEffect(uiState.statusBanner) {
    val msg = uiState.statusBanner
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  LaunchedEffect(uiState.latestReward) {
    if (uiState.latestReward != null) {
      delay(3200L)
      viewModel.clearLatestReward()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        if (!isInsidePlayer) {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.headlineSmall,
                )
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(999.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      Icons.Rounded.Flight,
                      contentDescription = "Offline travel ready",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = "OFFLINE",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            },
            actions = {
              IconButton(
                onClick = { VaultAudioEngine.playLobbyButtonSound(); viewModel.toggleDarkMode() },
                modifier = Modifier.testTag("top_theme_toggle"),
              ) {
                Icon(
                  imageVector =
                    if (uiState.preferences.isDarkMode) Icons.Rounded.LightMode
                    else Icons.Rounded.DarkMode,
                  contentDescription = stringResource(R.string.toggle_theme_cd),
                )
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
              ),
          )
        }
      },
      bottomBar = {
        if (!isInsidePlayer && !useNavigationRail) {
          NavigationBar {
            NAV_TABS.forEach { tab ->
              val selected = currentRoute == tab.route
              val label = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = {
                  VaultAudioEngine.playLobbyButtonSound()
                  if (currentRoute != tab.route) {
                    navController.navigate(tab.route) {
                      popUpTo(ROUTE_LIBRARY) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.tag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(if (isInsidePlayer) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
      ) {
        if (!isInsidePlayer && useNavigationRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.size(12.dp))
            NAV_TABS.forEach { tab ->
              val selected = currentRoute == tab.route
              val label = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = {
                  VaultAudioEngine.playLobbyButtonSound()
                  if (currentRoute != tab.route) {
                    navController.navigate(tab.route) {
                      popUpTo(ROUTE_LIBRARY) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.tag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
          val maxContentWidth = if (isInsidePlayer) 2000.dp else 1080.dp
          Box(modifier = Modifier.fillMaxSize().widthIn(max = maxContentWidth)) {
            NavHost(
              navController = navController,
              startDestination = ROUTE_LIBRARY,
              modifier = Modifier.fillMaxSize(),
            ) {
              composable(ROUTE_LIBRARY) {
                LibraryScreen(
                  uiState = uiState,
                  onFilterSelected = viewModel::setFilter,
                  onSearchChanged = viewModel::setSearchQuery,
                  onLaunchGame = { game ->
                    viewModel.onGameLaunched(game.id)
                    navController.navigate("player/${game.id}")
                  },
                )
              }
              composable(ROUTE_ACHIEVEMENTS) {
                AchievementsScreen(uiState = uiState)
              }
              composable(ROUTE_LEADERBOARD) {
                LeaderboardScreen(
                  uiState = uiState,
                  onSwitchProfile = viewModel::switchProfile,
                  onCreateProfile = viewModel::createProfile,
                )
              }
              composable(ROUTE_STUDIO) {
                VaultStudioScreen(
                  uiState = uiState,
                  onToggleDarkMode = viewModel::toggleDarkMode,
                  onSelectTheme = viewModel::selectThemePreset,
                  onToggleMusic = viewModel::toggleMusicEnabled,
                  onToggleLobbySfx = viewModel::toggleLobbySfxEnabled,
                  onUpdateControls = viewModel::updateControlConfig,
                  onUnlockCreator = viewModel::unlockCreatorMode,
                  onLockCreator = viewModel::lockCreatorMode,
                  onInspectPackage = viewModel::inspectPackageUri,
                  onClearInspected = viewModel::clearInspectedPackage,
                  onAddCreatorGame = viewModel::addCreatorGame,
                  onRemoveCreatorGame = viewModel::removeCreatorGame,
                  onBuildBackupJson = viewModel::buildOfflineBackupJson,
                  onImportBackupJson = viewModel::importOfflineBackupJson,
                )
              }
              composable(
                route = ROUTE_PLAYER,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
              ) { entry ->
                val gameId = entry.arguments?.getString("gameId") ?: "neon_hop"
                val game = uiState.games.firstOrNull { it.id == gameId }
                if (game != null) {
                  GameContainerScreen(
                    game = game,
                    uiState = uiState,
                    onAwardProgress = viewModel::awardGameProgress,
                    onUpgradeFighter = viewModel::upgradeFighterStat,
                    onFlipHud = {
                      viewModel.updateControlConfig { it.copy(leftHandedHud = !it.leftHandedHud) }
                    },
                    onExitGame = { navController.popBackStack() },
                  )
                }
              }
            }
          }

          // Floating Achievement / Tier Promotion Banner
          androidx.compose.animation.AnimatedVisibility(
            visible = uiState.latestReward != null && (uiState.latestReward.promotedToTier != null || uiState.latestReward.unlockedAchievementTitle != null),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 16.dp, end = 16.dp),
          ) {
            uiState.latestReward?.let { reward ->
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                  )
                  val msg =
                    when {
                      reward.promotedToTier != null ->
                        "RANK UP: ${reward.promotedToTier.displayName}! (+${reward.xpGained} XP)"
                      reward.unlockedAchievementTitle != null ->
                        "BADGE UNLOCKED: ${reward.unlockedAchievementTitle}"
                      else -> "+${reward.xpGained} XP"
                    }
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
