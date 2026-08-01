package com.yplayer.ui.screens.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongsViewModel(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.songs

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun playSong(index: Int) {
        val list = repository.songs.value
        if (index in list.indices) {
            playbackController.playQueue(list, index)
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
