package com.vidora.app.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CustomPlayerControls(
    player: ExoPlayer,
    visible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onShowQualityDialog: () -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var bufferedPercentage by remember { mutableStateOf(0) }
    
    // Update player state
    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0)
            isPlaying = player.isPlaying
            bufferedPercentage = player.bufferedPercentage
            delay(100) // Update every 100ms
        }
    }
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(visible, isPlaying) {
        if (visible && isPlaying) {
            delay(3000)
            onVisibilityChange(false)
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            ) {
                // Top controls
                TopControls(
                    onBack = onBack,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Center play/pause button
            CenterPlayPauseButton(
                isPlaying = isPlaying,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Bottom gradient overlay with controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            ) {
                BottomControls(
                    player = player,
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPercentage = bufferedPercentage,
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    onSeek = { position ->
                        player.seekTo(position)
                    },
                    onShowQualityDialog = onShowQualityDialog,
                    onShowSubtitleDialog = onShowSubtitleDialog,
                    onShowSpeedDialog = onShowSpeedDialog,
                    onNextEpisode = onNextEpisode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TopControls(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun CenterPlayPauseButton(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onPlayPause,
        modifier = modifier.size(72.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun BottomControls(
    player: ExoPlayer,
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onShowQualityDialog: () -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Seekbar with time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            CustomSeekbar(
                currentPosition = currentPosition,
                duration = duration,
                bufferedPercentage = bufferedPercentage,
                onSeek = onSeek,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Bottom button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Next Episode Button (only visible if callback provided)
            if (onNextEpisode != null) {
                TextButton(
                    onClick = onNextEpisode,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Episode",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Next Ep",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quality button
                IconButton(onClick = onShowQualityDialog) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quality",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Subtitle button
                IconButton(onClick = onShowSubtitleDialog) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Speed button
                IconButton(onClick = onShowSpeedDialog) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSeekbar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // State for preview during scrubbing
    var isSeeking by remember { mutableStateOf(false) }
    var previewPosition by remember { mutableStateOf(0f) }
    
    val displayProgress = if (isSeeking) {
        previewPosition
    } else {
        if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
    
    Slider(
        value = displayProgress,
        onValueChange = { newProgress ->
            isSeeking = true
            previewPosition = newProgress
        },
        onValueChangeFinished = {
            // Only seek when user releases
            val newPosition = (previewPosition * duration).toLong()
            onSeek(newPosition)
            isSeeking = false
        },
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

private fun formatTime(millis: Long): String {
    if (millis < 0) return "0:00"
    val totalSeconds = (millis / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
