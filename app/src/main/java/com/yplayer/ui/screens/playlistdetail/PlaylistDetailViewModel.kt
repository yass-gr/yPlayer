package com.yplayer.ui.screens.playlistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.data.playlist.PlaylistRepository
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController,
    private val playlistId: Long,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isInPlaylist = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val isInPlaylist: StateFlow<Map<Long, Boolean>> = _isInPlaylist.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.refresh()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val mediaIds = playlistRepository.getPlaylistSongs(playlistId)
            val allSongs = libraryRepository.songs.value.associateBy { it.id }
            _songs.value = mediaIds.mapNotNull { allSongs[it] }
            _isInPlaylist.value = allSongs.keys.associateWith { mediaIds.contains(it) }
        }
    }

    fun libraryAllSongs(): List<Song> = libraryRepository.songs.value

    fun addSong(mediaId: Long) {
        viewModelScope.launch {
            playlistRepository.addSong(playlistId, mediaId)
            refresh()
        }
    }

    fun removeSong(index: Int) {
        val song = _songs.value.getOrNull(index) ?: return
        viewModelScope.launch {
            playlistRepository.removeSong(playlistId, song.id)
            refresh()
        }
    }

    fun moveSong(from: Int, to: Int) {
        val current = _songs.value
        if (from !in current.indices || to !in current.indices) return
        val reordered = current.toMutableList().apply {
            add(to, removeAt(from))
        }
        viewModelScope.launch {
            playlistRepository.reorder(playlistId, reordered.map { it.id })
            refresh()
        }
    }

    fun playSong(index: Int) {
        val list = _songs.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
