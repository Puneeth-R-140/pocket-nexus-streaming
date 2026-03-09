package com.vidora.app.di

import android.content.Context
import com.vidora.app.data.remote.TmdbService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
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
    fun provideCache(@ApplicationContext context: Context): Cache {
        val cacheSize = 100L * 1024 * 1024 // 100MB
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, cacheSize)
    }

    @Provides
    @Singleton
    fun provideCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder()
                .maxAge(1, TimeUnit.HOURS) // Cache for 1 hour
                .build()
            response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .removeHeader("Pragma") // Remove pragma no-cache if present
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideBaseOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        cacheInterceptor: Interceptor,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("tmdbClient")
    fun provideTmdbOkHttpClient(
        baseClient: OkHttpClient
    ): OkHttpClient {
        // Intercept TMDB requests and inject the TMDB API key as a query param
        val tmdbAuthInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("api_key", "4ef0d7355d9ffb5151e987764708ce96") // TMDB v3 key
                .addQueryParameter("language", "en-US")
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }
        return baseClient.newBuilder()
            .addInterceptor(tmdbAuthInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbService(@javax.inject.Named("tmdbClient") okHttpClient: OkHttpClient): TmdbService {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/") // Real TMDB API
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
            .client(okHttpClient) // Uses base (clean) client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.vidora.app.data.remote.SubtitleService::class.java)
    }

    @Provides
    @Singleton
    fun provideOmdbService(okHttpClient: OkHttpClient): com.vidora.app.data.remote.OmdbService {
        return Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.vidora.app.data.remote.OmdbService::class.java)
    }
}

