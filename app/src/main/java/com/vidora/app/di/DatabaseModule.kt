package com.vidora.app.di

import android.content.Context
import androidx.room.Room
import com.vidora.app.data.local.MediaDao
import com.vidora.app.data.local.VidoraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VidoraDatabase {
        return Room.databaseBuilder(
            context,
            VidoraDatabase::class.java,
            "vidora_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMediaDao(database: VidoraDatabase): MediaDao {
        return database.mediaDao()
    }
}
