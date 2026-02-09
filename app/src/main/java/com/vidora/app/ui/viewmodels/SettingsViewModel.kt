package com.vidora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidora.app.data.local.SettingsEntity
import com.vidora.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val isLoading: Boolean = false,
    val cacheCleared: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val settings = repository.getSettings()
                _uiState.update { 
                    it.copy(
                        settings = settings,
                        isLoading = false,
                        error = null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    ) 
                }
            }
        }
    }

    fun updatePreferredQuality(quality: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(preferredQuality = quality)
            repository.updateSettings(updated)
            _uiState.update { it.copy(settings = updated) }
        }
    }

    fun updateAutoPlayNextEpisode(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(autoPlayNextEpisode = enabled)
            repository.updateSettings(updated)
            _uiState.update { it.copy(settings = updated) }
        }
    }

    fun updateDefaultSubtitleLanguage(language: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(defaultSubtitleLanguage = language)
            repository.updateSettings(updated)
            _uiState.update { it.copy(settings = updated) }
        }
    }

    fun updateDownloadQuality(quality: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(downloadQuality = quality)
            repository.updateSettings(updated)
            _uiState.update { it.copy(settings = updated) }
        }
    }

    fun clearCache(cacheDir: java.io.File) {
        viewModelScope.launch {
            try {
                // Clear Coil image cache
                cacheDir.resolve("image_cache").deleteRecursively()
                
                // Clear OkHttp cache
                cacheDir.resolve("http_cache").deleteRecursively()
                
                _uiState.update { it.copy(cacheCleared = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear cache: ${e.message}") }
            }
        }
    }

    fun resetCacheClearedFlag() {
        _uiState.update { it.copy(cacheCleared = false) }
    }
}
