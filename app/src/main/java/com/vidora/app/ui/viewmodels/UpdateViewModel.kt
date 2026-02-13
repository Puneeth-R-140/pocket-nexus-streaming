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
class UpdateViewModel @Inject constructor(
    private val okHttpClient: okhttp3.OkHttpClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState
    
    private val githubRepo = "Puneeth-R-140/pocket-nexus-streaming"
    
    fun checkForUpdates() {
        _uiState.update { it.copy(isChecking = true, error = null) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use OkHttp for better network handling and timeouts
                val request = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/$githubRepo/releases/latest")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("GitHub API Error: ${response.code}")
                }
                
                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val json = org.json.JSONObject(responseBody)
                
                val tagName = json.getString("tag_name") // e.g., "v1.2.0"
                val releaseNotes = json.optString("body", "No release notes available")
                val downloadUrl = json.getString("html_url")
                
                // Get current app version from BuildConfig
                val currentVersionName = com.vidora.app.BuildConfig.VERSION_NAME
                
                Log.d("UpdateCheck", "Current: $currentVersionName, Latest: $tagName")
                
                if (isNewerVersion(currentVersionName, tagName)) {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            updateAvailable = true,
                            updateInfo = UpdateInfo(
                                versionName = tagName,
                                versionCode = 0, // Not used for comparison anymore
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

    private fun isNewerVersion(current: String, latest: String): Boolean {
        fun parseVersion(version: String): List<Int> {
            return version.replace("v", "")
                .split(".")
                .mapNotNull { it.toIntOrNull() }
        }

        val currentParts = parseVersion(current)
        val latestParts = parseVersion(latest)

        val length = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until length) {
            val v1 = currentParts.getOrElse(i) { 0 }
            val v2 = latestParts.getOrElse(i) { 0 }
            
            if (v2 > v1) return true
            if (v2 < v1) return false
        }
        
        return false
    }
}
