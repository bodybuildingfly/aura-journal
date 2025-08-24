package com.mabbology.aurajournal.di

import com.mabbology.aurajournal.data.repository.*
import com.mabbology.aurajournal.domain.repository.*
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserProfilesRepository(impl: UserProfilesRepositoryImpl): UserProfilesRepository

    @Binds
    @Singleton
    abstract fun bindJournalsRepository(impl: JournalsRepositoryImpl): JournalsRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRequestsRepository(impl: ConnectionRequestsRepositoryImpl): ConnectionRequestsRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindJournalAssignmentsRepository(impl: JournalAssignmentsRepositoryImpl): JournalAssignmentsRepository
}
