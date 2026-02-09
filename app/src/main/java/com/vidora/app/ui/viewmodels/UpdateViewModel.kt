package com.vidora.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import javax.inject.Inject

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String
)

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val error: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState
    
    private val currentVersionCode = 1  // Update this with each release
    private val githubRepo = "Puneeth-R-140/pocket-nexus-streaming"
    
    fun checkForUpdates() {
        _uiState.update { it.copy(isChecking = true, error = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$githubRepo/releases/latest"
                val response = URL(url).readText()
                val json = org.json.JSONObject(response)
                
                val tagName = json.getString("tag_name") // e.g., "v1.2.0"
                val versionCode = tagName.replace("v", "").replace(".", "").toIntOrNull() ?: 0
                val releaseNotes = json.optString("body", "No release notes available")
                val downloadUrl = json.getString("html_url")
                
                if (versionCode > currentVersionCode) {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            updateAvailable = true,
                            updateInfo = UpdateInfo(
                                versionName = tagName,
                                versionCode = versionCode,
                                releaseNotes = releaseNotes,
                                downloadUrl = downloadUrl
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isChecking = false, updateAvailable = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateCheck", "Failed to check for updates", e)
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        error = "Failed to check for updates: ${e.message}"
                    )
                }
            }
        }
    }
}
