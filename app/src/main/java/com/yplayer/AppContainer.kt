package com.yplayer

import android.app.Application

class AppContainer(private val app: Application) {
    // Repositories and the playback controller are wired here as later tasks add them.
}
