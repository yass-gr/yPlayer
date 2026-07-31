package com.yplayer.player

import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class FakePlaybackController : PlaybackController {
    override val state: StateFlow<PlayerState> = MutableStateFlow(PlayerState()).asStateFlow()

    var lastQueue: List<Song>? = null
    var lastStartIndex: Int = -1
    var lastSeekMs: Long = -1
    var toggles = 0
    var nexts = 0
    var previous = 0

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        lastQueue = songs
        lastStartIndex = startIndex
    }

    override fun togglePlayPause() {
        toggles++
    }

    override fun next() {
        nexts++
    }

    override fun previous() {
        previous++
    }

    override fun seekTo(positionMs: Long) {
        lastSeekMs = positionMs
    }

    override fun setShuffle(on: Boolean) {}

    override fun cycleRepeat() {}
}
