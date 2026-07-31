package com.yplayer.data.library

import com.yplayer.data.model.Song

interface LibrarySource {
    suspend fun querySongs(): List<Song>
}
