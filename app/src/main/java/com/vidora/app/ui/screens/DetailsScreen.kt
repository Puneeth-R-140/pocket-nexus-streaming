package com.vidora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
                        model = "https://image.tmdb.org/t/p/original${media.backdropPath}",
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
                    
                    // Ratings Row
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // TMDB Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TMDB: ",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.1f", media.voteAverage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/10",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        
                        // IMDb Rating
                        uiState.imdbRating?.let { imdbRating ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IMDb: ",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = imdbRating,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF5C518) // IMDb yellow
                                )
                                Text(
                                    text = "/10",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Continue Watching button (if progress exists)
                    uiState.playbackProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FilledTonalButton(
                            onClick = {
                                // Construct URL for resuming
                                val resumeUrl = if (progress.season != null && progress.episode != null) {
                                    // TV show - resume specific episode
                                    "https://watch.vidora.su/watch/tv/${media.id}/${progress.season}/${progress.episode}"
                                } else {
                                    // Movie - resume movie
                                    "https://watch.vidora.su/watch/movie/${media.id}"
                                }
                                onWatchClick(media.id, media.realMediaType, resumeUrl)
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
                    
                    Button(
                        onClick = { 
                            if (media.realMediaType == "movie") {
                                viewModel.markWatched(media)
                            }
                            val finalUrl = when {
                                media.realMediaType == "tv" -> "https://watch.vidora.su/watch/tv/${media.id}/1/1"
                                else -> "https://watch.vidora.su/watch/movie/${media.id}"
                            }
                            onWatchClick(media.id, media.realMediaType, finalUrl)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Now", fontWeight = FontWeight.Bold)
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
                                EpisodeItem(episode = episode) {
                                    viewModel.markWatched(media, episode.seasonNumber, episode.episodeNumber)
                                    val episodeUrl = "https://watch.vidora.su/watch/tv/${media.id}/${episode.seasonNumber}/${episode.episodeNumber}"
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${episode.stillPath}",
                contentDescription = null,
                modifier = Modifier.width(100.dp).height(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
