package com.mabbology.aurajournal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    @Provides
    @Singleton
    fun provideAppwriteClient(@ApplicationContext context: Context): Client {
        return Client(context)
            // Replace with your local IP and Appwrite port (default is 80 or 443)
            .setEndpoint("https://appwrite.mabbology.com/v1")
            // Replace with your Appwrite Project ID
            .setProject("68a74008002bc24b5f1e")
            //.setSelfSigned(true) // Important for local development
    }
}