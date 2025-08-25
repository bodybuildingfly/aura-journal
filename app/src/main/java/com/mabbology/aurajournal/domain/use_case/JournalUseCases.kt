package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.domain.use_case.journal.CompleteAssignmentAndCreateJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.CreateJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.DeleteJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.GetJournalUseCase
import com.mabbology.aurajournal.domain.use_case.journal.GetJournalsUseCase
import com.mabbology.aurajournal.domain.use_case.journal.SyncJournalsUseCase
import com.mabbology.aurajournal.domain.use_case.journal.UpdateJournalUseCase

data class JournalUseCases(
    val getJournals: GetJournalsUseCase,
    val getJournal: GetJournalUseCase,
    val syncJournals: SyncJournalsUseCase,
    val createJournal: CreateJournalUseCase,
    val updateJournal: UpdateJournalUseCase,
    val deleteJournal: DeleteJournalUseCase,
    val completeAssignmentAndCreateJournal: CompleteAssignmentAndCreateJournalUseCase
)
