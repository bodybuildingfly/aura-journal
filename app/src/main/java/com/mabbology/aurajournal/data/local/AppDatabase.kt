package com.mabbology.aurajournal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Suppress("unused") // Suppress warnings as Room uses these converters at compile time
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(
    entities = [
        PartnerEntity::class,
        PartnerRequestEntity::class,
        JournalEntity::class,
        NoteEntity::class,
        JournalAssignmentEntity::class,
        MessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partnerDao(): PartnerDao
    abstract fun partnerRequestDao(): PartnerRequestDao
    abstract fun journalDao(): JournalDao
    abstract fun noteDao(): NoteDao
    abstract fun journalAssignmentDao(): JournalAssignmentDao
    abstract fun messageDao(): MessageDao
}
