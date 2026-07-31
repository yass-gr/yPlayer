package com.yplayer.data.playlist

import com.yplayer.data.db.PlaylistDao
import com.yplayer.data.db.PlaylistEntity
import com.yplayer.data.db.PlaylistSongDao
import com.yplayer.data.db.PlaylistSongEntity

class RoomPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao,
) : PlaylistRepository {

    override suspend fun create(name: String): Long {
        val now = System.currentTimeMillis()
        return playlistDao.insert(PlaylistEntity(name = name, createdAt = now))
    }

    override suspend fun getAll(): List<Playlist> =
        playlistDao.getAll().map { Playlist(it.id, it.name, it.createdAt) }

    override suspend fun delete(id: Long) {
        playlistSongDao.deleteAll(id)
        playlistDao.delete(id)
    }

    override suspend fun rename(id: Long, name: String) {
        playlistDao.get(id)?.let {
            playlistDao.update(it.copy(name = name))
        }
    }

    override suspend fun getPlaylistSongs(id: Long): List<Long> =
        playlistSongDao.getSongs(id).map { it.mediaId }

    override suspend fun addSong(playlistId: Long, mediaId: Long) {
        val position = playlistSongDao.getSongs(playlistId).size
        playlistSongDao.insertAll(
            listOf(PlaylistSongEntity(playlistId = playlistId, mediaId = mediaId, position = position))
        )
    }

    override suspend fun removeSong(playlistId: Long, mediaId: Long) {
        playlistSongDao.remove(playlistId, mediaId)
        renumber(playlistId)
    }

    override suspend fun reorder(playlistId: Long, orderedMediaIds: List<Long>) {
        val existing = playlistSongDao.getSongs(playlistId).associateBy { it.mediaId }
        val updated = orderedMediaIds.mapIndexedNotNull { index, mediaId ->
            existing[mediaId]?.copy(position = index)
        }
        playlistSongDao.deleteAll(playlistId)
        playlistSongDao.insertAll(updated)
    }

    override suspend fun isInPlaylist(playlistId: Long, mediaId: Long): Boolean =
        playlistSongDao.count(playlistId, mediaId) > 0

    private suspend fun renumber(playlistId: Long) {
        val current = playlistSongDao.getSongs(playlistId)
        playlistSongDao.deleteAll(playlistId)
        playlistSongDao.insertAll(current.mapIndexed { index, entity -> entity.copy(position = index) })
    }
}
