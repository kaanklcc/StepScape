package com.example.stepscape.di

import com.example.stepscape.data.repository.AuthRepositoryImpl
import com.example.stepscape.data.repository.StepRepositoryImpl
import com.example.stepscape.domain.repository.AuthRepository
import com.example.stepscape.domain.repository.StepRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStepRepository(
        stepRepositoryImpl: StepRepositoryImpl
    ): StepRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
