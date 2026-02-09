package com.vidora.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class, 
        HistoryEntity::class, 
        DownloadEntity::class, 
        SettingsEntity::class
    ], 
    version = 3, 
    exportSchema = false
)
abstract class VidoraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}

