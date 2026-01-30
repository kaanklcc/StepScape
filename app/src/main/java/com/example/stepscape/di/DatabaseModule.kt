package com.example.stepscape.di

import android.content.Context
import androidx.room.Room
import com.example.stepscape.data.local.StepDatabase
import com.example.stepscape.data.local.dao.StepDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * database için hilt.
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideStepDao(database: StepDatabase): StepDao {
        return database.stepDao()
    }
}
