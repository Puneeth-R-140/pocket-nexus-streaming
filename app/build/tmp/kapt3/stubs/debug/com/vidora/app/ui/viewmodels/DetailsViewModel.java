package com.vidora.app.ui.viewmodels;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0011H\u0002J\u0016\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0014\u001a\u00020\u0015J+\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0017\u001a\u00020\u00182\n\b\u0002\u0010\u0014\u001a\u0004\u0018\u00010\u00152\n\b\u0002\u0010\u0019\u001a\u0004\u0018\u00010\u0015\u00a2\u0006\u0002\u0010\u001aJ\u0006\u0010\u001b\u001a\u00020\u000fR\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u001c"}, d2 = {"Lcom/vidora/app/ui/viewmodels/DetailsViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/vidora/app/data/repository/MediaRepository;", "savedStateHandle", "Landroidx/lifecycle/SavedStateHandle;", "(Lcom/vidora/app/data/repository/MediaRepository;Landroidx/lifecycle/SavedStateHandle;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/vidora/app/ui/viewmodels/DetailsUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "loadDetails", "", "type", "", "id", "loadEpisodes", "season", "", "markWatched", "media", "Lcom/vidora/app/data/remote/MediaItem;", "episode", "(Lcom/vidora/app/data/remote/MediaItem;Ljava/lang/Integer;Ljava/lang/Integer;)V", "toggleFavorite", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class DetailsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.vidora.app.data.repository.MediaRepository repository = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.vidora.app.ui.viewmodels.DetailsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.vidora.app.ui.viewmodels.DetailsUiState> uiState = null;
    
    @javax.inject.Inject
    public DetailsViewModel(@org.jetbrains.annotations.NotNull
    com.vidora.app.data.repository.MediaRepository repository, @org.jetbrains.annotations.NotNull
    androidx.lifecycle.SavedStateHandle savedStateHandle) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.vidora.app.ui.viewmodels.DetailsUiState> getUiState() {
        return null;
    }
    
    private final void loadDetails(java.lang.String type, java.lang.String id) {
    }
    
    public final void loadEpisodes(@org.jetbrains.annotations.NotNull
    java.lang.String id, int season) {
    }
    
    public final void markWatched(@org.jetbrains.annotations.NotNull
    com.vidora.app.data.remote.MediaItem media, @org.jetbrains.annotations.Nullable
    java.lang.Integer season, @org.jetbrains.annotations.Nullable
    java.lang.Integer episode) {
    }
    
    public final void toggleFavorite() {
    }
}