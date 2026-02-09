package com.vidora.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val id: String, // For movies, this is tmdbId. For TV, this is "tvId_sX_eY"
    val mediaId: String,       // TMDB ID
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val season: Int? = null,
    val episode: Int? = null
)

fun FavoriteEntity.toMediaItem() = com.vidora.app.data.remote.MediaItem(
    id = id,
    title = if (mediaType == "movie") title else null,
    name = if (mediaType == "tv") title else null,
    overview = null,
    posterPath = posterPath,
    backdropPath = null,
    voteAverage = 0.0,
    releaseDate = null,
    firstAirDate = null,
    mediaType = mediaType,
    popularity = null,
    genres = null,
    credits = null,
    similar = null,
    numberOfSeasons = null,
    seasons = null,
    runtime = null,
    episodeRunTime = null,
    imdbId = null,
    contentRatings = null
)

fun HistoryEntity.toMediaItem() = com.vidora.app.data.remote.MediaItem(
    id = mediaId,
    title = if (mediaType == "movie") title else null,
    name = if (mediaType == "tv") title else null,
    overview = null,
    posterPath = posterPath,
    backdropPath = null,
    voteAverage = 0.0,
    releaseDate = null,
    firstAirDate = null,
    mediaType = mediaType,
    popularity = null,
    genres = null,
    credits = null,
    similar = null,
    numberOfSeasons = null,
    seasons = null,
    runtime = null,
    episodeRunTime = null,
    imdbId = null,
    contentRatings = null
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val quality: String,
    val fileSizeMb: Float,
    val filePath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val status: String = "completed", // pending, downloading, completed, failed
    val season: Int? = null,
    val episode: Int? = null
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val preferredQuality: String = "720p",
    val autoPlayNextEpisode: Boolean = true,
    val defaultSubtitleLanguage: String = "en",
    val downloadQuality: String = "720p"
)

