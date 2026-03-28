package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.ui.viewmodels.HomeViewModel
import com.vidora.app.ui.components.ErrorStateView
import com.vidora.app.ui.components.ShimmerCard
import com.vidora.app.ui.components.EmptyStateView
import com.vidora.app.ui.components.MediaCard
import com.vidora.app.ui.components.shimmerEffect

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Memoize mapped lists and filter out any accidental animations
    val historyAsItems = remember(uiState.history) { 
        uiState.history.map { it.toMediaItem() }
            .filter { item -> item.genres?.any { it.id == 16 || it.name.contains("Animation", ignoreCase = true) } != true }
    }
    val favoritesAsItems = remember(uiState.favorites) { 
        uiState.favorites.map { it.toMediaItem() }
            .filter { item -> item.genres?.any { it.id == 16 || it.name.contains("Animation", ignoreCase = true) } != true }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading && uiState.trendingMovies.isEmpty()) {
            ShimmerHomeScreen(onSearchClick, onSettingsClick)
        } else if (uiState.error != null && uiState.trendingMovies.isEmpty()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ErrorStateView(
                    message = (uiState.error ?: "Unknown error"),
                    onRetry = { viewModel.retry() }
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(contentType = "header") { 
                    HeaderSection(
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick
                    ) 
                }
                
                item(contentType = "notice") {
                    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
                    val primary = MaterialTheme.colorScheme.primary
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Use HEXA or BETA for multiple quality options. Use AUTO if both fail.",
                                    fontSize = 11.sp,
                                    color = onSecondaryContainer,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                        }
                    }
                }

                if (historyAsItems.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Continue Watching",
                            items = historyAsItems,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }

                if (favoritesAsItems.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "My Favorites",
                            items = favoritesAsItems,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.trendingMovies.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Trending Movies",
                            items = uiState.trendingMovies,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.popularShows.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Popular TV Shows",
                            items = uiState.popularShows,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.actionMovies.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Action Movies",
                            items = uiState.actionMovies,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.comedyMovies.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Comedy Movies",
                            items = uiState.comedyMovies,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.scifiMovies.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Sci-Fi & Fantasy",
                            items = uiState.scifiMovies,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.dramaShows.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Drama Series",
                            items = uiState.dramaShows,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                
                if (uiState.documentaries.isNotEmpty()) {
                    item(contentType = "section") {
                        MediaSection(
                            title = "Documentaries",
                            items = uiState.documentaries,
                            onMediaClick = onMediaClick,
                            historyMap = uiState.historyMap
                        )
                    }
                }
                
                if (uiState.trendingMovies.isEmpty() && uiState.popularShows.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyStateView(message = "No content available right now.")
                    }
                }

                if (uiState.error != null) {
                    item {
                        Surface(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.error ?: "",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { viewModel.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerHomeScreen(onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeaderSection(onSearchClick = onSearchClick, onSettingsClick = onSettingsClick)
        
        repeat(3) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .width(150.dp)
                        .height(24.dp)
                        .shimmerEffect()
                )
                
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        ShimmerCard()
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "POCKET NEXUS",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
            Text(
                text = "By GHOST",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Row {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun MediaSection(
    title: String,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    historyMap: Map<String, com.vidora.app.data.local.HistoryEntity> = emptyMap()
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = items.size,
                key = { index -> items[index].id },
                contentType = { "media_card" }
            ) { index ->
                val item = items[index]
                val history = historyMap[item.id]
                val progress = if (history != null && history.durationMs > 0) {
                    history.positionMs.toFloat() / history.durationMs.toFloat()
                } else null
                
                MediaCard(
                    item = item, 
                    onClick = { onMediaClick(item) },
                    progress = progress
                )
            }
        }
    }
}


