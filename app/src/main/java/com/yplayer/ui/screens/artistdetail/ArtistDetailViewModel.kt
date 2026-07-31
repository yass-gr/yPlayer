package com.yplayer.ui.screens.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    repository: LibraryRepository,
    private val playbackController: PlaybackController,
    artistName: String,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.songs
        .map { list -> list.filter { it.artist == artistName } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun playSong(index: Int) {
        val list = songs.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
