package com.yplayer.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.playlist.Playlist
import com.yplayer.data.playlist.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val repository: PlaylistRepository,
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _playlists.value = repository.getAll() }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.create(name)
            refresh()
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            refresh()
        }
    }
}
