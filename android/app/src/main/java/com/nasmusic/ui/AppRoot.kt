package com.nasmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nasmusic.NasMusicApp
import com.nasmusic.ui.screens.AlbumScreen
import com.nasmusic.ui.screens.AllAlbumsScreen
import com.nasmusic.ui.screens.AllArtistsScreen
import com.nasmusic.ui.screens.ArtistScreen
import com.nasmusic.ui.screens.FoldersScreen
import com.nasmusic.ui.screens.GenreScreen
import com.nasmusic.ui.screens.HomeScreen
import com.nasmusic.ui.screens.LibraryScreen
import com.nasmusic.ui.screens.LikedScreen
import com.nasmusic.ui.screens.LoginScreen
import com.nasmusic.ui.screens.MiniPlayer
import com.nasmusic.ui.screens.NowPlayingScreen
import com.nasmusic.ui.screens.PlaylistScreen
import com.nasmusic.ui.screens.SearchScreen
import com.nasmusic.ui.screens.UsersScreen
import com.nasmusic.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable
fun AppRoot(app: NasMusicApp) {
    val token by app.session.token.collectAsState()
    // Gives every screen (login, full-screen player) the theme's text colour.
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        CompositionLocalProvider(LocalApp provides app) {
            if (token == null) LoginScreen() else MainScaffold(app)
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Inicio", Icons.Rounded.Home),
    Tab(Routes.SEARCH, "Buscar", Icons.Rounded.Search),
    Tab(Routes.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
)

@Composable
private fun MainScaffold(app: NasMusicApp) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val player by app.player.state.collectAsState()
    var nowPlaying by rememberSaveable { mutableStateOf(false) }
    val toast: (String) -> Unit = { msg ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) { app.onSignedIn() }
    LaunchedEffect(player.error) {
        player.error?.let {
            toast(it)
            app.player.clearError()
        }
    }

    CompositionLocalProvider(LocalNav provides nav, LocalToast provides toast) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color(0xFF121212),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    Column {
                        if (player.current != null) MiniPlayer(onOpen = { nowPlaying = true })
                        val entry by nav.currentBackStackEntryAsState()
                        val current = entry?.destination?.route
                        NavigationBar(containerColor = Color.Black) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = current == tab.route,
                                    onClick = {
                                        nav.navigate(tab.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        unselectedIconColor = Muted,
                                        unselectedTextColor = Muted,
                                        indicatorColor = Color.Transparent,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
                    composable(Routes.HOME) { HomeScreen() }
                    composable(Routes.SEARCH) { SearchScreen() }
                    composable(Routes.LIBRARY) { LibraryScreen() }
                    composable(Routes.LIKED) { LikedScreen() }
                    composable(Routes.ALBUMS) { AllAlbumsScreen() }
                    composable(Routes.ARTISTS) { AllArtistsScreen() }
                    composable(Routes.FOLDERS) { FoldersScreen() }
                    composable(Routes.USERS) { UsersScreen() }
                    composable("album/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                        AlbumScreen(it.arguments!!.getLong("id"))
                    }
                    composable("artist/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                        ArtistScreen(it.arguments!!.getLong("id"))
                    }
                    composable("playlist/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                        PlaylistScreen(it.arguments!!.getLong("id"))
                    }
                    composable("genre/{name}") { GenreScreen(it.arguments?.getString("name").orEmpty()) }
                }
            }

            AnimatedVisibility(
                visible = nowPlaying && player.current != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                BackHandler { nowPlaying = false }
                Box(Modifier.fillMaxSize().background(Color(0xFF121212))) {
                    NowPlayingScreen(onClose = { nowPlaying = false })
                }
            }
        }
    }
}
