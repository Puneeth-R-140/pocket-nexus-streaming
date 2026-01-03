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
import com.vidora.app.ui.components.VideoPlayerWebView
import com.vidora.app.ui.theme.VidoraTheme
import com.vidora.app.ui.viewmodels.HomeViewModel
import com.vidora.app.ui.viewmodels.DetailsViewModel
import com.vidora.app.ui.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Simplistic PiP trigger - in a real app check if video is playing
        enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
    }

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
                                }
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
                                onWatchClick = { id, subPath ->
                                    val finalUrl = when {
                                        subPath == "tv" -> "https://watch.vidora.su/watch/tv/$id/1/1"
                                        subPath.startsWith("tv/") -> "https://watch.vidora.su/watch/tv/$id/${subPath.substringAfter("tv/")}"
                                        else -> "https://watch.vidora.su/watch/movie/$id"
                                    }
                                    navController.navigate("player/${java.net.URLEncoder.encode(finalUrl, "UTF-8")}")
                                }
                            )
                        }
                        
                        composable(
                            "player/{url}",
                            arguments = listOf(navArgument("url") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            VideoPlayerWebView(
                                url = java.net.URLDecoder.decode(url, "UTF-8"),
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
