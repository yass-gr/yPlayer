package com.yplayer.data.playlist

data class Playlist(val id: Long, val name: String, val createdAt: Long)

interface PlaylistRepository {
    suspend fun create(name: String): Long
    suspend fun getAll(): List<Playlist>
    suspend fun delete(id: Long)
    suspend fun rename(id: Long, name: String)
    suspend fun getPlaylistSongs(id: Long): List<Long>
    suspend fun addSong(playlistId: Long, mediaId: Long)
    suspend fun removeSong(playlistId: Long, mediaId: Long)
    suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>)
    suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean
}
