package com.yplayer

import android.app.Application
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlaybackControllerImpl

class AppContainer(private val app: Application) {
    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
    val playbackController: PlaybackController = PlaybackControllerImpl(app)
}
