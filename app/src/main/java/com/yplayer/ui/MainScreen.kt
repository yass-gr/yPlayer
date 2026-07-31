package com.yplayer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yplayer.ui.screens.PlaylistsScreen
import com.yplayer.ui.screens.albums.AlbumsScreen
import com.yplayer.ui.screens.artists.ArtistsScreen
import com.yplayer.ui.screens.songs.SongsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("songs", "Songs", Icons.AutoMirrored.Filled.List),
    TabItem("albums", "Albums", Icons.Filled.Album),
    TabItem("artists", "Artists", Icons.Filled.Face),
    TabItem("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        MusicPermissionGate {
            NavHost(
                navController = navController,
                startDestination = "songs",
                modifier = Modifier.padding(padding),
            ) {
                composable("songs") { SongsScreen() }
                composable("albums") { AlbumsScreen() }
                composable("artists") { ArtistsScreen() }
                composable("playlists") { PlaylistsScreen() }
            }
        }
    }
}
