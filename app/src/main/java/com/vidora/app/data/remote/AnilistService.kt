package com.vidora.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AnilistService {
    @POST("https://graphql.anilist.co")
    suspend fun postQuery(
        @Body request: AnilistRequest
    ): AnilistResponse
}
