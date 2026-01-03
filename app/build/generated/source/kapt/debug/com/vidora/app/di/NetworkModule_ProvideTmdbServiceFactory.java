package com.vidora.app.di;

import com.vidora.app.data.remote.TmdbService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class NetworkModule_ProvideTmdbServiceFactory implements Factory<TmdbService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public NetworkModule_ProvideTmdbServiceFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public TmdbService get() {
    return provideTmdbService(okHttpClientProvider.get());
  }

  public static NetworkModule_ProvideTmdbServiceFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new NetworkModule_ProvideTmdbServiceFactory(okHttpClientProvider);
  }

  public static TmdbService provideTmdbService(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideTmdbService(okHttpClient));
  }
}
