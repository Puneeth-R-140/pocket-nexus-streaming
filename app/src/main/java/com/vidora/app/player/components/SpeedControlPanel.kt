package com.vidora.app.player.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.exoplayer2.ExoPlayer

@Composable
fun SpeedControlPanel(
    player: ExoPlayer,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    var selectedSpeed by remember { mutableStateOf(player.playbackParameters.speed) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 300.dp)
                    .heightIn(max = 500.dp) // Add max height
            ) {
                Text(
                    text = "Playback Speed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Make scrollable to show all speeds
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    speeds.forEach { speed ->
                        SpeedOption(
                            speed = speed,
                            isSelected = (speed == selectedSpeed),
                            onClick = {
                                player.setPlaybackSpeed(speed)
                                selectedSpeed = speed
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedOption(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayText = when {
        speed == 1f -> "Normal"
        speed == speed.toInt().toFloat() -> "${speed.toInt()}x" // Show "2x" for 2.0
        else -> "${speed}x"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            fontSize = 16.sp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
