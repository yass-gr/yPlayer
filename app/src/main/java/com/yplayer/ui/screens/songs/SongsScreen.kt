package com.yplayer.ui.screens.songs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yplayer.YPlayerApp
import com.yplayer.ui.components.PlayableSongList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
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
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = false,
        state = refreshState,
        onRefresh = viewModel::refresh,
    ) {
        PlayableSongList(
            songs = songs,
            onPlay = viewModel::playSong,
        )
    }
}
