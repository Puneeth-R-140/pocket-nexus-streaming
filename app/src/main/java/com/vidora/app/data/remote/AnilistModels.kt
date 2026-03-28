package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName

data class AnilistRequest(
    val query: String,
    val variables: Map<String, Any>
)

data class AnilistResponse(
    @SerializedName("data") val data: AnilistData
)

data class AnilistData(
    @SerializedName("Media") val media: AnilistMedia?
)

data class AnilistMedia(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: AnilistTitle
)

data class AnilistTitle(
    @SerializedName("romaji") val romaji: String?,
    @SerializedName("english") val english: String?,
    @SerializedName("native") val native: String?
)
