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
    val favorites: List<com.vidora.app.data.local.FavoriteEntity> = emptyList(),
    val history: List<com.vidora.app.data.local.HistoryEntity> = emptyList(),
    val actionMovies: List<MediaItem> = emptyList(),
    val comedyMovies: List<MediaItem> = emptyList(),
    val horrorMovies: List<MediaItem> = emptyList(),
    val scifiMovies: List<MediaItem> = emptyList(),
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
            
            // Launch both requests in parallel using async
            val moviesDeferred = async {
                var moviesResult: NetworkResult<List<MediaItem>>? = null
                repository.getTrendingMovies().collect { result ->
                    moviesResult = result
                }
                moviesResult
            }
            
            val tvShowsDeferred = async {
                var tvResult: NetworkResult<List<MediaItem>>? = null
                repository.getTrendingTVShows().collect { result ->
                    tvResult = result
                }
                tvResult
            }
            
            // Fetch genre-based content (Action=28, Comedy=35, Horror=27, Sci-Fi=878)
            val actionDeferred = async {
                var result: NetworkResult<List<MediaItem>>? = null
                repository.getMoviesByGenre(28).collect { r -> result = r }
                result
            }
            
            val comedyDeferred = async {
                var result: NetworkResult<List<MediaItem>>? = null
                repository.getMoviesByGenre(35).collect { r -> result = r }
                result
            }
            
            val horrorDeferred = async {
                var result: NetworkResult<List<MediaItem>>? = null
                repository.getMoviesByGenre(27).collect { r -> result = r }
                result
            }
            
            val scifiDeferred = async {
                var result: NetworkResult<List<MediaItem>>? = null
                repository.getMoviesByGenre(878).collect { r -> result = r }
                result
            }
            
            // Wait for both to complete
            val moviesResult = moviesDeferred.await()
            val tvResult = tvShowsDeferred.await()
            val actionResult = actionDeferred.await()
            val comedyResult = comedyDeferred.await()
            val horrorResult = horrorDeferred.await()
            val scifiResult = scifiDeferred.await()
            
            // Update UI based on results
            val hasError = moviesResult is NetworkResult.Error || tvResult is NetworkResult.Error
            val errorMessage = when {
                moviesResult is NetworkResult.Error -> moviesResult.message
                tvResult is NetworkResult.Error -> tvResult.message
                else -> null
            }
            
            _uiState.update {
                it.copy(
                    trendingMovies = if (moviesResult is NetworkResult.Success) moviesResult.data else emptyList(),
                    popularShows = if (tvResult is NetworkResult.Success) tvResult.data else emptyList(),
                    actionMovies = if (actionResult is NetworkResult.Success) actionResult.data else emptyList(),
                    comedyMovies = if (comedyResult is NetworkResult.Success) comedyResult.data else emptyList(),
                    horrorMovies = if (horrorResult is NetworkResult.Success) horrorResult.data else emptyList(),
                    scifiMovies = if (scifiResult is NetworkResult.Success) scifiResult.data else emptyList(),
                    isLoading = false,
                    error = errorMessage,
                    canRetry = hasError
                )
            }
        }
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
