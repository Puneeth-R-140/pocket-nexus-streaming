package com.vidora.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT * FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getWatchHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE id = :id")
    suspend fun getHistoryItem(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHistory(history: HistoryEntity)
    
    // Downloads
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)
    
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownload(id: String): DownloadEntity?
    
    // Settings
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: SettingsEntity)
}

