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
    val historyMap: Map<String, com.vidora.app.data.local.HistoryEntity> = emptyMap(),
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
            repository.getFavorites()
                .distinctUntilChanged()
                .collect { favs ->
                    _uiState.update { it.copy(favorites = favs) }
                }
        }
        viewModelScope.launch {
            repository.getWatchHistory()
                .distinctUntilChanged()
                .collect { history ->
                    // Map of mediaId -> (the most recent history entity for that media)
                    val map = history.associateBy { it.mediaId }
                    _uiState.update { it.copy(history = history, historyMap = map) }
                }
        }
    }

    fun loadHomeContent() {
        val current = _uiState.value
        if (current.trendingMovies.isNotEmpty()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        loadSection("Trending Movies") { repository.getTrendingMovies() }
        loadSection("Popular TV") { repository.getTrendingTVShows() }
        loadSection("Action") { repository.getMoviesByGenre("28") }
        loadSection("Comedy") { repository.getMoviesByGenre("35") }
        loadSection("Sci-Fi") { repository.getMoviesByGenre("878") }
        loadSection("Drama") { repository.getTvShowsByGenre("18") }
        loadSection("Animation") { repository.getTvShowsByGenre("16") }
        loadSection("Documentaries") { repository.getTvShowsByGenre("99") }
    }

    private fun loadSection(name: String, flowProvider: () -> Flow<NetworkResult<List<MediaItem>>>) {
        viewModelScope.launch {
            flowProvider().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update { state ->
                            val newState = when (name) {
                                "Trending Movies" -> state.copy(trendingMovies = result.data)
                                "Popular TV" -> state.copy(popularShows = result.data)
                                "Action" -> state.copy(actionMovies = result.data)
                                "Comedy" -> state.copy(comedyMovies = result.data)
                                "Sci-Fi" -> state.copy(scifiMovies = result.data)
                                "Drama" -> state.copy(dramaShows = result.data)
                                "Animation" -> state.copy(animationShows = result.data)
                                "Documentaries" -> state.copy(documentaries = result.data)
                                else -> state
                            }
                            // Only hide loading if we have some data
                            newState.copy(isLoading = false, error = null)
                        }
                    }
                    is NetworkResult.Error -> {
                        // Only show error if we have no data at all
                        if (_uiState.value.trendingMovies.isEmpty() && _uiState.value.popularShows.isEmpty()) {
                            _uiState.update { it.copy(isLoading = false, error = result.message, canRetry = true) }
                        }
                    }
                    is NetworkResult.Loading -> {
                        // Handled by initial isLoading = true
                    }
                }
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
