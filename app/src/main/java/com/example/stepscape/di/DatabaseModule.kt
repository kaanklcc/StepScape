package com.example.stepscape.di

import android.content.Context
import androidx.room.Room
import com.example.stepscape.data.local.StepDatabase
import com.example.stepscape.data.local.dao.StepDao
import com.example.stepscape.data.local.dao.StepSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database için Hilt modülü.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStepDatabase(
        @ApplicationContext context: Context
    ): StepDatabase {
        return Room.databaseBuilder(
            context,
            StepDatabase::class.java,
            StepDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStepDao(database: StepDatabase): StepDao {
        return database.stepDao()
    }

    @Provides
    @Singleton
    fun provideStepSessionDao(database: StepDatabase): StepSessionDao {
        return database.stepSessionDao()
    }
}
