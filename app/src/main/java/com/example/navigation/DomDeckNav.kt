package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.model.ConsoleCatalog
import com.example.ui.components.ConsoleTransitionOverlay
import com.example.ui.components.DomDeckAmbientBackdrop
import com.example.ui.components.GlassFloatingContainer
import com.example.ui.screens.ConsoleSetupScreen
import com.example.ui.screens.ConsolesScreen
import com.example.ui.screens.GameDetailsScreen
import com.example.ui.screens.GameLibraryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentMuted
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.viewmodel.DomDeckViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ConsolesRoute

@Serializable
data object SettingsRoute

@Serializable
data class ConsoleSetupRoute(val consoleId: String)

@Serializable
data class GameLibraryRoute(val consoleId: String)

@Serializable
data class GameDetailsRoute(val gameId: Long)

private data class TopLevelDestination(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        label = "Início",
        route = HomeRoute,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    TopLevelDestination(
        label = "Emuladores",
        route = ConsolesRoute,
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports,
        testTag = "nav_consoles"
    ),
    TopLevelDestination(
        label = "Configurações",
        route = SettingsRoute,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun DomDeckApp(
    viewModel: DomDeckViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isShowingPs1Transition by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEnvironmentStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevel = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ConsolesRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    val triggerPs1LibraryWithAnimation: () -> Unit = {
        if (!isShowingPs1Transition) {
            isShowingPs1Transition = true
        }
    }

    val navigateTopLevel: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BackHandler(enabled = isShowingPs1Transition) {
        // Prevent double navigation or accidental cancel during the 650ms disc showcase
    }

    DomDeckAmbientBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val frontendRoute = currentDestination?.hasRoute<HomeRoute>() == true ||
                currentDestination?.hasRoute<GameLibraryRoute>() == true ||
                currentDestination?.hasRoute<ConsolesRoute>() == true
            val useRail = maxWidth >= 600.dp && !frontendRoute

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (isTopLevel && !useRail && !frontendRoute) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassFloatingContainer(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    topLevelDestinations.forEach { dest ->
                                        val selected = when (dest.route) {
                                            HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                            ConsolesRoute -> currentDestination?.hasRoute<ConsolesRoute>() == true
                                            SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                            else -> false
                                        }
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label) },
                                            modifier = Modifier.testTag(dest.testTag),
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = DomDeckAccent,
                                                selectedTextColor = DomDeckTextPrimary,
                                                indicatorColor = DomDeckAccentMuted,
                                                unselectedIconColor = DomDeckTextSecondary,
                                                unselectedTextColor = DomDeckTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTopLevel && useRail) {
                        GlassFloatingContainer(
                            modifier = Modifier
                                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            NavigationRail(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp),
                                containerColor = Color.Transparent
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    topLevelDestinations.forEach { dest ->
                                        val selected = when (dest.route) {
                                            HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                            ConsolesRoute -> currentDestination?.hasRoute<ConsolesRoute>() == true
                                            SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                            else -> false
                                        }
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label) },
                                            modifier = Modifier.testTag(dest.testTag),
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = DomDeckAccent,
                                                selectedTextColor = DomDeckTextPrimary,
                                                indicatorColor = DomDeckAccentMuted,
                                                unselectedIconColor = DomDeckTextSecondary,
                                                unselectedTextColor = DomDeckTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    uiState = uiState,
                                    onOpenPs1Showcase = triggerPs1LibraryWithAnimation,
                                    onOpenConsoles = { navigateTopLevel(ConsolesRoute) },
                                    onOpenPs1Setup = {
                                        navController.navigate(ConsoleSetupRoute(ConsoleCatalog.PS1_ID))
                                    },
                                    onOpenGameDetails = { game ->
                                        navController.navigate(GameDetailsRoute(game.id))
                                    },
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                    onPlayGame = viewModel::launchPs1Game,
                                    onDismissLaunchResult = viewModel::dismissLaunchResult
                                )
                            }

                            composable<ConsolesRoute> {
                                ConsolesScreen(
                                    uiState = uiState,
                                    onOpenPs1WithShowcase = triggerPs1LibraryWithAnimation,
                                    onOpenPs1Setup = {
                                        navController.navigate(ConsoleSetupRoute(ConsoleCatalog.PS1_ID))
                                    },
                                    onReturnHome = { navigateTopLevel(HomeRoute) },
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) }
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onOpenPs1Setup = {
                                        navController.navigate(ConsoleSetupRoute(ConsoleCatalog.PS1_ID))
                                    }
                                )
                            }

                            composable<ConsoleSetupRoute> {
                                ConsoleSetupScreen(
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                    onRefreshDetection = viewModel::refreshEnvironmentStatus,
                                    onFolderPicked = viewModel::onPs1FolderPicked,
                                    onAssociateDuckStation = viewModel::setDuckStationAssociated,
                                    onStartScan = viewModel::startPs1Scan,
                                    onCancelScan = viewModel::cancelActiveScan,
                                    onOpenPs1Library = {
                                        navController.navigate(GameLibraryRoute(ConsoleCatalog.PS1_ID))
                                    },
                                    onDismissInfo = viewModel::clearInfoMessage
                                )
                            }

                            composable<GameLibraryRoute> {
                                HomeScreen(
                                    uiState = uiState,
                                    onOpenPs1Showcase = {},
                                    onOpenConsoles = { navigateTopLevel(ConsolesRoute) },
                                    onOpenPs1Setup = {
                                        navController.navigate(ConsoleSetupRoute(ConsoleCatalog.PS1_ID))
                                    },
                                    onOpenGameDetails = { game ->
                                        navController.navigate(GameDetailsRoute(game.id))
                                    },
                                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                                    onPlayGame = viewModel::launchPs1Game,
                                    onDismissLaunchResult = viewModel::dismissLaunchResult
                                )
                            }

                            composable<GameDetailsRoute> { backStackEntry ->
                                val args = backStackEntry.toRoute<GameDetailsRoute>()
                                val selectedGame by viewModel.observeGame(args.gameId)
                                    .collectAsStateWithLifecycle(initialValue = null)

                                GameDetailsScreen(
                                    game = selectedGame,
                                    uiState = uiState,
                                    onEnsureMetadata = viewModel::ensureGameMetadata,
                                    onSaveManualMetadata = viewModel::saveManualGameMetadata,
                                    onRestoreAutomaticMetadata = viewModel::restoreAutomaticGameMetadata,
                                    onBack = { navController.popBackStack() },
                                    onOpenSetup = {
                                        navController.navigate(ConsoleSetupRoute(ConsoleCatalog.PS1_ID))
                                    },
                                    onPlayClick = viewModel::launchPs1Game,
                                    onOpenDuckStationApp = viewModel::openDuckStationApp,
                                    onDismissLaunchResult = viewModel::dismissLaunchResult
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isShowingPs1Transition,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ConsoleTransitionOverlay(
                    consoleName = "PlayStation 1",
                    consoleShortName = "PS1",
                    onAnimationComplete = {
                        if (isShowingPs1Transition) {
                            isShowingPs1Transition = false
                            navController.navigate(GameLibraryRoute(ConsoleCatalog.PS1_ID)) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
