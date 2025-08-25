package com.mabbology.aurajournal.di

import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import com.mabbology.aurajournal.domain.use_case.journal.CompleteAssignmentAndCreateJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.CreateJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.DeleteJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.GetJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.GetJournalsUseCase
import com.mabbology.aurajournal.domain.use_case.journal.JournalUseCases
import com.mabbology.aurajournal.domain.use_case.journal.SyncJournalsUseCase
import com.mabbology.aurajournal.domain.use_case.journal.UpdateJournalUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideJournalUseCases(
        journalsRepository: JournalsRepository,
        assignmentsRepository: JournalAssignmentsRepository
    ): JournalUseCases {
        val createJournalUseCase = CreateJournalUseCase(journalsRepository)
        return JournalUseCases(
            getJournals = GetJournalsUseCase(journalsRepository),
            getJournal = GetJournalUseCase(journalsRepository),
            syncJournals = SyncJournalsUseCase(journalsRepository),
            createJournal = createJournalUseCase,
            updateJournal = UpdateJournalUseCase(journalsRepository),
            deleteJournal = DeleteJournalUseCase(journalsRepository),
            completeAssignmentAndCreateJournal = CompleteAssignmentAndCreateJournalUseCase(
                createJournalUseCase,
                assignmentsRepository
            )
        )
    }
}
