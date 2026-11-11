package com.f7developer.tapeback.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.f7developer.tapeback.R
import com.f7developer.tapeback.ui.capsule.CapsuleScreen
import com.f7developer.tapeback.ui.components.AnalogOverlay
import com.f7developer.tapeback.ui.components.retroBackground
import com.f7developer.tapeback.ui.deck.DeckScreen
import com.f7developer.tapeback.ui.diary.DiaryScreen
import com.f7developer.tapeback.ui.library.CassetteDetailScreen
import com.f7developer.tapeback.ui.library.LibraryScreen
import com.f7developer.tapeback.ui.onboarding.OnboardingScreen
import com.f7developer.tapeback.ui.settings.SettingsScreen
import com.f7developer.tapeback.ui.theme.Retro
import com.f7developer.tapeback.ui.theme.TapeBackTheme
import kotlinx.coroutines.flow.MutableStateFlow

object Routes {
    const val DECK = "deck"
    const val LIBRARY = "library"
    const val DIARY = "diary"
    const val CAPSULES = "capsules"
    const val SETTINGS = "settings"
    const val CASSETTE = "cassette/{id}"
    fun cassette(id: Long) = "cassette/$id"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DECK, R.string.tab_deck, Icons.Rounded.Radio),
    Tab(Routes.LIBRARY, R.string.tab_library, Icons.Rounded.LibraryMusic),
    Tab(Routes.DIARY, R.string.tab_diary, Icons.Rounded.AutoStories),
    Tab(Routes.CAPSULES, R.string.tab_capsules, Icons.Rounded.HourglassTop),
)

@Composable
fun TapeBackRoot(rootVm: RootViewModel, pendingRoute: MutableStateFlow<String?>) {
    val settings by rootVm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return // the splash screen stays visible until settings are read
    TapeBackTheme(s) {
        val palette = Retro.palette
        val activity = LocalActivity.current as? ComponentActivity
        LaunchedEffect(palette.isDark) {
            val style = if (palette.isDark) SystemBarStyle.dark(Color.Transparent.toArgb())
            else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
            activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        Box(Modifier.fillMaxSize().retroBackground(palette.background, palette.backgroundGlow)) {
            // The shell is transparent over the retro background, so set the content color explicitly.
            CompositionLocalProvider(LocalContentColor provides palette.text) {
                if (!s.onboardingDone) {
                    OnboardingScreen(onDone = rootVm::completeOnboarding)
                } else {
                    MainShell(pendingRoute)
                }
            }
            AnalogOverlay(grain = s.grain, scanlines = s.scanlines, reduceMotion = Retro.reduceMotion, dark = palette.isDark)
        }
    }
}

@Composable
private fun MainShell(pendingRoute: MutableStateFlow<String?>) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showNav = currentRoute in tabs.map { it.route } || currentRoute == null
    val palette = Retro.palette

    val pending by pendingRoute.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        pending?.let {
            nav.navigateTab(it)
            pendingRoute.value = null
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide && showNav) {
                NavigationRail(containerColor = palette.surface.copy(alpha = 0.92f), modifier = Modifier.statusBarsPadding()) {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = palette.accent.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = palette.text,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (!wide && showNav) {
                        NavigationBar(containerColor = palette.surface.copy(alpha = 0.94f)) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { nav.navigateTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(stringResource(tab.label)) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = palette.accent.copy(alpha = 0.25f)),
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) { padding ->
                NavHost(navController = nav, startDestination = Routes.DECK) {
                    composable(Routes.DECK) {
                        DeckScreen(
                            snackbar = snackbar,
                            contentPadding = padding,
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                            onOpenLibrary = { nav.navigateTab(Routes.LIBRARY) },
                        )
                    }
                    composable(Routes.LIBRARY) {
                        LibraryScreen(onOpenCassette = { nav.navigate(Routes.cassette(it)) }, contentPadding = padding)
                    }
                    composable(Routes.CASSETTE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        CassetteDetailScreen(
                            cassetteId = id,
                            onBack = { nav.popBackStack() },
                            onOpenDeck = { nav.navigateTab(Routes.DECK) },
                            contentPadding = padding,
                        )
                    }
                    composable(Routes.DIARY) {
                        DiaryScreen(onOpenDeck = { nav.navigateTab(Routes.DECK) }, contentPadding = padding)
                    }
                    composable(Routes.CAPSULES) {
                        CapsuleScreen(
                            onRecordCapsule = { nav.navigateTab(Routes.DECK) },
                            onOpenDeck = { nav.navigateTab(Routes.DECK) },
                            contentPadding = padding,
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(onBack = { nav.popBackStack() }, contentPadding = padding)
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
