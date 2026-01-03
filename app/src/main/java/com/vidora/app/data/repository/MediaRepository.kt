package com.vidora.app.data.repository

import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.remote.TmdbService
import com.vidora.app.data.local.MediaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbService: TmdbService,
    private val mediaDao: MediaDao
) {
    fun getTrendingMovies(): Flow<List<MediaItem>> = flow {
        val response = tmdbService.getTrending("movie")
        emit(response.results)
    }

    fun getTrendingTVShows(): Flow<List<MediaItem>> = flow {
        val response = tmdbService.getTrending("tv")
        emit(response.results)
    }

    fun search(query: String): Flow<List<MediaItem>> = flow {
        val response = tmdbService.searchMulti(query)
        emit(response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" })
    }

    fun getDetails(mediaType: String, id: String): Flow<MediaItem> = flow {
        emit(tmdbService.getDetails(mediaType, id))
    }

    fun getEpisodes(tvId: String, season: Int): Flow<List<com.vidora.app.data.remote.Episode>> = flow {
        emit(tmdbService.getSeasonEpisodes(tvId, season).episodes)
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

    suspend fun updateHistory(media: MediaItem, season: Int? = null, episode: Int? = null) {
        mediaDao.updateHistory(
            com.vidora.app.data.local.HistoryEntity(
                id = media.id,
                title = media.displayTitle,
                posterPath = media.posterPath,
                mediaType = media.realMediaType,
                season = season,
                episode = episode
            )
        )
    }
}
