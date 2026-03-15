package com.lockin.app.core.di

import com.lockin.app.data.remote.exercisedb.ExerciseDbApi
import com.lockin.app.data.remote.wger.WgerApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * NetworkModule – Provides Retrofit clients for remote APIs.
 *
 * The ExerciseDB base URL points to the free public instance.
 * Replace with your own self-hosted URL if desired.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * ExerciseDB public API base URL.
     * See: https://github.com/ExerciseDB/exercisedb-api
     */
    private const val EXERCISEDB_BASE_URL = "https://exercisedb-api.vercel.app/api/v1/"
    private const val WGER_BASE_URL = "https://wger.de/api/v2/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideExerciseDbApi(client: OkHttpClient): ExerciseDbApi {
        return Retrofit.Builder()
            .baseUrl(EXERCISEDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExerciseDbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWgerApi(client: OkHttpClient): WgerApi {
        return Retrofit.Builder()
            .baseUrl(WGER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WgerApi::class.java)
    }
}
