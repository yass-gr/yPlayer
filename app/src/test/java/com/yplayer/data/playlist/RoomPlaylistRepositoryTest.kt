package com.yplayer.data.playlist

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yplayer.data.db.PlaylistDao
import com.yplayer.data.db.PlaylistSongDao
import com.yplayer.data.db.YPlayerDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomPlaylistRepositoryTest {

    private lateinit var db: YPlayerDatabase
    private lateinit var repo: RoomPlaylistRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, YPlayerDatabase::class.java).build()
        repo = RoomPlaylistRepository(db.playlistDao(), db.playlistSongDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createAndGet_returnsOrderedByName() = runTest {
        repo.create("Road Trip")
        repo.create("Chill")

        val playlists = repo.getAll()
        assertEquals(listOf("Chill", "Road Trip"), playlists.map { it.name })
    }

    @Test
    fun addSongs_appendsAtEnd() = runTest {
        val id = repo.create("Mix")

        repo.addSong(id, 101)
        repo.addSong(id, 202)
        repo.addSong(id, 101)

        assertEquals(listOf(101L, 202L, 101L), repo.getPlaylistSongs(id))
    }

    @Test
    fun removeSong_deletesOnlyThatSong() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)
        repo.addSong(id, 202)

        repo.removeSong(id, 101)

        assertEquals(listOf(202L), repo.getPlaylistSongs(id))
    }

    @Test
    fun reorder_updatesPositions() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)
        repo.addSong(id, 202)
        repo.addSong(id, 303)

        repo.reorder(id, listOf(303L, 101L, 202L))

        assertEquals(listOf(303L, 101L, 202L), repo.getPlaylistSongs(id))
    }

    @Test
    fun isInPlaylist_checksMembership() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)

        assertTrue(repo.isInPlaylist(id, 101))
        assertEquals(false, repo.isInPlaylist(id, 999))
    }

    @Test
    fun delete_removesPlaylistAndItsSongs() = runTest {
        val id = repo.create("Mix")
        repo.addSong(id, 101)

        repo.delete(id)

        assertEquals(0, repo.getAll().size)
    }

    @Test
    fun rename_updatesName() = runTest {
        val id = repo.create("Old")
        repo.rename(id, "New")

        assertEquals("New", repo.getAll().single().name)
    }
}
