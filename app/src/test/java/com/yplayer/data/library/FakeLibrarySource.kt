package com.yplayer.data.library

import com.yplayer.data.model.Song

class FakeLibrarySource(private val songs: List<Song>) : LibrarySource {
    override suspend fun querySongs(): List<Song> = songs
}
