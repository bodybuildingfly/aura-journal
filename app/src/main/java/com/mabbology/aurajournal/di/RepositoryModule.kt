package com.mabbology.aurajournal.di

import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.repository.*
import com.mabbology.aurajournal.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import javax.inject.Singleton

// Change from 'abstract class' to 'object' and remove @Binds
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // Use @Provides for each repository to be explicit
    @Provides
    @Singleton
    fun provideAuthRepository(
        account: Account,
        userProfilesRepository: UserProfilesRepository,
        dispatcherProvider: DispatcherProvider
    ): AuthRepository {
        return AuthRepositoryImpl(account, userProfilesRepository, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideUserProfilesRepository(
        databases: Databases,
        account: Account,
        dispatcherProvider: DispatcherProvider
    ): UserProfilesRepository {
        return UserProfilesRepositoryImpl(databases, account, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideJournalsRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        journalDao: com.mabbology.aurajournal.data.local.JournalDao,
        dispatcherProvider: DispatcherProvider
    ): JournalsRepository {
        return JournalsRepositoryImpl(databases, account, functions, journalDao, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun providePartnerRequestsRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        userProfilesRepository: UserProfilesRepository,
        partnerRequestDao: com.mabbology.aurajournal.data.local.PartnerRequestDao,
        dispatcherProvider: DispatcherProvider
    ): PartnerRequestsRepository {
        return PartnerRequestsRepositoryImpl(databases, account, functions, userProfilesRepository, partnerRequestDao, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun providePartnersRepository(
        databases: Databases,
        functions: Functions,
        account: Account,
        userProfilesRepository: UserProfilesRepository,
        partnerDao: com.mabbology.aurajournal.data.local.PartnerDao,
        dispatcherProvider: DispatcherProvider
    ): PartnersRepository {
        return PartnersRepositoryImpl(databases, functions, account, userProfilesRepository, partnerDao, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideNotesRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        noteDao: com.mabbology.aurajournal.data.local.NoteDao,
        dispatcherProvider: DispatcherProvider
    ): NotesRepository {
        return NotesRepositoryImpl(databases, account, functions, noteDao, dispatcherProvider)
    }

    @Provides
    @Singleton
    fun provideJournalAssignmentsRepository(
        databases: Databases,
        account: Account,
        assignmentDao: com.mabbology.aurajournal.data.local.JournalAssignmentDao,
        dispatcherProvider: DispatcherProvider
    ): JournalAssignmentsRepository {
        return JournalAssignmentsRepositoryImpl(databases, account, assignmentDao, dispatcherProvider)
    }
}
