package com.mabbology.aurajournal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import io.appwrite.services.Realtime
import io.appwrite.services.Storage
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Singleton
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    @Provides
    @Singleton
    fun provideAppwriteClient(@ApplicationContext context: Context): Client {
        val client = Client(context)
            .setEndpoint(AppwriteConstants.ENDPOINT)
            .setProject(AppwriteConstants.PROJECT_ID)

        try {
            val clientClass = client::class
            val httpInternal = clientClass.memberProperties.find { it.name == "http" }
            httpInternal?.let {
                it.isAccessible = true
                val okHttpClient = it.getter.call(client) as okhttp3.OkHttpClient
                val newOkHttpClient = okHttpClient.newBuilder()
                    .addInterceptor(SequenceInterceptor())
                    .build()
                val field = clientClass.java.getDeclaredField("http")
                field.isAccessible = true
                field.set(client, newOkHttpClient)
            }
        } catch (e: Exception) {
            Log.e("AppwriteModule", "Error adding interceptor", e)
        }

        return client
    }

    @Provides
    @Singleton
    fun provideAppwriteAccount(client: Client): Account {
        return Account(client)
    }

    @Provides
    @Singleton
    fun provideAppwriteDatabases(client: Client): Databases {
        return Databases(client)
    }

    @Provides
    @Singleton
    fun provideAppwriteFunctions(client: Client): Functions {
        return Functions(client)
    }

    @Provides
    @Singleton
    fun provideAppwriteStorage(client: Client): Storage {
        return Storage(client)
    }

    @Provides
    @Singleton
    fun provideAppwriteRealtime(client: Client): Realtime {
        return Realtime(client)
    }
}

class SequenceInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        if (body != null) {
            val contentType = body.contentType()
            val responseBody = body.string()
            val newBody = if (response.request.url.toString().contains("/documents")) {
                try {
                    val json = JSONObject(responseBody)
                    if (json.has("documents")) {
                        val documents = json.getJSONArray("documents")
                        for (i in 0 until documents.length()) {
                            val document = documents.getJSONObject(i)
                            if (!document.has("\$sequence")) {
                                document.put("\$sequence", 0L)
                            }
                        }
                    } else {
                        if (!json.has("\$sequence")) {
                            json.put("\$sequence", 0L)
                        }
                    }
                    json.toString()
                } catch (_: Exception) {
                    responseBody
                }
            } else {
                responseBody
            }
            return response.newBuilder()
                .body(newBody.toResponseBody(contentType))
                .build()
        }
        return response
    }
}
