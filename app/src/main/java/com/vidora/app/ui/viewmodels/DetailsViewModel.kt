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
    val playbackProgress: PlaybackProgress? = null,
    val isMovieWatched: Boolean = false,
    val watchedEpisodes: Set<String> = emptySet(),
    val preloadedStreamUrl: String? = null,
    val selectedServer: String = "hexa", // hexa, beta, auto, animepahe
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
            observeWatchStatus(id)
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
            // Pass identical non-zero values so progress calculates as 100%
            repository.updateHistory(media, 100L, 100L, season, episode)
        }
    }
    
    fun updateServer(server: String) {
        _uiState.update { it.copy(selectedServer = server) }
        // If we have a media item, restart preloading with the new server
        startPreloading()
    }
    
    private fun loadPlaybackHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val history = repository.getMostRecentHistoryItem(id)
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

    private fun observeWatchStatus(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllHistoryForMedia(id).collect { historyList ->
                val watchedEps = mutableSetOf<String>()
                var movieWatched = false
                
                for (item in historyList) {
                    val progressPercent = if (item.durationMs > 0) {
                        ((item.positionMs.toDouble() / item.durationMs) * 100).toInt()
                    } else 0
                    
                    if (progressPercent >= 90) {
                        if (item.season != null && item.episode != null) {
                            watchedEps.add("S${item.season}E${item.episode}")
                        } else {
                            movieWatched = true
                        }
                    }
                }
                
                _uiState.update { state ->
                    state.copy(
                        isMovieWatched = movieWatched,
                        watchedEpisodes = watchedEps
                    )
                }
            }
        }
    }

    // Ratings logic removed
    

    // VidNest Provider URL Construction
    fun getVidNestUrl(type: String, id: String, season: Int? = null, episode: Int? = null, forcedServer: String? = null): String {
        val server = forcedServer ?: _uiState.value.selectedServer
        val serverParam = if (server != "auto") "?server=$server" else ""
        
        return when {
            type == "movie" -> 
                "https://vidnest.fun/movie/$id$serverParam"
            type == "tv" -> 
                "https://vidnest.fun/tv/$id/${season ?: 1}/${episode ?: 1}$serverParam"
            else -> 
                "https://vidnest.fun/movie/$id$serverParam"
        }
    }
    
    // Pre-load stream URL when Details Screen opens
    fun startPreloading() {
        val media = _uiState.value.media ?: return
        
        // Cancel any existing pre-load job
        preloadJob?.cancel()
        
        preloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Construct VidNest stream URL (primary provider)
                val streamUrl = getVidNestUrl(media.realMediaType, media.id)
                
                // Store pre-loaded URL
                _uiState.update { it.copy(preloadedStreamUrl = streamUrl) }
            } catch (e: Exception) {
                // Silent fail
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
