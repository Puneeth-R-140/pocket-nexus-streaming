package com.vidora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vidora.app.ui.screens.HomeScreen
import com.vidora.app.ui.screens.DetailsScreen
import com.vidora.app.ui.screens.SearchScreen
import com.vidora.app.ui.screens.SettingsScreen
import com.vidora.app.ui.components.VideoPlayerWebView
import com.vidora.app.ui.theme.VidoraTheme
import com.vidora.app.ui.viewmodels.HomeViewModel
import com.vidora.app.ui.viewmodels.DetailsViewModel
import com.vidora.app.ui.viewmodels.SearchViewModel
import com.vidora.app.ui.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // PiP disabled - was causing playback when screen off
    // override fun onUserLeaveHint() {
    //     super.onUserLeaveHint()
    //     enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VidoraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val viewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = viewModel,
                                onMediaClick = { media ->
                                    navController.navigate("details/${media.id}/${media.realMediaType}")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("settings") {
                            val viewModel: SettingsViewModel = hiltViewModel()
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onGestureGuideClick = {
                                    navController.navigate("gesture_guide")
                                },
                                onCheckUpdatesClick = {
                                    navController.navigate("update_check")
                                }
                            )
                        }

                        composable("gesture_guide") {
                            com.vidora.app.ui.screens.GestureGuideScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("update_check") {
                            com.vidora.app.ui.screens.UpdateScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            val viewModel: SearchViewModel = hiltViewModel()
                            SearchScreen(
                                viewModel = viewModel,
                                onMediaClick = { media ->
                                    navController.navigate("details/${media.id}/${media.realMediaType}")
                                }
                            )
                        }
                        
                        composable(
                            "details/{id}/{type}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType },
                                navArgument("type") { type = NavType.StringType }
                            )
                        ) {
                            val viewModel: DetailsViewModel = hiltViewModel()
                            DetailsScreen(
                                viewModel = viewModel,
                                onWatchClick = { mediaId, mediaType, url ->
                                    navController.navigate("player/$mediaId/$mediaType/${java.net.URLEncoder.encode(url, "UTF-8")}")
                                },
                                onMediaClick = { media ->
                                    navController.navigate("details/${media.id}/${media.realMediaType}")
                                }
                            )
                        }
                        
                        composable(
                            "player/{mediaId}/{mediaType}/{url}",
                            arguments = listOf(
                                navArgument("mediaId") { type = NavType.StringType },
                                navArgument("mediaType") { type = NavType.StringType },
                                navArgument("url") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: ""
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            
                            // Create minimal media object for player
                            val media = com.vidora.app.data.remote.MediaItem(
                                id = mediaId,
                                title = if (mediaType == "movie") "Loading..." else null,
                                name = if (mediaType == "tv") "Loading..." else null,
                                overview = null,
                                posterPath = null,
                                backdropPath = null,
                                voteAverage = 0.0,
                                releaseDate = null,
                                firstAirDate = null,
                                mediaType = mediaType,
                                popularity = null,
                                genres = null,
                                credits = null,
                                similar = null,
                                numberOfSeasons = null,
                                seasons = null,
                                runtime= null,
                                episodeRunTime = null,
                                imdbId = null,
                                contentRatings = null
                            )
                            
                            com.vidora.app.player.NativePlayerScreen(
                                media = media,
                                playerUrl = java.net.URLDecoder.decode(url, "UTF-8"),
                                onBack = { navController.popBackStack() },
                                onNavigateToEpisode = { nextSeason, nextEpisode ->
                                    // Extract base URL pattern and construct next episode URL
                                    val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
                                    
                                    // Handle different URL patterns by reconstructing completely
                                    val nextEpisodeUrl = when {
                                        decodedUrl.contains("watch.vidora.su") -> {
                                            "https://watch.vidora.su/watch/tv/$mediaId/$nextSeason/$nextEpisode"
                                        }
                                        else -> {
                                            // Default to vidsrc.xyz format which works reliably
                                            "https://vidsrc.xyz/embed/tv/$mediaId/$nextSeason/$nextEpisode"
                                        }
                                    }
                                    
                                    android.util.Log.d("AutoPlay", "Current URL: $decodedUrl")
                                    android.util.Log.d("AutoPlay", "Next episode URL: $nextEpisodeUrl")
                                    
                                    // Navigate to next episode
                                    navController.navigate("player/$mediaId/$mediaType/${java.net.URLEncoder.encode(nextEpisodeUrl, "UTF-8")}") {
                                        // Replace current player in back stack to avoid stacking
                                        popUpTo("player/{mediaId}/{mediaType}/{url}") {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
