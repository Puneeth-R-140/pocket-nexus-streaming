package com.vidora.app.data.repository

import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.remote.TmdbService
import com.vidora.app.data.local.MediaDao
import com.vidora.app.util.NetworkResult
import com.vidora.app.util.safeApiCall
import com.vidora.app.util.retryApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbService: TmdbService,
    private val subtitleService: com.vidora.app.data.remote.SubtitleService,
    private val omdbService: com.vidora.app.data.remote.OmdbService,
    private val mediaDao: MediaDao
) {
    fun getTrendingMovies(): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                val response = tmdbService.getTrending("movie")
                response.results
            }
        }
        emit(result)
    }

    fun getTrendingTVShows(): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                val response = tmdbService.getTrending("tv")
                response.results
            }
        }
        emit(result)
    }

    fun search(query: String): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = safeApiCall {
            val response = tmdbService.searchMulti(query)
            response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
        }
        emit(result)
    }

    fun getDetails(mediaType: String, id: String): Flow<NetworkResult<MediaItem>> = flow {
        emit(NetworkResult.Loading)
        val result = retryApiCall {
            safeApiCall {
                tmdbService.getDetails(mediaType, id)
            }
        }
        emit(result)
    }

    fun getEpisodes(tvId: String, season: Int): Flow<NetworkResult<List<com.vidora.app.data.remote.Episode>>> = flow {
        emit(NetworkResult.Loading)
        val result = safeApiCall {
            tmdbService.getSeasonEpisodes(tvId, season).episodes
        }
        emit(result)
    }

    fun getRecommendations(mediaType: String, id: String): Flow<NetworkResult<List<MediaItem>>> = flow {
        emit(NetworkResult.Loading)
        val result = safeApiCall {
            tmdbService.getRecommendations(mediaType, id).results
        }
        emit(result)
    }

    suspend fun getSubtitles(tmdbId: String, mediaType: String, season: Int? = null, episode: Int? = null): List<com.vidora.app.data.remote.SubtitleItem> {
        return try {
            if (mediaType == "tv" && season != null && episode != null) {
                subtitleService.searchSubtitles(tmdbId, season, episode)
            } else {
                subtitleService.searchSubtitles(tmdbId)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Local Favorites
    fun getFavorites() = mediaDao.getAllFavorites()

    suspend fun toggleFavorite(media: MediaItem) {
        if (mediaDao.isFavorite(media.id)) {
            mediaDao.deleteFavorite(com.vidora.app.data.local.FavoriteEntity(media.id, media.displayTitle, media.posterPath, media.realMediaType))
        } else {
            mediaDao.insertFavorite(com.vidora.app.data.local.FavoriteEntity(media.id, media.displayTitle, media.posterPath, media.realMediaType))
        }
    }

    suspend fun isFavorite(id: String) = mediaDao.isFavorite(id)

    // History
    fun getWatchHistory() = mediaDao.getWatchHistory()

    suspend fun getHistoryItem(id: String, season: Int? = null, episode: Int? = null): com.vidora.app.data.local.HistoryEntity? {
        val compositeId = if (season != null && episode != null) "${id}_s${season}_e${episode}" else id
        return mediaDao.getHistoryItem(compositeId)
    }

    suspend fun updateHistory(media: MediaItem, positionMs: Long, durationMs: Long, season: Int? = null, episode: Int? = null) {
        val compositeId = if (season != null && episode != null) "${media.id}_s${season}_e${episode}" else media.id
        mediaDao.updateHistory(
            com.vidora.app.data.local.HistoryEntity(
                id = compositeId,
                mediaId = media.id,
                title = media.displayTitle,
                posterPath = media.posterPath,
                mediaType = media.realMediaType,
                positionMs = positionMs,
                durationMs = durationMs,
                season = season,
                episode = episode
            )
        )
    }
    
    // IMDb & Rotten Tomatoes Ratings
    suspend fun getRatings(imdbId: String): com.vidora.app.data.remote.OmdbResponse? {
        return try {
            safeApiCall {
                omdbService.getRatings(imdbId, "8e11dbab") // Free API key
            }.let { result ->
                when (result) {
                    is NetworkResult.Success -> result.data
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Downloads
    fun getAllDownloads() = mediaDao.getAllDownloads()
    
    suspend fun addDownload(download: com.vidora.app.data.local.DownloadEntity) {
        mediaDao.insertDownload(download)
    }
    
    suspend fun deleteDownload(download: com.vidora.app.data.local.DownloadEntity) {
        mediaDao.deleteDownload(download)
    }
    
    suspend fun getDownload(id: String) = mediaDao.getDownload(id)
    
    // Settings
    suspend fun getSettings(): com.vidora.app.data.local.SettingsEntity {
        return mediaDao.getSettings() ?: com.vidora.app.data.local.SettingsEntity()
    }
    
    suspend fun updateSettings(settings: com.vidora.app.data.local.SettingsEntity) {
        mediaDao.updateSettings(settings)
    }
}

