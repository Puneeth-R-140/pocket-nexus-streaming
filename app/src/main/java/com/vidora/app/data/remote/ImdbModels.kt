package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName

data class ImdbTitleResponse(
    @SerializedName("results") val results: List<ImdbTitle>?,
    @SerializedName("titles") val titles: List<ImdbTitle>?, // /search/titles uses "titles"
    @SerializedName("totalCount") val totalCount: Int?,
    @SerializedName("nextPageToken") val nextPageToken: String?
)

data class ImdbBatchGetTitlesResponse(
    @SerializedName("titles") val titles: List<ImdbTitle>
)

data class ImdbTitle(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String?,
    @SerializedName("primaryTitle") val primaryTitle: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("primaryImage") val primaryImage: ImdbImage?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("rating") val rating: ImdbRating?,
    @SerializedName("startYear") val startYear: Int?,
    @SerializedName("runtimeSeconds") val runtimeSeconds: Int?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("stars") val stars: List<ImdbName>?,
    @SerializedName("directors") val directors: List<ImdbName>?,
    @SerializedName("originCountries") val originCountries: List<ImdbCountry>?,
    @SerializedName("spokenLanguages") val spokenLanguages: List<ImdbLanguage>?
)

data class ImdbImage(
    @SerializedName("url") val url: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)

data class ImdbRating(
    @SerializedName("aggregateRating") val aggregateRating: Double?,
    @SerializedName("voteCount") val voteCount: Int?
)

data class ImdbName(
    @SerializedName("id") val id: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("primaryImage") val primaryImage: ImdbImage?
)

data class ImdbCountry(
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?
)

data class ImdbLanguage(
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?
)

data class ImdbSeason(
    @SerializedName("season") val season: String,
    @SerializedName("episodeCount") val episodeCount: Int?
)

data class ImdbListSeasonsResponse(
    @SerializedName("seasons") val seasons: List<ImdbSeason>
)

data class ImdbEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("episodeNumber") val episodeNumber: Int?,
    @SerializedName("season") val season: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("primaryImage") val primaryImage: ImdbImage?,
    @SerializedName("rating") val rating: ImdbRating?
)

data class ImdbListEpisodesResponse(
    @SerializedName("episodes") val episodes: List<ImdbEpisode>,
    @SerializedName("totalCount") val totalCount: Int?
)
