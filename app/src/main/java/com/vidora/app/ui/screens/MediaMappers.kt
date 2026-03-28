package com.vidora.app.ui.screens

import com.vidora.app.data.local.FavoriteEntity
import com.vidora.app.data.local.HistoryEntity
import com.vidora.app.data.remote.MediaItem

fun FavoriteEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        title = if (mediaType == "movie") title else null,
        name = if (mediaType == "tv") title else null,
        overview = null,
        posterPath = posterPath,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = null,
        firstAirDate = null,
        mediaType = mediaType,
        popularity = null,
        genres = null,
        credits = null,
        similar = null,
        numberOfSeasons = null,
        seasons = null,
        runtime = null,
        episodeRunTime = null,
        genreIds = null,
        imdbId = null,
        contentRatings = null
    )
}

fun HistoryEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        title = if (mediaType == "movie") title else null,
        name = if (mediaType == "tv") title else null,
        overview = null,
        posterPath = posterPath,
        backdropPath = null,
        voteAverage = 0.0,
        releaseDate = null,
        firstAirDate = null,
        mediaType = mediaType,
        popularity = null,
        genres = null,
        credits = null,
        similar = null,
        numberOfSeasons = null,
        seasons = null,
        runtime = null,
        episodeRunTime = null,
        genreIds = null,
        imdbId = null,
        contentRatings = null
    )
}
