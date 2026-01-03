package com.vidora.app.data.repository;

import com.vidora.app.data.local.MediaDao;
import com.vidora.app.data.remote.TmdbService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class MediaRepository_Factory implements Factory<MediaRepository> {
  private final Provider<TmdbService> tmdbServiceProvider;

  private final Provider<MediaDao> mediaDaoProvider;

  public MediaRepository_Factory(Provider<TmdbService> tmdbServiceProvider,
      Provider<MediaDao> mediaDaoProvider) {
    this.tmdbServiceProvider = tmdbServiceProvider;
    this.mediaDaoProvider = mediaDaoProvider;
  }

  @Override
  public MediaRepository get() {
    return newInstance(tmdbServiceProvider.get(), mediaDaoProvider.get());
  }

  public static MediaRepository_Factory create(Provider<TmdbService> tmdbServiceProvider,
      Provider<MediaDao> mediaDaoProvider) {
    return new MediaRepository_Factory(tmdbServiceProvider, mediaDaoProvider);
  }

  public static MediaRepository newInstance(TmdbService tmdbService, MediaDao mediaDao) {
    return new MediaRepository(tmdbService, mediaDao);
  }
}
