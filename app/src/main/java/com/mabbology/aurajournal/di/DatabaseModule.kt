package com.mabbology.aurajournal.di

import android.content.Context
import androidx.room.Room
import com.mabbology.aurajournal.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
            .fallbackToDestructiveMigration()
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
}
