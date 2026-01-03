package com.vidora.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbResponse<MediaItem>

    @GET("trending/{media_type}/week")
    suspend fun getTrending(
        @Path("media_type") mediaType: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    @GET("{media_type}/popular")
    suspend fun getPopular(
        @Path("media_type") mediaType: String,
        @Query("page") page: Int = 1
    ): TmdbResponse<MediaItem>

    @GET("{media_type}/{id}")
    suspend fun getDetails(
        @Path("media_type") mediaType: String,
        @Path("id") id: String,
        @Query("append_to_response") append: String = "credits,similar"
    ): MediaItem

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonEpisodes(
        @Path("id") id: String,
        @Path("season_number") season: Int
    ): SeasonEpisodesResponse
}
