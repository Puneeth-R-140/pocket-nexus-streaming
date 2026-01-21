package com.vidora.app.di

import com.vidora.app.data.remote.TmdbService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbService(okHttpClient: OkHttpClient): TmdbService {
        return Retrofit.Builder()
            .baseUrl("https://themoviedb.vidora.su/api/tmdb/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbService::class.java)
    }

    @Provides
    @Singleton
    fun provideSubtitleService(okHttpClient: OkHttpClient): com.vidora.app.data.remote.SubtitleService {
        return Retrofit.Builder()
            .baseUrl("https://sub.wyzie.ru/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.vidora.app.data.remote.SubtitleService::class.java)
    }
}
