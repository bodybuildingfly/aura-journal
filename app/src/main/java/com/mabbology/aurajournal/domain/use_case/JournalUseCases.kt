package com.mabbology.aurajournal.domain.use_case.journal

data class JournalUseCases(
    val getJournals: GetJournalsUseCase,
    val getJournal: GetJournalUseCase,
    val syncJournals: SyncJournalsUseCase,
    val createJournal: CreateJournalUseCase,
    val updateJournal: UpdateJournalUseCase,
    val deleteJournal: DeleteJournalUseCase,
    val completeAssignmentAndCreateJournal: CompleteAssignmentAndCreateJournalUseCase
)
