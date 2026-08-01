package com.yplayer.ui.screens.nowplaying

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.SubcomposeAsyncImage
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlayerState
import com.yplayer.ui.components.AlbumArt
import com.yplayer.ui.components.SongArt
import com.yplayer.ui.motion.SheetState
import com.yplayer.ui.motion.rememberArtworkAngle
import com.yplayer.ui.motion.rememberSheetState
import com.yplayer.ui.motion.sheetDrag
import com.yplayer.ui.motion.shouldReduceMotion
import kotlin.math.roundToInt

private val MiniBarHeight = 64.dp

@Composable
fun NowPlayingSheet(
    playbackController: PlaybackController,
    bottomBarHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    val state by playbackController.state.collectAsStateWithLifecycle()
    val song = state.currentSong ?: return
    val reducedMotion = shouldReduceMotion()
    val sheetState = rememberSheetState(reducedMotion)
    val density = LocalDensity.current

    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val collapsedHeightPx = with(density) { MiniBarHeight.toPx() }
    val dragRangePx = screenHeightPx - bottomBarHeightPx.toFloat() - collapsedHeightPx
    val progress = sheetState.progress.value

    val containerHeightPx = collapsedHeightPx + progress * (screenHeightPx - collapsedHeightPx)
    val topPx = (1f - progress) * dragRangePx

    Box(modifier = modifier) {
        if (progress > 0.02f) {
            Backdrop(
                songUri = song.uri,
                albumId = song.albumId,
                progress = progress,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, topPx.roundToInt()) }
                .height(with(density) { containerHeightPx.toDp() }),
        ) {
            ExpandedContent(
                playbackController = playbackController,
                state = state,
                sheetState = sheetState,
                progress = progress,
                dragRangePx = dragRangePx,
                modifier = Modifier.weight(1f),
            )
            MiniBar(
                playbackController = playbackController,
                state = state,
                sheetState = sheetState,
                dragRangePx = dragRangePx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { collapsedHeightPx.toDp() })
                    .alpha((1f - progress).coerceIn(0f, 1f)),
            )
        }
    }
}

@Composable
private fun Backdrop(
    songUri: Uri?,
    albumId: Long,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.graphicsLayer { alpha = progress }) {
        SubcomposeAsyncImage(
            model = SongArt(songUri, albumId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.2f)
                .blur(48.dp),
            error = { },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.7f)),
                    ),
                ),
        )
    }
}

@Composable
private fun ExpandedContent(
    playbackController: PlaybackController,
    state: PlayerState,
    sheetState: SheetState,
    progress: Float,
    dragRangePx: Float,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    val angle = rememberArtworkAngle(
        isPlaying = state.isPlaying,
        degreesPerSecond = { lerp(8f, 24f, progress) },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragHandle(modifier = Modifier.padding(top = 12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .sheetDrag(sheetState, dragRangePx),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkDisc(
                songUri = song.uri,
                albumId = song.albumId,
                angle = angle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f),
            )
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
            onValueChange = { playbackController.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.coerceAtLeast(1L).toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${formatTime(state.positionMs)}  /  ${formatTime(state.durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TransportControls(
            playbackController = playbackController,
            state = state,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun ArtworkDisc(
    songUri: Uri?,
    albumId: Long,
    angle: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = angle }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = SongArt(songUri, albumId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { },
            error = { },
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            )
        }
    }
}

@Composable
private fun MiniBar(
    playbackController: PlaybackController,
    state: PlayerState,
    sheetState: SheetState,
    dragRangePx: Float,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .sheetDrag(sheetState, dragRangePx, onTap = { sheetState.expand() })
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("miniPlayer"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = song.albumId,
            songUri = song.uri,
            modifier = Modifier.size(48.dp),
            cornerRadius = 8,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = { playbackController.togglePlayPause() }) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
            )
        }
    }
}

@Composable
private fun TransportControls(
    playbackController: PlaybackController,
    state: PlayerState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { playbackController.setShuffle(!state.isShuffle) }) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffle) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { playbackController.previous() }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
        }
        IconButton(
            onClick = { playbackController.togglePlayPause() },
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(72.dp),
            )
        }
        IconButton(onClick = { playbackController.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next")
        }
        IconButton(onClick = { playbackController.cycleRepeat() }) {
            Icon(
                imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
