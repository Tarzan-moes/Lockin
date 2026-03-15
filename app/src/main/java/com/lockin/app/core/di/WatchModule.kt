package com.lockin.app.core.di

import android.content.Context
import com.lockin.app.watch.connection.WatchConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WatchModule — Provides the Watch Connection manager as a singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object WatchModule {

    @Provides
    @Singleton
    fun provideWatchConnectionManager(
        @ApplicationContext context: Context
    ): WatchConnectionManager = WatchConnectionManager(context)
}

