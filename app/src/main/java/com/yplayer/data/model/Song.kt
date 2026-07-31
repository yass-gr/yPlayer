package com.yplayer.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songs: List<Song>,
)

data class Artist(
    val name: String,
    val songs: List<Song>,
)
