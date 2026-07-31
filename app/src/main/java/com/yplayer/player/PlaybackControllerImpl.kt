package com.yplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yplayer.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackControllerImpl(context: Context) : PlaybackController, Player.Listener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionTicker: Job? = null

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(this) }
        }, MoreExecutors.directExecutor())
    }

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        val items = songs.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
        c.prepare()
        c.play()
    }

    override fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun setShuffle(on: Boolean) {
        controller?.shuffleModeEnabled = on
    }

    override fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_REPEAT_MODE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
        ) {
            syncState()
        }
    }

    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        // A file was deleted or became unreadable: skip to the next playable track.
        controller?.seekToNextMediaItem()
        syncState()
    }

    private fun syncState() {
        val c = controller ?: return
        _state.value = PlayerState(
            currentSong = c.currentMediaItem?.toSong(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition,
            durationMs = c.duration.coerceAtLeast(0L),
            isShuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
        )
        updateTicker(c.isPlaying)
    }

    private fun updateTicker(isPlaying: Boolean) {
        if (isPlaying) {
            positionTicker?.cancel()
            positionTicker = scope.launch {
                while (isActive) {
                    _state.value = _state.value.copy(positionMs = controller?.currentPosition ?: 0L)
                    delay(500)
                }
            }
        } else {
            positionTicker?.cancel()
        }
    }
}
