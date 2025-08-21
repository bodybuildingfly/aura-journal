package com.mabbology.aurajournal.di

import com.mabbology.aurajournal.data.repository.AuthRepositoryImpl
import com.mabbology.aurajournal.data.repository.UserProfilesRepositoryImpl
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserProfilesRepository(
        userProfilesRepositoryImpl: UserProfilesRepositoryImpl
    ): UserProfilesRepository
}