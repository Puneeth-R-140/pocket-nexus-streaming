package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbService {
    @GET("/")
    suspend fun getRatings(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String = "YOUR_API_KEY_HERE"
    ): OmdbResponse
}

data class OmdbResponse(
    @SerializedName("imdbID") val imdbId: String?,
    @SerializedName("imdbRating") val imdbRating: String?,
    @SerializedName("imdbVotes") val imdbVotes: String?,
    @SerializedName("Ratings") val ratings: List<Rating>?,
    @SerializedName("Response") val response: String?
) {
    val rottenTomatoesRating: String? get() {
        return ratings?.find { it.source == "Rotten Tomatoes" }?.value
    }
}

data class Rating(
    @SerializedName("Source") val source: String,
    @SerializedName("Value") val value: String
)
