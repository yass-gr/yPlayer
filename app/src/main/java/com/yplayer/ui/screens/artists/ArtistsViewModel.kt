package com.yplayer.ui.screens.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Artist
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistsViewModel(
    repository: LibraryRepository,
) : ViewModel() {

    val artists: StateFlow<List<Artist>> = repository.artists

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
