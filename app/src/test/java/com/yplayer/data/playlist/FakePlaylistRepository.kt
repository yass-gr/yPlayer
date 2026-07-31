package com.yplayer.data.playlist

class FakePlaylistRepository : PlaylistRepository {

    private val playlists = mutableListOf<Playlist>()
    private val songs = mutableMapOf<Long, MutableList<Long>>()
    private var nextId = 1L

    override suspend fun create(name: String): Long {
        val id = nextId++
        playlists += Playlist(id, name, 0L)
        return id
    }

    override suspend fun getAll(): List<Playlist> = playlists.sortedBy { it.name.lowercase() }

    override suspend fun delete(id: Long) {
        playlists.removeAll { it.id == id }
        songs.remove(id)
    }

    override suspend fun rename(id: Long, name: String) {
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx >= 0) playlists[idx] = playlists[idx].copy(name = name)
    }

    override suspend fun getPlaylistSongs(id: Long): List<Long> = songs[id].orEmpty().toList()

    override suspend fun addSong(playlistId: Long, mediaId: Long) {
        songs.getOrPut(playlistId) { mutableListOf() }.add(mediaId)
    }

    override suspend fun removeSong(playlistId: Long, mediaId: Long) {
        songs[playlistId]?.remove(mediaId)
    }

    override suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        songs[playlistId] = orderedMediaIds.toMutableList()
    }

    override suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean =
        songs[playlistId]?.contains(mediaId) == true
}
