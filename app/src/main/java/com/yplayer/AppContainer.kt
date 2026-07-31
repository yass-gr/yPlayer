package com.yplayer

import android.app.Application
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.library.MediaStoreLibrarySource

class AppContainer(private val app: Application) {
    val libraryRepository = LibraryRepository(MediaStoreLibrarySource(app))
}
