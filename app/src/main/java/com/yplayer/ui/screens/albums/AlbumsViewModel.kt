package com.yplayer.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Album
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel(
    repository: LibraryRepository,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository.albums

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
