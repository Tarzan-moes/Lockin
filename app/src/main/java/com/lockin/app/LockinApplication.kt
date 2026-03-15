package com.lockin.app

import android.app.Application
import android.util.Log
import com.lockin.app.ai.inference.ModelDownloadWorker
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * LockinApplication – The Application subclass for the Lockin app.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation and
 * serve as the parent component for dependency injection across the app.
 * This is referenced in AndroidManifest.xml via android:name=".LockinApplication".
 *
 * Implements ImageLoaderFactory so Coil automatically uses GIF decoding app-wide.
 */
@HiltAndroidApp
class LockinApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        initBackgroundWork()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    /**
     * Initialize logging framework.
     * TODO: Replace with Timber when added as a dependency.
     */
    private fun initLogging() {
        Log.d(TAG, "Lockin application started")
    }

    /**
     * Initialize background work scheduling via WorkManager.
     * TODO: Enqueue periodic sync workers for Health Connect data,
     *       workout reminders, and habit streak tracking.
     */
    private fun initBackgroundWork() {
        ModelDownloadWorker.enqueue(this)
        Log.d(TAG, "Background work scheduler initialized (model download enqueued)")
    }

    companion object {
        private const val TAG = "LockinApp"
    }
}
