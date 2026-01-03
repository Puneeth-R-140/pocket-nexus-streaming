package com.vidora.app.di;

import com.vidora.app.data.local.MediaDao;
import com.vidora.app.data.local.VidoraDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideMediaDaoFactory implements Factory<MediaDao> {
  private final Provider<VidoraDatabase> databaseProvider;

  public DatabaseModule_ProvideMediaDaoFactory(Provider<VidoraDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MediaDao get() {
    return provideMediaDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMediaDaoFactory create(
      Provider<VidoraDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMediaDaoFactory(databaseProvider);
  }

  public static MediaDao provideMediaDao(VidoraDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMediaDao(database));
  }
}
