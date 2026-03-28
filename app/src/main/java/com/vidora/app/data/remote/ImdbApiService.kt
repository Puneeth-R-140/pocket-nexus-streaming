package com.vidora.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ImdbApiService {
    
    @GET("search/titles")
    suspend fun searchTitles(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): ImdbTitleResponse

    @GET("titles")
    suspend fun listTitles(
        @Query("types") type: String? = null,
        @Query("sortBy") sortBy: String = "SORT_BY_POPULARITY",
        @Query("sortOrder") sortOrder: String = "DESC",
        @Query("limit") limit: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): ImdbTitleResponse

    @GET("titles/{titleId}")
    suspend fun getTitle(
        @Path("titleId") titleId: String
    ): ImdbTitle

    @GET("titles/{titleId}/seasons")
    suspend fun getSeasons(
        @Path("titleId") titleId: String
    ): ImdbListSeasonsResponse

    @GET("titles/{titleId}/episodes")
    suspend fun getEpisodes(
        @Path("titleId") titleId: String,
        @Query("season") season: String? = null
    ): ImdbListEpisodesResponse
}
