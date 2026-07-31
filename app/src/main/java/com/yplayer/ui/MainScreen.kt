package com.yplayer.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yplayer.ui.screens.albumdetail.AlbumDetailScreen
import com.yplayer.ui.screens.albums.AlbumsScreen
import com.yplayer.ui.screens.artistdetail.ArtistDetailScreen
import com.yplayer.ui.screens.artists.ArtistsScreen
import com.yplayer.ui.screens.nowplaying.NowPlayingScreen
import com.yplayer.ui.screens.playlistdetail.PlaylistDetailScreen
import com.yplayer.ui.screens.playlists.PlaylistsScreen
import com.yplayer.ui.screens.search.SearchScreen
import com.yplayer.ui.screens.songs.SongsScreen

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("songs", "Songs", Icons.AutoMirrored.Filled.List),
    TabItem("albums", "Albums", Icons.Filled.Album),
    TabItem("artists", "Artists", Icons.Filled.Face),
    TabItem("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in tabs.map { it.route }
    Scaffold(
        topBar = {
            if (isTopLevel) {
                TopAppBar(
                    title = { Text("yPlayer") },
                    actions = {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar {
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
                composable("albums") {
                    AlbumsScreen(onAlbumClick = { album ->
                        navController.navigate(
                            "album/${album.id}/${Uri.encode(album.title)}/${Uri.encode(album.artist)}"
                        )
                    })
                }
                composable("artists") {
                    ArtistsScreen(onArtistClick = { artist ->
                        navController.navigate("artist/${Uri.encode(artist.name)}")
                    })
                }
                composable("playlists") {
                    PlaylistsScreen(onPlaylistClick = { playlist ->
                        navController.navigate("playlist/${playlist.id}/${Uri.encode(playlist.name)}")
                    })
                }
                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { navController.navigate("nowPlaying") },
                    )
                }
                composable(
                    route = "album/{albumId}/{title}/{artist}",
                    arguments = listOf(
                        navArgument("albumId") { type = NavType.LongType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("artist") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val albumId = entry.arguments?.getLong("albumId") ?: 0L
                    val title = entry.arguments?.getString("title") ?: ""
                    val artist = entry.arguments?.getString("artist") ?: ""
                    AlbumDetailScreen(
                        albumId = albumId,
                        albumTitle = title,
                        albumArtist = artist,
                        onBack = { navController.popBackStack() },
                        onSongClick = { index -> navController.navigate("nowPlaying") },
                    )
                }
                composable(
                    route = "artist/{name}",
                    arguments = listOf(navArgument("name") { type = NavType.StringType }),
                ) { entry ->
                    val name = entry.arguments?.getString("name") ?: ""
                    ArtistDetailScreen(
                        artistName = name,
                        onBack = { navController.popBackStack() },
                        onSongClick = { index -> navController.navigate("nowPlaying") },
                    )
                }
                composable("nowPlaying") { NowPlayingScreen() }
                composable(
                    route = "playlist/{playlistId}/{name}",
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.LongType },
                        navArgument("name") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
                    val name = entry.arguments?.getString("name") ?: ""
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        playlistName = name,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
