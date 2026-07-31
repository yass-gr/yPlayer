package com.yplayer.player

import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0,
)

interface PlaybackController {
    val state: StateFlow<PlayerState>
    fun playQueue(songs: List<Song>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(on: Boolean)
    fun cycleRepeat()
}
