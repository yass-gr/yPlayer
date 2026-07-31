package com.yplayer.data.library

import com.yplayer.data.model.Album
import com.yplayer.data.model.Artist
import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryRepository(private val source: LibrarySource) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    suspend fun refresh() {
        val list = source.querySongs()
        _songs.value = list
        _albums.value = groupAlbums(list)
        _artists.value = groupArtists(list)
    }

    fun songsOfAlbum(albumId: Long): List<Song> = _songs.value.filter { it.albumId == albumId }

    fun songsOfArtist(artistName: String): List<Song> = _songs.value.filter { it.artist == artistName }

    fun search(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _songs.value
        return _songs.value.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    private fun groupAlbums(list: List<Song>): List<Album> =
        list.groupBy { it.albumId }
            .values
            .map { group ->
                Album(
                    id = group.first().albumId,
                    title = group.first().album,
                    artist = group.first().artist,
                    songs = group.sortedBy { it.title },
                )
            }

    private fun groupArtists(list: List<Song>): List<Artist> =
        list.groupBy { it.artist }
            .values
            .map { group -> Artist(name = group.first().artist, songs = group.sortedBy { it.title }) }
            .sortedBy { it.name }
}
