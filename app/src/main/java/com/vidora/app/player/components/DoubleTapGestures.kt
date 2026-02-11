package com.vidora.app.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SeekDirection {
    FORWARD, BACKWARD
}

data class DoubleTapState(
    val direction: SeekDirection,
    val count: Int
)

@Composable
fun DoubleTapControls(
    player: ExoPlayer,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doubleTapState by remember { mutableStateOf<DoubleTapState?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var tapCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // State for long-press 2x speed
    var isLongPressing by remember { mutableStateOf(false) }
    var originalSpeed by remember { mutableStateOf(1f) }
    
    // Reset long press if player stops
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            originalSpeed = player.playbackParameters.speed
            player.setPlaybackSpeed(2f)
        } else {
            if (player.playbackParameters.speed == 2f) {
                player.setPlaybackSpeed(originalSpeed)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        val x = offset.x
                        
                        when {
                            x < screenWidth * 0.35f -> {
                                // Left side - seek backward
                                tapCount++
                                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                if (player.playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) {
                                    player.play()
                                }
                                doubleTapState = DoubleTapState(SeekDirection.BACKWARD, tapCount)
                            }
                            x > screenWidth * 0.65f -> {
                                // Right side - seek forward
                                tapCount++
                                player.seekTo(player.currentPosition + 10000)
                                if (player.playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) {
                                    player.play()
                                }
                                doubleTapState = DoubleTapState(SeekDirection.FORWARD, tapCount)
                            }
                        }
                    },
                    onTap = { offset ->
                        // Standard single tap toggle (handled by system with delay to wait for double tap)
                        onToggleControls()
                    },
                    onLongPress = {
                        // Long press anywhere - 2x speed
                        isLongPressing = true
                    },
                    onPress = {
                        // Detect when finger is released
                        tryAwaitRelease()
                        isLongPressing = false
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left zone for backward seek
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Ripple animation for backward
                androidx.compose.animation.AnimatedVisibility(
                    visible = doubleTapState?.direction == SeekDirection.BACKWARD,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    SeekFeedback(
                        direction = SeekDirection.BACKWARD,
                        count = doubleTapState?.count ?: 0,
                        onAnimationEnd = {
                            doubleTapState = null
                            tapCount = 0
                        }
                    )
                }
            }
            
            // Center zone (empty, just for tap detection)
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Right zone for forward seek
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Ripple animation for forward
                androidx.compose.animation.AnimatedVisibility(
                    visible = doubleTapState?.direction == SeekDirection.FORWARD,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    SeekFeedback(
                        direction = SeekDirection.FORWARD,
                        count = doubleTapState?.count ?: 0,
                        onAnimationEnd = {
                            doubleTapState = null
                            tapCount = 0
                        }
                    )
                }
            }
        }
        
        // 2x Speed Indicator (top-right corner when long-pressing)
        androidx.compose.animation.AnimatedVisibility(
            visible = isLongPressing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "2x",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SeekFeedback(
    direction: SeekDirection,
    count: Int,
    onAnimationEnd: () -> Unit
) {
    LaunchedEffect(count) {
        delay(800)
        onAnimationEnd()
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(24.dp)
    ) {
        Icon(
            imageVector = if (direction == SeekDirection.BACKWARD) 
                Icons.Default.FastRewind 
            else 
                Icons.Default.FastForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${count * 10} sec",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// Circular ripple effect overlay
@Composable
fun RippleEffect(
    visible: Boolean,
    position: Offset,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.5f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ripple_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.3f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ripple_alpha"
    )
    
    if (alpha > 0f) {
        Box(
            modifier = modifier
                .offset(x = position.x.dp, y = position.y.dp)
                .size((100 * scale).dp)
                .background(
                    color = Color.White.copy(alpha = alpha),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}
