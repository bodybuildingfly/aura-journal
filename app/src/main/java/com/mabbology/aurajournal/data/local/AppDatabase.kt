package com.mabbology.aurajournal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PartnerEntity::class,
        PartnerRequestEntity::class,
        JournalEntity::class,
        NoteEntity::class,
        JournalAssignmentEntity::class
    ],
    version = 1, // IMPORTANT: Increment the database version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao
    abstract fun partnerRequestDao(): PartnerRequestDao
    abstract fun journalDao(): JournalDao
    abstract fun noteDao(): NoteDao
    abstract fun journalAssignmentDao(): JournalAssignmentDao
}
