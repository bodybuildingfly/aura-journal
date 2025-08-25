package com.mabbology.aurajournal.di

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
        userProfilesRepository: UserProfilesRepository
    ): AuthRepository {
        return AuthRepositoryImpl(account, userProfilesRepository)
    }

    @Provides
    @Singleton
    fun provideUserProfilesRepository(
        databases: Databases,
        account: Account
    ): UserProfilesRepository {
        return UserProfilesRepositoryImpl(databases, account)
    }

    @Provides
    @Singleton
    fun provideJournalsRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        journalDao: com.mabbology.aurajournal.data.local.JournalDao
    ): JournalsRepository {
        return JournalsRepositoryImpl(databases, account, functions, journalDao)
    }

    @Provides
    @Singleton
    fun providePartnerRequestsRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        userProfilesRepository: UserProfilesRepository,
        partnerRequestDao: com.mabbology.aurajournal.data.local.PartnerRequestDao
    ): PartnerRequestsRepository {
        return PartnerRequestsRepositoryImpl(databases, account, functions, userProfilesRepository, partnerRequestDao)
    }

    @Provides
    @Singleton
    fun providePartnersRepository(
        databases: Databases,
        account: Account,
        userProfilesRepository: UserProfilesRepository,
        partnerDao: com.mabbology.aurajournal.data.local.PartnerDao
    ): PartnersRepository {
        return PartnersRepositoryImpl(databases, account, userProfilesRepository, partnerDao)
    }

    @Provides
    @Singleton
    fun provideNotesRepository(
        databases: Databases,
        account: Account,
        functions: Functions,
        noteDao: com.mabbology.aurajournal.data.local.NoteDao
    ): NotesRepository {
        return NotesRepositoryImpl(databases, account, functions, noteDao)
    }

    @Provides
    @Singleton
    fun provideJournalAssignmentsRepository(
        databases: Databases,
        account: Account,
        assignmentDao: com.mabbology.aurajournal.data.local.JournalAssignmentDao
    ): JournalAssignmentsRepository {
        return JournalAssignmentsRepositoryImpl(databases, account, assignmentDao)
    }
}
