package com.vidora.app.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\bf\u0018\u00002\u00020\u0001J/\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00052\b\b\u0003\u0010\u0007\u001a\u00020\u0005H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\bJ+\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00030\n2\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0003\u0010\u000b\u001a\u00020\fH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\rJ%\u0010\u000e\u001a\u00020\u000f2\b\b\u0001\u0010\u0006\u001a\u00020\u00052\b\b\u0001\u0010\u0010\u001a\u00020\fH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\rJ+\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\n2\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0003\u0010\u000b\u001a\u00020\fH\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\rJ5\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\n2\b\b\u0001\u0010\u0013\u001a\u00020\u00052\b\b\u0003\u0010\u000b\u001a\u00020\f2\b\b\u0003\u0010\u0014\u001a\u00020\u0005H\u00a7@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0015\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0016"}, d2 = {"Lcom/vidora/app/data/remote/TmdbService;", "", "getDetails", "Lcom/vidora/app/data/remote/MediaItem;", "mediaType", "", "id", "append", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPopular", "Lcom/vidora/app/data/remote/TmdbResponse;", "page", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSeasonEpisodes", "Lcom/vidora/app/data/remote/SeasonEpisodesResponse;", "season", "getTrending", "searchMulti", "query", "language", "(Ljava/lang/String;ILjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface TmdbService {
    
    @retrofit2.http.GET(value = "search/multi")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object searchMulti(@retrofit2.http.Query(value = "query")
    @org.jetbrains.annotations.NotNull
    java.lang.String query, @retrofit2.http.Query(value = "page")
    int page, @retrofit2.http.Query(value = "language")
    @org.jetbrains.annotations.NotNull
    java.lang.String language, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.vidora.app.data.remote.TmdbResponse<com.vidora.app.data.remote.MediaItem>> $completion);
    
    @retrofit2.http.GET(value = "trending/{media_type}/week")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getTrending(@retrofit2.http.Path(value = "media_type")
    @org.jetbrains.annotations.NotNull
    java.lang.String mediaType, @retrofit2.http.Query(value = "page")
    int page, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.vidora.app.data.remote.TmdbResponse<com.vidora.app.data.remote.MediaItem>> $completion);
    
    @retrofit2.http.GET(value = "{media_type}/popular")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getPopular(@retrofit2.http.Path(value = "media_type")
    @org.jetbrains.annotations.NotNull
    java.lang.String mediaType, @retrofit2.http.Query(value = "page")
    int page, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.vidora.app.data.remote.TmdbResponse<com.vidora.app.data.remote.MediaItem>> $completion);
    
    @retrofit2.http.GET(value = "{media_type}/{id}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getDetails(@retrofit2.http.Path(value = "media_type")
    @org.jetbrains.annotations.NotNull
    java.lang.String mediaType, @retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @retrofit2.http.Query(value = "append_to_response")
    @org.jetbrains.annotations.NotNull
    java.lang.String append, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.vidora.app.data.remote.MediaItem> $completion);
    
    @retrofit2.http.GET(value = "tv/{id}/season/{season_number}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getSeasonEpisodes(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @retrofit2.http.Path(value = "season_number")
    int season, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.vidora.app.data.remote.SeasonEpisodesResponse> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}