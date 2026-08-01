package com.yplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yplayer.data.model.Song

@Composable
fun SongList(songs: List<Song>, onSongClick: (Int) -> Unit, emptyMessage: String = "No songs") {
    if (songs.isEmpty()) {
        EmptyState(emptyMessage)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongRow(song = song, onClick = { onSongClick(index) })
            }
        }
    }
}

@Composable
fun PlayableSongList(
    songs: List<Song>,
    onPlay: (Int) -> Unit,
    onPlaybackStarted: () -> Unit,
    emptyMessage: String = "No songs",
) {
    SongList(
        songs = songs,
        emptyMessage = emptyMessage,
        onSongClick = { index ->
            onPlay(index)
            onPlaybackStarted()
        },
    )
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("songRow")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = song.albumId,
            songUri = song.uri,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
}
