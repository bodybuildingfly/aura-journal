package com.mabbology.aurajournal.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mabbology.aurajournal.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE journal_assignments ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `partnershipId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `mediaUrl` TEXT, `mediaType` TEXT, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN `status` TEXT NOT NULL DEFAULT 'sent'")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aura_journal_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun providePartnerDao(appDatabase: AppDatabase): PartnerDao {
        return appDatabase.partnerDao()
    }

    @Provides
    @Singleton
    fun providePartnerRequestDao(appDatabase: AppDatabase): PartnerRequestDao {
        return appDatabase.partnerRequestDao()
    }

    @Provides
    @Singleton
    fun provideJournalDao(appDatabase: AppDatabase): JournalDao {
        return appDatabase.journalDao()
    }

    @Provides
    @Singleton
    fun provideNoteDao(appDatabase: AppDatabase): NoteDao {
        return appDatabase.noteDao()
    }

    @Provides
    @Singleton
    fun provideJournalAssignmentDao(appDatabase: AppDatabase): JournalAssignmentDao {
        return appDatabase.journalAssignmentDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(appDatabase: AppDatabase): MessageDao {
        return appDatabase.messageDao()
    }
}
