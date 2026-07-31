package com.yplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class YPlayerDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        fun get(context: Context): YPlayerDatabase =
            Room.databaseBuilder(context, YPlayerDatabase::class.java, "yplayer.db").build()
    }
}
