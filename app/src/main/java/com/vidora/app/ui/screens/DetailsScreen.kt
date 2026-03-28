package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import coil.compose.AsyncImage
import com.vidora.app.data.remote.MediaItem
import com.vidora.app.ui.viewmodels.DetailsViewModel

import com.vidora.app.ui.components.ErrorStateView
import com.vidora.app.ui.components.ShimmerCard
import com.vidora.app.ui.components.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onWatchClick: (String, String, String) -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val media = uiState.media
    
    // Pre-load stream when screen opens, cancel when user leaves (minimal data!)
    DisposableEffect(media) {
        if (media != null) {
            viewModel.startPreloading()
        }
        onDispose {
            viewModel.cancelPreload()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading && media == null) {
            ShimmerDetailsScreen()
        } else if (uiState.error != null && media == null) {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        } else if (media != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w780${media.backdropPath}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = media.displayTitle,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (uiState.isFavorite) 
                                    Icons.Filled.Favorite 
                                else 
                                    Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) Color.Red else Color.White
                            )
                        }
                    }
                    
                    // Metadata row: Runtime, Year, Genres, Ratings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Runtime
                        media.runtimeMinutes?.let { minutes ->
                            val hours = minutes / 60
                            val mins = minutes % 60
                            val runtimeText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Text(
                                text = runtimeText,
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Text("•", fontSize = 13.sp, color = Color.Gray)
                        }
                        
                        // Year
                        media.releaseYear?.let { year ->
                            Text(
                                text = year,
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Text("•", fontSize = 13.sp, color = Color.Gray)
                        }
                        
                        // Media Type
                        Text(
                            text = media.realMediaType.uppercase(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Movie Watched Indicator
                        if (media.realMediaType == "movie" && uiState.isMovieWatched) {
                            Text("•", fontSize = 13.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Watched",
                                    tint = Color(0xFF4CAF50), // Green
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Watched",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Genres
                    media.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.take(3).forEach { genre ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = genre.name,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    
                    // Ratings removed at user request
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Continue Watching button (if progress exists)
                    uiState.playbackProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FilledTonalButton(
                            onClick = {
                                val url = viewModel.getVidNestUrl(
                                    media.realMediaType, 
                                    media.id, 
                                    progress.season, 
                                    progress.episode
                                )
                                onWatchClick(media.id, media.realMediaType, url)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Continue Watching",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${progress.progressPercent}% watched • ${progress.timeRemaining}",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Stream Server",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val servers = listOf("auto", "hexa", "beta")
                            
                        servers.forEach { server ->
                            val isSelected = uiState.selectedServer == server
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateServer(server) },
                                label = { 
                                    Text(
                                        text = server.uppercase(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            if (media.realMediaType == "movie") {
                                viewModel.markWatched(media)
                            }
                            val finalUrl = viewModel.getVidNestUrl(media.realMediaType, media.id)
                            onWatchClick(media.id, media.realMediaType, finalUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedServer == "auto") "Watch Now (Auto)" else "Watch on ${uiState.selectedServer.uppercase()}", 
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = media.overview ?: "No description available.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.LightGray
                    )

                    // Cast & Crew Section
                    media.credits?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Cast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cast.take(10)) { castMember ->
                                Column(
                                    modifier = Modifier.width(100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = castMember.profilePath?.let { 
                                            "https://image.tmdb.org/t/p/w185$it" 
                                        },
                                        contentDescription = castMember.name,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .background(Color.DarkGray, MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = castMember.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 2,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = castMember.character,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (media.realMediaType == "tv") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            val totalSeasons = media.totalSeasons
                            if (totalSeasons > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items((1..totalSeasons).toList()) { seasonNum ->
                                        FilterChip(
                                            selected = uiState.currentSeason == seasonNum,
                                            onClick = { viewModel.loadEpisodes(media.id, seasonNum) },
                                            label = { Text("S$seasonNum") }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (uiState.episodes.isEmpty() && uiState.isLoading) {
                            Column {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .padding(vertical = 4.dp)
                                            .shimmerEffect()
                                    )
                                }
                            }
                        } else if (uiState.episodes.isEmpty() && uiState.error != null) {
                            TextButton(onClick = { 
                                media?.let { viewModel.loadEpisodes(it.id, uiState.currentSeason) }
                            }) {
                                Text("Click to retry loading episodes", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            uiState.episodes.forEach { episode ->
                                val isWatched = uiState.watchedEpisodes.contains("S${episode.seasonNumber}E${episode.episodeNumber}")
                                EpisodeItem(
                                    episode = episode,
                                    isWatched = isWatched
                                ) {
                                    viewModel.markWatched(media, episode.seasonNumber, episode.episodeNumber)
                                    val episodeUrl = viewModel.getVidNestUrl(
                                        type = "tv", 
                                        id = media.id, 
                                        season = episode.seasonNumber, 
                                        episode = episode.episodeNumber
                                    )
                                    onWatchClick(media.id, "tv", episodeUrl)
                                }
                            }
                        }
                    }

                    if (uiState.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "You Might Also Like",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recommendations) { recommendation ->
                                com.vidora.app.ui.components.MediaCard(
                                    item = recommendation,
                                    onClick = { onMediaClick(recommendation) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else if (uiState.error != null && media == null) {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        }
    }
}

@Composable
fun ShimmerDetailsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .shimmerEffect()
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(32.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(20.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(24.dp)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeItem(
    episode: com.vidora.app.data.remote.Episode,
    isWatched: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w185${episode.stillPath}",
                contentDescription = null,
                modifier = Modifier.width(100.dp).height(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${episode.episodeNumber}. ${episode.name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isWatched) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Watched",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = episode.overview ?: "",
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = Color.Gray
                )
            }
        }
    }
}
