package com.yplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.yplayer.ui.components.SongArtFetcher
import com.yplayer.ui.components.SongArtKeyer

class YPlayerApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(SongArtKeyer())
                add(SongArtFetcher.Factory())
            }
            .build()
}
