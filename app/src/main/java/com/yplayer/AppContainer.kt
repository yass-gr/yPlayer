package com.yplayer

import android.app.Application
import com.yplayer.data.db.YPlayerDatabase
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource
import com.yplayer.data.playlist.PlaylistRepository
import com.yplayer.data.playlist.RoomPlaylistRepository
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlaybackControllerImpl

class AppContainer(private val app: Application) {
    private val database = YPlayerDatabase.get(app)

    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
    val playlistRepository: PlaylistRepository =
        RoomPlaylistRepository(database.playlistDao(), database.playlistSongDao())
    val playbackController: PlaybackController = PlaybackControllerImpl(app)
}
