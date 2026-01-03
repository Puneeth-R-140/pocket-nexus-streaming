package com.vidora.app.data.repository;

@javax.inject.Singleton
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000bJ\"\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\b2\u0006\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u0012J\u0012\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\u000e0\bJ\u0012\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u000e0\bJ\u0012\u0010\u0016\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u000e0\bJ\u0012\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00180\u000e0\bJ\u0019\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\f\u001a\u00020\u000bH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u001bJ\u001a\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u000e0\b2\u0006\u0010\u001d\u001a\u00020\u000bJ\u0019\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\tH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010!J1\u0010\"\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\t2\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u00122\n\b\u0002\u0010#\u001a\u0004\u0018\u00010\u0012H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010$R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006%"}, d2 = {"Lcom/vidora/app/data/repository/MediaRepository;", "", "tmdbService", "Lcom/vidora/app/data/remote/TmdbService;", "mediaDao", "Lcom/vidora/app/data/local/MediaDao;", "(Lcom/vidora/app/data/remote/TmdbService;Lcom/vidora/app/data/local/MediaDao;)V", "getDetails", "Lkotlinx/coroutines/flow/Flow;", "Lcom/vidora/app/data/remote/MediaItem;", "mediaType", "", "id", "getEpisodes", "", "Lcom/vidora/app/data/remote/Episode;", "tvId", "season", "", "getFavorites", "Lcom/vidora/app/data/local/FavoriteEntity;", "getTrendingMovies", "getTrendingTVShows", "getWatchHistory", "Lcom/vidora/app/data/local/HistoryEntity;", "isFavorite", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "search", "query", "toggleFavorite", "", "media", "(Lcom/vidora/app/data/remote/MediaItem;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateHistory", "episode", "(Lcom/vidora/app/data/remote/MediaItem;Ljava/lang/Integer;Ljava/lang/Integer;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class MediaRepository {
    @org.jetbrains.annotations.NotNull
    private final com.vidora.app.data.remote.TmdbService tmdbService = null;
    @org.jetbrains.annotations.NotNull
    private final com.vidora.app.data.local.MediaDao mediaDao = null;
    
    @javax.inject.Inject
    public MediaRepository(@org.jetbrains.annotations.NotNull
    com.vidora.app.data.remote.TmdbService tmdbService, @org.jetbrains.annotations.NotNull
    com.vidora.app.data.local.MediaDao mediaDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.remote.MediaItem>> getTrendingMovies() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.remote.MediaItem>> getTrendingTVShows() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.remote.MediaItem>> search(@org.jetbrains.annotations.NotNull
    java.lang.String query) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<com.vidora.app.data.remote.MediaItem> getDetails(@org.jetbrains.annotations.NotNull
    java.lang.String mediaType, @org.jetbrains.annotations.NotNull
    java.lang.String id) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.remote.Episode>> getEpisodes(@org.jetbrains.annotations.NotNull
    java.lang.String tvId, int season) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.local.FavoriteEntity>> getFavorites() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object toggleFavorite(@org.jetbrains.annotations.NotNull
    com.vidora.app.data.remote.MediaItem media, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object isFavorite(@org.jetbrains.annotations.NotNull
    java.lang.String id, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.vidora.app.data.local.HistoryEntity>> getWatchHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object updateHistory(@org.jetbrains.annotations.NotNull
    com.vidora.app.data.remote.MediaItem media, @org.jetbrains.annotations.Nullable
    java.lang.Integer season, @org.jetbrains.annotations.Nullable
    java.lang.Integer episode, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}