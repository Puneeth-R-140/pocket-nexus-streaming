package com.vidora.app.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.data.repository.MediaRepository
import com.vidora.app.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val media: MediaItem? = null,
    val episodes: List<com.vidora.app.data.remote.Episode> = emptyList(),
    val recommendations: List<MediaItem> = emptyList(),
    val currentSeason: Int = 1,
    val isFavorite: Boolean = false,
    val imdbRating: String? = null,
    val playbackProgress: PlaybackProgress? = null,
    val preloadedStreamUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false
)

data class PlaybackProgress(
    val positionMs: Long,
    val durationMs: Long,
    val season: Int? = null,
    val episode: Int? = null
) {
    val progressPercent: Int get() = ((positionMs.toDouble() / durationMs) * 100).toInt()
    val timeRemaining: String get() {
        val remaining = (durationMs - positionMs) / 1000 / 60
        return if (remaining > 0) "$remaining min left" else "Almost done"
    }
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState
    
    private var preloadJob: Job? = null

    init {
        val id: String = savedStateHandle["id"] ?: ""
        val type: String = savedStateHandle["type"] ?: "movie"
        if (id.isNotEmpty()) {
            loadDetails(type, id)
            loadPlaybackHistory(id)
        }
    }

    private fun loadDetails(type: String, id: String) {
        viewModelScope.launch {
            repository.getDetails(type, id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null, canRetry = false) }
                    }
                    is NetworkResult.Success -> {
                        val isFav = repository.isFavorite(id)
                        _uiState.update { 
                            it.copy(
                                media = result.data, 
                                isFavorite = isFav, 
                                isLoading = false,
                                error = null
                            ) 
                        }
                        // Fetch IMDb rating if available
                        result.data.imdbId?.let { imdbId ->
                            loadImdbRating(imdbId)
                        }
                        if (type == "tv") {
                            loadEpisodes(id, 1)
                        }
                        loadRecommendations(type, id)
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = result.message,
                                canRetry = true
                            ) 
                        }
                    }
                }
            }
        }
    }

    fun loadEpisodes(id: String, season: Int) {
        viewModelScope.launch {
            repository.getEpisodes(id, season).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                episodes = result.data, 
                                currentSeason = season,
                                isLoading = false,
                                error = null
                            ) 
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = result.message,
                                canRetry = true
                            ) 
                        }
                    }
                }
            }
        }
    }

    fun loadRecommendations(type: String, id: String) {
        viewModelScope.launch {
            repository.getRecommendations(type, id).collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(recommendations = result.data) }
                }
            }
        }
    }

    fun retry() {
        val mediaId = _uiState.value.media?.id
        val mediaType = _uiState.value.media?.realMediaType ?: "movie"
        if (mediaId != null) {
            loadDetails(mediaType, mediaId)
        }
    }

    fun markWatched(media: MediaItem, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            repository.updateHistory(media, 0L, 0L, season, episode)
        }
    }
    
    private fun loadPlaybackHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val history = repository.getHistoryItem(id)
            history?.let {
                // Only show progress if user has watched > 5% and < 95%
                val progressPercent = if (it.durationMs > 0) {
                    ((it.positionMs.toDouble() / it.durationMs) * 100).toInt()
                } else 0
                
                if (progressPercent in 5..95) {
                    _uiState.update { state ->
                        state.copy(
                            playbackProgress = PlaybackProgress(
                                positionMs = it.positionMs,
                                durationMs = it.durationMs,
                                season = it.season,
                                episode = it.episode
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadImdbRating(imdbId: String) {
        viewModelScope.launch {
            val ratings = repository.getRatings(imdbId)
            ratings?.imdbRating?.let { rating ->
                _uiState.update { it.copy(imdbRating = rating) }
            }
        }
    }
    
    // Pre-load stream URL when Details Screen opens
    fun startPreloading() {
        val media = _uiState.value.media ?: return
        
        // Cancel any existing pre-load job
        preloadJob?.cancel()
        
        preloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Construct stream URL (doesn't actually fetch yet, just prepares)
                val streamUrl = when {
                    media.realMediaType == "tv" -> {
                        // For TV shows, pre-load S01E01
                        "https://vidsrc.xyz/embed/tv/${media.id}/1/1"
                    }
                    else -> {
                        // For movies
                        "https://vidsrc.xyz/embed/movie/${media.id}"
                    }
                }
                
                // Store pre-loaded URL
                _uiState.update { it.copy(preloadedStreamUrl = streamUrl) }
                
                // Note: In production, you could use a headless WebView here
                // to actually fetch the stream link, but that requires more setup
            } catch (e: Exception) {
                // Silent fail - doesn't affect user experience
            }
        }
    }
    
    // Cancel pre-loading if user leaves Details Screen (minimal data usage!)
    fun cancelPreload() {
        preloadJob?.cancel()
        preloadJob = null
        _uiState.update { it.copy(preloadedStreamUrl = null) }
    }

    fun toggleFavorite() {
        val media = _uiState.value.media ?: return
        viewModelScope.launch {
            repository.toggleFavorite(media)
            _uiState.emit(_uiState.value.copy(isFavorite = !(_uiState.value.isFavorite)))
        }
    }
}
