package com.vidora.app.data.remote

fun ImdbTitle.toMediaItem(): MediaItem {
    val isTv = type?.contains("TV", ignoreCase = true) == true
    return MediaItem(
        id = id,
        title = if (!isTv) primaryTitle else null,
        name = if (isTv) primaryTitle else null,
        overview = plot,
        posterPath = primaryImage?.url,
        backdropPath = null, // IMDbAPI doesn't seem to have backdrops in basic title info
        voteAverage = rating?.aggregateRating ?: 0.0,
        releaseDate = if (!isTv) startYear?.toString() else null,
        firstAirDate = if (isTv) startYear?.toString() else null,
        mediaType = if (isTv) "tv" else "movie",
        popularity = null,
        genres = genres?.map { Genre(0, it) },
        credits = Credits(cast = stars?.map { CastMember(0, it.displayName ?: "Unknown", "Star", it.primaryImage?.url) } ?: emptyList()),
        similar = null,
        numberOfSeasons = null, // Need separate call or mapping
        seasons = null,
        runtime = if (runtimeSeconds != null) runtimeSeconds / 60 else null,
        episodeRunTime = null,
        genreIds = null,
        imdbId = id,
        contentRatings = null
    )
}

fun ImdbEpisode.toEpisode(): Episode {
    return Episode(
        id = 0, // No numeric ID in IMDbAPI
        name = title ?: "Episode $episodeNumber",
        overview = plot,
        episodeNumber = episodeNumber ?: 0,
        seasonNumber = season?.toIntOrNull() ?: 1,
        stillPath = primaryImage?.url
    )
}
