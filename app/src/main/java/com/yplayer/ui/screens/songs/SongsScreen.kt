package com.yplayer.ui.screens.songs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.YPlayerApp
import com.yplayer.ui.components.SongList

@Composable
fun SongsScreen(
    onSongClick: (Int) -> Unit = {},
    viewModel: SongsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as YPlayerApp
                SongsViewModel(app.container.libraryRepository, app.container.playbackController)
            }
        }
    ),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    SongList(songs = songs, onSongClick = onSongClick)
}
