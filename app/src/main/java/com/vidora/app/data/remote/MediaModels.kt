package com.vidora.app.data.remote

import com.google.gson.annotations.SerializedName

data class TmdbResponse<T>(
    @SerializedName("results") val results: List<T>,
    @SerializedName("total_results") val totalResults: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("page") val page: Int
)

data class MediaItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("popularity") val popularity: Double?,
    @SerializedName("genres") val genres: List<Genre>?,
    @SerializedName("credits") val credits: Credits?,
    @SerializedName("similar") val similar: TmdbResponse<MediaItem>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("seasons") val seasons: List<Season>?,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("imdb_id") val imdbId: String?,
    @SerializedName("content_ratings") val contentRatings: ContentRatings?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val realMediaType: String get() = mediaType ?: if (title != null) "movie" else "tv"
    val totalSeasons: Int get() = numberOfSeasons ?: seasons?.size ?: 1
    val runtimeMinutes: Int? get() = runtime ?: episodeRunTime?.firstOrNull()
    val releaseYear: String? get() = (releaseDate ?: firstAirDate)?.take(4)
}

data class Season(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int?
)

data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class Credits(
    @SerializedName("cast") val cast: List<CastMember>
)

data class CastMember(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class Episode(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("still_path") val stillPath: String?
)

data class SeasonEpisodesResponse(
    @SerializedName("episodes") val episodes: List<Episode>
)

data class ContentRatings(
    @SerializedName("results") val results: List<ContentRating>?
)

data class ContentRating(
    @SerializedName("iso_3166_1") val country: String,
    @SerializedName("rating") val rating: String?,
    @SerializedName("certification") val certification: String?
) {
    val displayRating: String get() = rating ?: certification ?: ""
}

