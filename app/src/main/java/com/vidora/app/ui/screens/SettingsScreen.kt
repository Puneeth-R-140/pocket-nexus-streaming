package com.vidora.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidora.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onGestureGuideClick: () -> Unit = {},
    onCheckUpdatesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }

    // Show toast when cache is cleared
    LaunchedEffect(uiState.cacheCleared) {
        if (uiState.cacheCleared) {
            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            viewModel.resetCacheClearedFlag()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Playback Section
            SettingsSection(title = "Playback") {
                PreferenceDropdown(
                    title = "Preferred Quality",
                    options = listOf("Auto", "720p", "1080p"),
                    selectedOption = uiState.settings.preferredQuality,
                    onOptionSelected = { viewModel.updatePreferredQuality(it) }
                )
                
                Divider()
                
                PreferenceSwitch(
                    title = "Auto-play Next Episode",
                    subtitle = "Automatically play next episode for TV shows",
                    checked = uiState.settings.autoPlayNextEpisode,
                    onCheckedChange = { viewModel.updateAutoPlayNextEpisode(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Downloads Section
            SettingsSection(title = "Downloads") {
                PreferenceDropdown(
                    title = "Download Quality",
                    options = listOf("720p", "1080p", "Auto"),
                    selectedOption = uiState.settings.downloadQuality,
                    onOptionSelected = { viewModel.updateDownloadQuality(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Section
            SettingsSection(title = "Storage") {
                PreferenceButton(
                    title = "Clear Cache",
                    subtitle = "Free up space by clearing image and API cache",
                    icon = Icons.Default.Delete,
                    onClick = { showClearCacheDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSection(title = "About") {
                PreferenceItem(
                    title = "Gesture Guide",
                    subtitle = "Learn player gestures and controls",
                    onClick = onGestureGuideClick
                )
                Divider()
                PreferenceItem(
                    title = "Check for Updates",
                    subtitle = "See if a new version is available",
                    onClick = onCheckUpdatesClick
                )
                Divider()
                PreferenceItem(
                    title = "App Version",
                    subtitle = com.vidora.app.BuildConfig.VERSION_NAME
                )
                Divider()
                PreferenceItem(
                    title = "Developed By",
                    subtitle = "GHOST"
                )
            }
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("This will clear all cached images and API responses. The app may load slower until the cache rebuilds.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache(context.cacheDir)
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun PreferenceItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceDropdown(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = selectedOption,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PreferenceButton(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
