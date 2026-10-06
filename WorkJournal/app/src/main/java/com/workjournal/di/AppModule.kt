package com.workjournal.di

import android.content.Context
import androidx.room.Room
import com.workjournal.data.ai.GeminiAssistant
import com.workjournal.data.db.AppDatabase
import com.workjournal.data.db.WorkJournalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "work_journal.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideDao(db: AppDatabase): WorkJournalDao = db.workJournalDao()

    @Provides @Singleton
    fun provideGeminiAssistant(): GeminiAssistant = GeminiAssistant()
}
