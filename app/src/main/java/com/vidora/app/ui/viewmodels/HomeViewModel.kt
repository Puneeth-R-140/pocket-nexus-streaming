package com.vidora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
import com.vidora.app.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

data class HomeUiState(
    val trendingMovies: List<MediaItem> = emptyList(),
    val popularShows: List<MediaItem> = emptyList(),
    val actionMovies: List<MediaItem> = emptyList(),
    val comedyMovies: List<MediaItem> = emptyList(),
    val scifiMovies: List<MediaItem> = emptyList(),
    val dramaShows: List<MediaItem> = emptyList(),
    val animationShows: List<MediaItem> = emptyList(),
    val documentaries: List<MediaItem> = emptyList(),
    val favorites: List<com.vidora.app.data.local.FavoriteEntity> = emptyList(),
    val history: List<com.vidora.app.data.local.HistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHomeContent()
        observeLocalData()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            repository.getFavorites().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            repository.getWatchHistory().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }
            
            // Launch all requests in parallel
            val moviesDeferred = async { getList { repository.getTrendingMovies() } }
            val tvDeferred = async { getList { repository.getTrendingTVShows() } }
            val actionDeferred = async { getList { repository.getMoviesByGenre("28") } }
            val comedyDeferred = async { getList { repository.getMoviesByGenre("35") } }
            val scifiDeferred = async { getList { repository.getMoviesByGenre("878") } }
            val dramaDeferred = async { getList { repository.getTvShowsByGenre("18") } }
            val animeDeferred = async { getList { repository.getTvShowsByGenre("16") } }
            val docDeferred = async { getList { repository.getTvShowsByGenre("99") } }
            
            val moviesResult = moviesDeferred.await()
            val tvResult = tvDeferred.await()
            val actionResult = actionDeferred.await()
            val comedyResult = comedyDeferred.await()
            val scifiResult = scifiDeferred.await()
            val dramaResult = dramaDeferred.await()
            val animeResult = animeDeferred.await()
            val docResult = docDeferred.await()
            
            val hasError = moviesResult == null && tvResult == null 
            
            _uiState.update {
                it.copy(
                    trendingMovies = moviesResult ?: emptyList(),
                    popularShows = tvResult ?: emptyList(),
                    actionMovies = actionResult ?: emptyList(),
                    comedyMovies = comedyResult ?: emptyList(),
                    scifiMovies = scifiResult ?: emptyList(),
                    dramaShows = dramaResult ?: emptyList(),
                    animationShows = animeResult ?: emptyList(),
                    documentaries = docResult ?: emptyList(),
                    isLoading = false,
                    error = if (hasError) "Failed to load content" else null,
                    canRetry = hasError
                )
            }
        }
    }
    
    private suspend fun getList(call: () -> Flow<NetworkResult<List<MediaItem>>>): List<MediaItem>? {
        var resultList: List<MediaItem>? = null
        call().collect { result ->
            if (result is NetworkResult.Success) {
                resultList = result.data
            }
        }
        return resultList
    }
    
    fun retry() {
        loadHomeContent()
    }
}

fun FavoriteEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        title = title,
        name = null,
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
}

fun HistoryEntity.toMediaItem(): MediaItem {
    return MediaItem(
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
}
