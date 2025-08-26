package com.mabbology.aurajournal.di

import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.core.util.StandardDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return StandardDispatchers()
    }
}
