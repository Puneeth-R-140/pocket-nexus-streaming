package com.vidora.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface SubtitleService {
    /**
     * Search for subtitles using TMDB ID.
     * For TV shows, provide season and episode.
     * For movies, only provide the ID.
     */
    @GET("search")
    suspend fun searchSubtitles(
        @Query("id") id: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): SubtitleResponse
}
