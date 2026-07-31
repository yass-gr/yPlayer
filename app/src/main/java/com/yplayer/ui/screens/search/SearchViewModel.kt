package com.yplayer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.player.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: LibraryRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Song>> = combine(query, repository.songs) { q, songs ->
        repository.search(q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun playSong(index: Int) {
        val list = results.value
        if (index in list.indices) playbackController.playQueue(list, index)
    }
}
